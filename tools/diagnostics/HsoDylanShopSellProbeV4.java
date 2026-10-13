import java.io.*;
import java.lang.instrument.Instrumentation;
import java.lang.invoke.*;
import java.lang.reflect.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.concurrent.*;

/**
 * Diagnostic Probe V4:
 * Handles the complete 2-step sale flow:
 * Step 1: Opens server shop (Opcode 23 -> -30 -> 23).
 * Step 2: Sends Opcode 18 (del_item, typedelete=1).
 * Step 3: Receives Opcode -32 server confirmation dialog ("Bạn có muốn bán...").
 * Step 4: Automatically sends Opcode -32 confirmation answer (dialogId, dialogType, 1).
 * Step 5: Confirms inventory update (Opcode 16) and Gold credit.
 */
public final class HsoDylanShopSellProbeV4 implements Runnable {
    private final Instrumentation inst;
    private final Path root;
    private Class<?> game, canvas, messageClass;
    private Object player, map, session, originalHandler, proxyHandler;
    private int currentMapId, area, x, y, activeNpc = -3, menuId = -1, option = -1;
    private long coinBefore;
    private CountDownLatch menuLatch = new CountDownLatch(1);
    private CountDownLatch shopLatch = new CountDownLatch(1);
    private CountDownLatch dialogLatch = new CountDownLatch(1);
    private volatile boolean gotMenu = false;
    private volatile boolean gotShop = false;
    private volatile String shopTitle = "";
    private volatile int inventoryUpdates = 0;
    private volatile boolean saleSuccess = false;
    private volatile int lastDialogId = Integer.MIN_VALUE;
    private volatile int lastDialogType = -1;
    private volatile String lastDialogText = "";
    private volatile boolean confirmationSent = false;

    // Target item
    private int targetId = -1;
    private int targetCat = -1;
    private String targetName = "";
    private int targetQtyBefore = 0;

    private HsoDylanShopSellProbeV4(Instrumentation inst, String args) {
        this.inst = inst;
        String[] parts = args.split("\\|", 3);
        root = Paths.get(parts[1]);
    }

    public static synchronized void agentmain(String args, Instrumentation inst) {
        String key = "hso.dylanProbeV4.running";
        if (System.getProperty(key) != null) {
            throw new IllegalStateException("Probe V4 already running");
        }
        System.setProperty(key, "true");
        Thread t = new Thread(new HsoDylanShopSellProbeV4(inst, args), "HSO Shop sell probe V4");
        t.setDaemon(true);
        t.start();
    }

    private static Object call(Method m, Object obj, Object... args) throws Throwable {
        MethodHandle h = MethodHandles.publicLookup().unreflect(m);
        if (!Modifier.isStatic(m.getModifiers())) h = h.bindTo(obj);
        return h.invokeWithArguments(args);
    }

    private static Object field(Object obj, String name) throws Exception {
        return obj.getClass().getField(name).get(obj);
    }

    private static int num(Object obj, String name) throws Exception {
        return ((Number)field(obj, name)).intValue();
    }

    private long coins() throws Exception {
        return ((Number)field(player, "coin")).longValue();
    }

    private synchronized void log(String s) {
        String line = new java.text.SimpleDateFormat("HH:mm:ss.SSS").format(new java.util.Date())
                + " PROBE_V4 " + s + "\n";
        try {
            Files.write(root.resolve("hso_remote_sell_probe.log"),
                    line.getBytes(StandardCharsets.UTF_8),
                    StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        } catch (IOException ignored) {}
        System.setProperty("hso.dylanProbeV4.status", s);
    }

    private void guard() throws Throwable {
        if (!(Boolean)call(session.getClass().getMethod("isConnected"), session)
                || game.getField("player").get(null) != player
                || canvas.getField("loadmap").get(null) != map) {
            throw new IllegalStateException("Disconnected or player switched");
        }
    }

    private Object receive(Object obj, Method method, Object[] args) throws Throwable {
        if (method.getName().equals("onMessage") && args != null && args.length == 1) {
            Object msg = args[0];
            int cmd = num(msg, "command");
            if (cmd == -30 || cmd == 23 || cmd == 16 || cmd == -32 || cmd == 37 || cmd == 53) {
                DataInputStream in = (DataInputStream)call(messageClass.getMethod("reader"), msg);
                in.mark(in.available() + 1);
                try {
                    if (cmd == -30) {
                        int npc = in.readShort();
                        int id = in.readByte();
                        int count = in.readUnsignedByte();
                        if (count <= 32) {
                            StringBuilder labels = new StringBuilder();
                            for (int i = 0; i < count; i++) {
                                String label = in.readUTF();
                                labels.append('[').append(i).append(':').append(label).append(']');
                                if ("Mua bán".equalsIgnoreCase(label)
                                        || "Đồ thương nhân".equalsIgnoreCase(label)
                                        || "Cửa hàng".equalsIgnoreCase(label)) {
                                    option = i;
                                }
                            }
                            if (option < 0 && count > 0) option = 0;
                            menuId = id;
                            gotMenu = true;
                            log("RX menu npc=" + npc + " menu=" + menuId + " option=" + option + " labels=" + labels);
                            menuLatch.countDown();
                        }
                    } else if (cmd == 23) {
                        String title = in.readUTF();
                        int type = in.readByte();
                        if (type < 0) {
                            log("RX chat from NPC " + type + ": '" + title + "'");
                        } else {
                            shopTitle = title;
                            gotShop = true;
                            log("RX shop cmd=23 type=" + type + " title='" + title + "'");
                            shopLatch.countDown();
                        }
                    } else if (cmd == 16) {
                        inventoryUpdates++;
                        long currentCoins = coins();
                        log("RX inventory cmd=16 update #" + inventoryUpdates + " coin=" + currentCoins);
                        if (currentCoins > coinBefore) {
                            saleSuccess = true;
                            log("SALE_CONFIRMED! Coin increased: " + coinBefore + " -> " + currentCoins
                                    + " (+" + (currentCoins - coinBefore) + " Vàng)");
                        }
                    } else if (cmd == -32) {
                        int id = in.readShort();
                        int type = in.readByte();
                        String text = in.readUTF();
                        lastDialogId = id;
                        lastDialogType = type;
                        lastDialogText = text;
                        log("RX dialog cmd=-32 id=" + id + " type=" + type + " text='" + text + "'");
                        dialogLatch.countDown();
                    } else if (cmd == 37) {
                        String text = in.readUTF();
                        String extra = in.readUTF();
                        int type = in.readByte();
                        log("RX info37 type=" + type + " text='" + text.replace('\n', '|') + "' extra='" + extra + "'");
                    } else if (cmd == 53) {
                        String text = in.readUTF();
                        log("RX notice53: '" + text + "'");
                    }
                } catch (Throwable error) {
                    log("CAPTURE error: " + error);
                } finally {
                    in.reset();
                }
            }
        }
        return call(method, originalHandler, args == null ? new Object[0] : args);
    }

    private void sendPacket(int cmd, byte[] data) throws Throwable {
        guard();
        Object msg = MethodHandles.publicLookup()
                .findConstructor(messageClass, MethodType.methodType(void.class, byte.class))
                .invokeWithArguments((byte)cmd);
        DataOutputStream out = (DataOutputStream)call(messageClass.getMethod("writer"), msg);
        out.write(data);
        call(session.getClass().getMethod("sendMessage", messageClass), session, msg);
        StringBuilder hex = new StringBuilder();
        for (byte b : data) hex.append(String.format("%02X", b & 255));
        log("TX cmd=" + cmd + " payload=" + hex);
    }

    private boolean selectTargetItem(Object vecInventory) throws Throwable {
        int size = (Integer)call(vecInventory.getClass().getMethod("size"), vecInventory);
        log("INVENTORY size=" + size + " items total");

        for (int i = 0; i < size; i++) {
            Object it = call(vecInventory.getClass().getMethod("elementAt", int.class), vecInventory, i);
            if (it == null) continue;
            int id = num(it, "Id");
            int cat = num(it, "ItemCatagory");
            String name = (String)field(it, "itemName");
            int lock = num(it, "isLock");
            int tier = num(it, "tier");
            int qty = cat == 3 ? 1 : Math.max(1, num(it, "numPotion"));

            // Select unlocked, tier 0 equipment
            if (cat == 3 && lock == 0 && tier == 0) {
                targetId = id;
                targetCat = cat;
                targetName = name;
                targetQtyBefore = qty;
                log("TARGET_SELECTED: id=" + targetId + " cat=" + targetCat + " name='" + targetName + "'");
                return true;
            }
        }
        return false;
    }

    private boolean openNpcShop(int npcId, String npcName) throws Throwable {
        activeNpc = npcId;
        menuLatch = new CountDownLatch(1);
        shopLatch = new CountDownLatch(1);
        gotMenu = false;
        gotShop = false;
        option = -1;

        log("Requesting menu from " + npcName + " (ID: " + npcId + ")");
        sendPacket(23, new byte[]{(byte)npcId});
        if (!menuLatch.await(3500, TimeUnit.MILLISECONDS) || !gotMenu) {
            log(npcName + " did not return menu");
            return false;
        }

        log("Selecting option " + option + " from " + npcName);
        ByteArrayOutputStream selectBytes = new ByteArrayOutputStream();
        DataOutputStream selectOut = new DataOutputStream(selectBytes);
        selectOut.writeShort(npcId);
        selectOut.writeByte(menuId);
        selectOut.writeByte(option);
        sendPacket(-30, selectBytes.toByteArray());

        if (!shopLatch.await(3500, TimeUnit.MILLISECONDS) || !gotShop) {
            log(npcName + " did not return shop");
            return false;
        }

        log("Shop opened via " + npcName + ": '" + shopTitle + "'");
        return true;
    }

    @Override
    public void run() {
        boolean sellSent = false;
        try {
            for (Class<?> c : inst.getAllLoadedClasses()) {
                if (c.getName().equals("GameScreen.GameScreen") && c.getField("player").get(null) != null) {
                    game = c;
                    break;
                }
            }
            if (game == null) throw new IllegalStateException("Game not found");
            ClassLoader loader = game.getClassLoader();
            canvas = Class.forName("Main.GameCanvas", false, loader);
            messageClass = Class.forName("net.Message", false, loader);
            player = game.getField("player").get(null);
            map = canvas.getField("loadmap").get(null);
            session = call(Class.forName("CLib.Session_ME", false, loader).getMethod("gI"), null);
            area = ((Number)Class.forName("Thread_More.LoadMap", false, loader).getField("Area").get(null)).intValue();
            currentMapId = num(map, "idMap");
            x = num(player, "x");
            y = num(player, "y");
            coinBefore = coins();

            log("INIT name=" + field(player, "name") + " map=" + currentMapId
                    + " area=" + area + " pos=(" + x + "," + y + ") coin=" + coinBefore);

            Class<?> itemClass = Class.forName("GameObjects.Item", false, loader);
            Object vecInventory = itemClass.getField("VecInvetoryPlayer").get(null);
            if (vecInventory == null) throw new IllegalStateException("VecInvetoryPlayer is null");

            if (!selectTargetItem(vecInventory)) {
                throw new IllegalStateException("No unlocked, tier-0 equipment found");
            }

            // Install message capture proxy
            originalHandler = field(session, "messageHandler");
            proxyHandler = Proxy.newProxyInstance(loader,
                    new Class<?>[]{Class.forName("net.IMessageHandler", false, loader)},
                    this::receive);
            session.getClass().getField("messageHandler").set(session, proxyHandler);

            // Step 1: Open Shop
            boolean shopOpened = false;
            if (currentMapId == 1) {
                shopOpened = openNpcShop(-3, "Lisa");
            } else if (currentMapId == 19) {
                shopOpened = openNpcShop(-57, "Dylan");
            } else {
                shopOpened = openNpcShop(-57, "Dylan");
                if (!shopOpened) shopOpened = openNpcShop(-58, "Graham");
                if (!shopOpened) shopOpened = openNpcShop(-3, "Lisa");
            }

            if (!shopOpened) {
                throw new IllegalStateException("Could not open any shop on Map " + currentMapId);
            }

            // Step 2: Send Opcode 18 to request selling
            Thread.sleep(200);
            ByteArrayOutputStream sellBytes = new ByteArrayOutputStream();
            DataOutputStream sellOut = new DataOutputStream(sellBytes);
            sellOut.writeByte((byte)targetCat);
            sellOut.writeShort((short)targetId);
            sellOut.writeByte(1); // typedelete = 1
            sellSent = true;
            sendPacket(18, sellBytes.toByteArray());
            log("TX DEL_ITEM: Sent sell request for '" + targetName + "' id=" + targetId);

            // Step 3: Wait for confirmation dialog (Opcode -32)
            boolean dialogArrived = dialogLatch.await(3000, TimeUnit.MILLISECONDS);
            if (dialogArrived && lastDialogId != Integer.MIN_VALUE) {
                log("CONFIRMATION DIALOG DETECTED: id=" + lastDialogId + " type=" + lastDialogType + " text='" + lastDialogText + "'");
                Thread.sleep(150);

                // Step 4: Send Opcode -32 confirmation: [short dialogId, byte dialogType, byte 1]
                ByteArrayOutputStream confirmBytes = new ByteArrayOutputStream();
                DataOutputStream confirmOut = new DataOutputStream(confirmBytes);
                confirmOut.writeShort((short)lastDialogId);
                confirmOut.writeByte((byte)lastDialogType);
                confirmOut.writeByte(1); // 1 = OK / Agree
                confirmationSent = true;
                sendPacket(-32, confirmBytes.toByteArray());
                log("TX DIALOG_CONFIRM: Sent agreement [id=" + lastDialogId + ", type=" + lastDialogType + ", value=1]");
            }

            // Step 5: Await Opcode 16 inventory update
            long until = System.currentTimeMillis() + 8000;
            while (System.currentTimeMillis() < until) {
                guard();
                if (saleSuccess || inventoryUpdates > 0) break;
                Thread.sleep(100);
            }

            long coinAfter = coins();
            log("FINAL_RESULT: sellSent=" + sellSent
                    + " confirmationSent=" + confirmationSent
                    + " inventoryUpdates=" + inventoryUpdates
                    + " saleSuccess=" + saleSuccess
                    + " coin=" + coinBefore + " -> " + coinAfter
                    + " (diff=+" + (coinAfter - coinBefore) + " Vàng)");

        } catch (Throwable t) {
            log("ABORT: error=" + t);
        } finally {
            try {
                if (proxyHandler != null && session != null
                        && field(session, "messageHandler") == proxyHandler) {
                    session.getClass().getField("messageHandler").set(session, originalHandler);
                    log("CLEANUP: original messageHandler restored successfully");
                }
            } catch (Throwable t) {
                log("CLEANUP_ERROR: " + t);
            }
            System.clearProperty("hso.dylanProbeV4.running");
        }
    }
}
