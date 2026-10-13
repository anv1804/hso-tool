import java.io.*;
import java.lang.instrument.Instrumentation;
import java.lang.invoke.*;
import java.lang.reflect.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.concurrent.*;

/**
 * Diagnostic Probe V3 for Direction 1:
 * Dynamically tests selling an item (Opcode 18) during an active server shop session.
 * 1. Audits inventory, prioritizes unlocked tier-0 equipment (Cat 3).
 * 2. Attempts to open Dylan's shop (NPC -57).
 * 3. If Dylan does not reply on current map, attempts Graham (NPC -58).
 * 4. As soon as any server shop (Opcode 23) is confirmed, sends Opcode 18 (typedelete=1).
 * 5. Records all inventory/coin/dialog/notice responses.
 */
public final class HsoDylanShopSellProbeV3 implements Runnable {
    private final Instrumentation inst;
    private final Path root;
    private final boolean auditOnly;
    private Class<?> game, canvas, messageClass;
    private Object player, map, session, originalHandler, proxyHandler;
    private int currentMapId, area, x, y, activeNpc = -57, menuId = -1, option = -1;
    private long coinBefore;
    private CountDownLatch menuLatch = new CountDownLatch(1);
    private CountDownLatch shopLatch = new CountDownLatch(1);
    private volatile boolean gotMenu = false;
    private volatile boolean gotShop = false;
    private volatile String shopTitle = "";
    private volatile int inventoryUpdates = 0;
    private volatile boolean saleSuccess = false;
    private volatile int lastDialogId = Integer.MIN_VALUE;
    private volatile int lastDialogType = -1;
    private volatile String lastDialogText = "";
    private volatile String lastNotice = "";
    private volatile String lastInfo37 = "";

    // Target item selected for sale
    private int targetId = -1;
    private int targetCat = -1;
    private String targetName = "";
    private int targetQtyBefore = 0;
    private int targetTier = 0;
    private int targetLock = 0;

    private HsoDylanShopSellProbeV3(Instrumentation inst, String args) {
        this.inst = inst;
        String[] parts = args.split("\\|", 4);
        root = Paths.get(parts[1]);
        auditOnly = parts.length >= 4 && "audit".equalsIgnoreCase(parts[3]);
    }

    public static synchronized void agentmain(String args, Instrumentation inst) {
        String key = "hso.dylanProbeV3.running";
        if (System.getProperty(key) != null) {
            throw new IllegalStateException("Probe V3 already running");
        }
        System.setProperty(key, "true");
        Thread t = new Thread(new HsoDylanShopSellProbeV3(inst, args), "HSO Dylan shop sell probe V3");
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
                + " PROBE_V3 " + s + "\n";
        try {
            Files.write(root.resolve("hso_remote_sell_probe.log"),
                    line.getBytes(StandardCharsets.UTF_8),
                    StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        } catch (IOException ignored) {}
        System.setProperty("hso.dylanProbeV3.status", s);
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
                            log("SALE_CONFIRMED! Coin increased: " + coinBefore + " -> " + currentCoins);
                        }
                    } else if (cmd == -32) {
                        int id = in.readShort();
                        int type = in.readByte();
                        String text = in.readUTF();
                        lastDialogId = id;
                        lastDialogType = type;
                        lastDialogText = text;
                        log("RX dialog cmd=-32 id=" + id + " type=" + type + " text='" + text + "'");
                    } else if (cmd == 37) {
                        String text = in.readUTF();
                        String extra = in.readUTF();
                        int type = in.readByte();
                        lastInfo37 = text;
                        log("RX info37 type=" + type + " text='" + text.replace('\n', '|') + "' extra='" + extra + "'");
                    } else if (cmd == 53) {
                        String text = in.readUTF();
                        lastNotice = text;
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
        Object chosenEquipment = null;
        Object chosenOther = null;

        for (int i = 0; i < size; i++) {
            Object it = call(vecInventory.getClass().getMethod("elementAt", int.class), vecInventory, i);
            if (it == null) continue;
            int id = num(it, "Id");
            int cat = num(it, "ItemCatagory");
            String name = (String)field(it, "itemName");
            int lock = num(it, "isLock");
            int tier = num(it, "tier");
            int qty = cat == 3 ? 1 : Math.max(1, num(it, "numPotion"));
            int canSell = num(it, "canSell");

            log("ITEM [" + i + "] id=" + id + " cat=" + cat + " name='" + name
                    + "' qty=" + qty + " tier=" + tier + " lock=" + lock + " canSell=" + canSell);

            // Prioritize unlocked tier 0 equipment (cat 3)
            if (lock == 0 && tier == 0) {
                if (cat == 3 && chosenEquipment == null) {
                    chosenEquipment = it;
                    targetId = id;
                    targetCat = cat;
                    targetName = name;
                    targetQtyBefore = qty;
                    targetTier = tier;
                    targetLock = lock;
                } else if ((cat == 4 || cat == 7) && chosenOther == null) {
                    chosenOther = it;
                }
            }
        }

        if (chosenEquipment != null) {
            log("TARGET_SELECTED (Equipment Cat 3): id=" + targetId + " name='" + targetName + "'");
            return true;
        } else if (chosenOther != null) {
            targetId = num(chosenOther, "Id");
            targetCat = num(chosenOther, "ItemCatagory");
            targetName = (String)field(chosenOther, "itemName");
            targetQtyBefore = Math.max(1, num(chosenOther, "numPotion"));
            targetTier = num(chosenOther, "tier");
            targetLock = num(chosenOther, "isLock");
            log("TARGET_SELECTED (Potion/Material): id=" + targetId + " cat=" + targetCat + " name='" + targetName + "'");
            return true;
        }
        return false;
    }

    private boolean tryNpcShop(int npcId, String npcName) throws Throwable {
        activeNpc = npcId;
        menuLatch = new CountDownLatch(1);
        shopLatch = new CountDownLatch(1);
        gotMenu = false;
        gotShop = false;
        option = -1;

        log("TRY NPC " + npcName + " (ID: " + npcId + ")");
        sendPacket(23, new byte[]{(byte)npcId});
        boolean gotM = menuLatch.await(3500, TimeUnit.MILLISECONDS);
        if (!gotM || !gotMenu) {
            log("NPC " + npcName + " did not return menu on Map " + currentMapId);
            return false;
        }

        log("Selecting option " + option + " from " + npcName);
        ByteArrayOutputStream selectBytes = new ByteArrayOutputStream();
        DataOutputStream selectOut = new DataOutputStream(selectBytes);
        selectOut.writeShort(npcId);
        selectOut.writeByte(menuId);
        selectOut.writeByte(option);
        sendPacket(-30, selectBytes.toByteArray());

        boolean gotS = shopLatch.await(3500, TimeUnit.MILLISECONDS);
        if (!gotS || !gotShop) {
            log("NPC " + npcName + " did not return shop on Map " + currentMapId);
            return false;
        }

        log("SUCCESS! Server shop opened via " + npcName + ": '" + shopTitle + "'");
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
                    + " area=" + area + " pos=(" + x + "," + y + ") coin=" + coinBefore
                    + " auditOnly=" + auditOnly);

            Class<?> itemClass = Class.forName("GameObjects.Item", false, loader);
            Object vecInventory = itemClass.getField("VecInvetoryPlayer").get(null);
            if (vecInventory == null) throw new IllegalStateException("VecInvetoryPlayer is null");

            boolean hasTarget = selectTargetItem(vecInventory);
            if (auditOnly) {
                log("AUDIT_COMPLETE: Found target=" + hasTarget + "; audit mode ends without sending packets");
                return;
            }

            if (!hasTarget) {
                throw new IllegalStateException("No safe, unlocked, tier-0 item found in inventory to test selling");
            }

            // Install message capture proxy
            originalHandler = field(session, "messageHandler");
            proxyHandler = Proxy.newProxyInstance(loader,
                    new Class<?>[]{Class.forName("net.IMessageHandler", false, loader)},
                    this::receive);
            session.getClass().getField("messageHandler").set(session, proxyHandler);

            // Step 1: Attempt Dylan (-57)
            boolean shopOpened = tryNpcShop(-57, "Dylan");
            if (!shopOpened) {
                // Step 2: Fallback to Graham (-58)
                shopOpened = tryNpcShop(-58, "Graham");
            }
            if (!shopOpened) {
                // Step 3: Fallback to Lisa (-3)
                shopOpened = tryNpcShop(-3, "Lisa");
            }

            if (!shopOpened) {
                log("NO_SERVER_SHOP: Server did not return a shop from any tested NPC on Map " + currentMapId);
                return;
            }

            log("STEP 3: ACTIVE SHOP CONFIRMED! Title='" + shopTitle + "'. Testing Opcode 18 DEL_ITEM...");
            Thread.sleep(200);

            // Send Opcode 18: [byte cat, short id, byte 1]
            ByteArrayOutputStream sellBytes = new ByteArrayOutputStream();
            DataOutputStream sellOut = new DataOutputStream(sellBytes);
            sellOut.writeByte((byte)targetCat);
            sellOut.writeShort((short)targetId);
            sellOut.writeByte(1); // typedelete = 1 (Sell for gold)
            sellSent = true;
            sendPacket(18, sellBytes.toByteArray());
            log("TX DEL_ITEM: Sent sell packet for '" + targetName + "' id=" + targetId
                    + " cat=" + targetCat + " typedelete=1 during active shop session!");

            // Await outcome for up to 8 seconds
            long until = System.currentTimeMillis() + 8000;
            while (System.currentTimeMillis() < until) {
                guard();
                if (saleSuccess || inventoryUpdates > 0) break;
                Thread.sleep(100);
            }

            long coinAfter = coins();
            log("OUTCOME: sellSent=" + sellSent
                    + " inventoryUpdates=" + inventoryUpdates
                    + " saleSuccess=" + saleSuccess
                    + " coin=" + coinBefore + " -> " + coinAfter
                    + " (diff=" + (coinAfter - coinBefore) + ")"
                    + " lastDialog='" + lastDialogText + "' (type=" + lastDialogType + ")"
                    + " lastInfo37='" + lastInfo37 + "'"
                    + " lastNotice='" + lastNotice + "'");

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
            System.clearProperty("hso.dylanProbeV3.running");
        }
    }
}
