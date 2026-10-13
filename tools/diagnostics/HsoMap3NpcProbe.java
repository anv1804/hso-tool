import java.io.*;
import java.lang.instrument.Instrumentation;
import java.lang.invoke.*;
import java.lang.reflect.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.concurrent.*;

/**
 * Diagnostic Probe to test in-map NPCs on Map 3 (Bìa Rừng):
 * Miss Keva (ID: -90) and Mr Edgar (ID: -85).
 * Tests if either NPC provides a server menu/shop that permits selling items.
 */
public final class HsoMap3NpcProbe implements Runnable {
    private final Instrumentation inst;
    private final Path root;
    private Class<?> game, canvas, messageClass;
    private Object player, map, session, originalHandler, proxyHandler;
    private int currentMapId, area, x, y;
    private long coinBefore;

    private volatile int currentTestingNpc = 0;
    private CountDownLatch menuLatch = new CountDownLatch(1);
    private CountDownLatch shopLatch = new CountDownLatch(1);
    private CountDownLatch dialogLatch = new CountDownLatch(1);

    private volatile boolean gotMenu = false;
    private volatile int lastMenuId = -1;
    private volatile int targetOption = -1;
    private volatile boolean gotShop = false;
    private volatile String shopTitle = "";
    private volatile int inventoryUpdates = 0;
    private volatile boolean saleSuccess = false;
    private volatile int lastDialogId = Integer.MIN_VALUE;
    private volatile int lastDialogType = -1;
    private volatile String lastDialogText = "";
    private volatile boolean confirmationSent = false;

    // Target item
    private int targetId = 28;
    private int targetCat = 3;
    private String targetName = "Áo vải xanh";

    private HsoMap3NpcProbe(Instrumentation inst, String args) {
        this.inst = inst;
        String[] parts = args.split("\\|", 3);
        root = Paths.get(parts[1]);
    }

    public static synchronized void agentmain(String args, Instrumentation inst) {
        String key = "hso.map3NpcProbe.running";
        if (System.getProperty(key) != null) {
            throw new IllegalStateException("Map 3 NPC Probe is already running");
        }
        System.setProperty(key, "true");
        Thread t = new Thread(new HsoMap3NpcProbe(inst, args), "HSO Map 3 NPC Probe");
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
                + " MAP3_NPC_PROBE " + s + "\n";
        try {
            Files.write(root.resolve("hso_remote_sell_probe.log"),
                    line.getBytes(StandardCharsets.UTF_8),
                    StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        } catch (IOException ignored) {}
        System.setProperty("hso.map3NpcProbe.status", s);
    }

    private void guard() throws Throwable {
        if (!(Boolean)call(session.getClass().getMethod("isConnected"), session)
                || game.getField("player").get(null) != player
                || canvas.getField("loadmap").get(null) != map) {
            throw new IllegalStateException("Disconnected or context changed");
        }
    }

    private Object receive(Object obj, Method method, Object[] args) throws Throwable {
        if (method.getName().equals("onMessage") && args != null && args.length == 1) {
            Object msg = args[0];
            int cmd = num(msg, "command");
            if (cmd == -30 || cmd == 23 || cmd == 16 || cmd == -32 || cmd == 37 || cmd == 53 || cmd == 60) {
                DataInputStream in = (DataInputStream)call(messageClass.getMethod("reader"), msg);
                in.mark(in.available() + 1);
                try {
                    if (cmd == -30) {
                        int npc = in.readShort();
                        int id = in.readByte();
                        int count = in.readUnsignedByte();
                        if (count <= 32) {
                            StringBuilder labels = new StringBuilder();
                            int shopOpt = -1;
                            for (int i = 0; i < count; i++) {
                                String label = in.readUTF();
                                labels.append('[').append(i).append(':').append(label).append(']');
                                String lower = label.toLowerCase();
                                if (lower.contains("mua") || lower.contains("bán") || lower.contains("cửa hàng") || lower.contains("shop")) {
                                    shopOpt = i;
                                }
                            }
                            lastMenuId = id;
                            targetOption = shopOpt >= 0 ? shopOpt : 0;
                            gotMenu = true;
                            log("RX menu npc=" + npc + " id=" + id + " options=" + labels + " -> selectedOption=" + targetOption);
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
                    } else if (cmd == 60) {
                        log("RX chat_npc60");
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

    private boolean testNpc(int npcId, String npcName) throws Throwable {
        currentTestingNpc = npcId;
        menuLatch = new CountDownLatch(1);
        shopLatch = new CountDownLatch(1);
        gotMenu = false;
        gotShop = false;
        lastMenuId = -1;
        targetOption = -1;

        log("--- TESTING NPC " + npcName + " (ID: " + npcId + ") ON MAP " + currentMapId + " ---");
        // Request menu from NPC: Opcode 23 [byte npcId]
        sendPacket(23, new byte[]{(byte)npcId});

        boolean menuArrived = menuLatch.await(3500, TimeUnit.MILLISECONDS);
        if (!menuArrived || !gotMenu) {
            log(npcName + " did not return any Dynamic_Menu within 3.5s");
            return false;
        }

        log(npcName + " returned menuId=" + lastMenuId + "! Selecting option " + targetOption + "...");
        ByteArrayOutputStream sel = new ByteArrayOutputStream();
        DataOutputStream out = new DataOutputStream(sel);
        sel.reset();
        out.writeShort(npcId);
        out.writeByte(lastMenuId);
        out.writeByte(targetOption);
        sendPacket(-30, sel.toByteArray());

        boolean shopArrived = shopLatch.await(3500, TimeUnit.MILLISECONDS);
        if (!shopArrived || !gotShop) {
            log(npcName + " option " + targetOption + " did not return a shop");
            return false;
        }

        log("SUCCESS: Server shop opened via " + npcName + "! Shop Title: '" + shopTitle + "'");

        // Now test Opcode 18 selling!
        Thread.sleep(200);
        ByteArrayOutputStream sellBytes = new ByteArrayOutputStream();
        DataOutputStream sellOut = new DataOutputStream(sellBytes);
        sellOut.writeByte((byte)targetCat);
        sellOut.writeShort((short)targetId);
        sellOut.writeByte(1); // typedelete = 1
        dialogLatch = new CountDownLatch(1);
        sendPacket(18, sellBytes.toByteArray());
        log("TX DEL_ITEM: Sent sell packet for " + targetName + " id=" + targetId + " cat=" + targetCat);

        // Wait for dialog confirmation
        boolean dialogArrived = dialogLatch.await(3000, TimeUnit.MILLISECONDS);
        if (dialogArrived && lastDialogId != Integer.MIN_VALUE) {
            log("CONFIRMATION DIALOG RECEIVED: id=" + lastDialogId + " type=" + lastDialogType + " text='" + lastDialogText + "'");
            Thread.sleep(150);
            ByteArrayOutputStream confirmBytes = new ByteArrayOutputStream();
            DataOutputStream confirmOut = new DataOutputStream(confirmBytes);
            confirmOut.writeShort((short)lastDialogId);
            confirmOut.writeByte((byte)lastDialogType);
            confirmOut.writeByte(1);
            confirmationSent = true;
            sendPacket(-32, confirmBytes.toByteArray());
            log("TX DIALOG_CONFIRM sent!");
        }

        // Wait for inventory update / coin update
        long until = System.currentTimeMillis() + 8000;
        while (System.currentTimeMillis() < until) {
            guard();
            if (saleSuccess || inventoryUpdates > 0) break;
            Thread.sleep(100);
        }

        return saleSuccess;
    }

    private void findTargetItem(Object vecInventory) throws Throwable {
        int size = (Integer)call(vecInventory.getClass().getMethod("size"), vecInventory);
        for (int i = 0; i < size; i++) {
            Object it = call(vecInventory.getClass().getMethod("elementAt", int.class), vecInventory, i);
            if (it == null) continue;
            int cat = num(it, "ItemCatagory");
            int id = num(it, "Id");
            int lock = num(it, "isLock");
            int tier = num(it, "tier");
            String name = (String)field(it, "itemName");

            if (cat == 3 && lock == 0 && tier == 0) {
                targetId = id;
                targetCat = cat;
                targetName = name;
                log("CHOSEN TEST ITEM: " + name + " (id=" + id + ", cat=" + cat + ")");
                return;
            }
        }
    }

    @Override
    public void run() {
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

            log("START name=" + field(player, "name") + " map=" + currentMapId
                    + " area=" + area + " pos=(" + x + "," + y + ") coin=" + coinBefore);

            Class<?> itemClass = Class.forName("GameObjects.Item", false, loader);
            Object vecInventory = itemClass.getField("VecInvetoryPlayer").get(null);
            if (vecInventory != null) findTargetItem(vecInventory);

            // Hook messageHandler
            originalHandler = field(session, "messageHandler");
            proxyHandler = Proxy.newProxyInstance(loader,
                    new Class<?>[]{Class.forName("net.IMessageHandler", false, loader)},
                    this::receive);
            session.getClass().getField("messageHandler").set(session, proxyHandler);

            // Test 1: Miss Keva (-90)
            boolean successKeva = testNpc(-90, "Miss Keva");

            if (!successKeva) {
                Thread.sleep(1000);
                // Test 2: Mr Edgar (-85)
                boolean successEdgar = testNpc(-85, "Mr Edgar");
            }

            long coinAfter = coins();
            log("SUMMARY: coin=" + coinBefore + " -> " + coinAfter + " (diff=" + (coinAfter - coinBefore) + ")"
                    + " inventoryUpdates=" + inventoryUpdates + " saleSuccess=" + saleSuccess);

        } catch (Throwable t) {
            log("ABORT: " + t);
        } finally {
            try {
                if (proxyHandler != null && session != null
                        && field(session, "messageHandler") == proxyHandler) {
                    session.getClass().getField("messageHandler").set(session, originalHandler);
                    log("CLEANUP: original messageHandler restored");
                }
            } catch (Throwable t) {
                log("CLEANUP_ERROR: " + t);
            }
            System.clearProperty("hso.map3NpcProbe.running");
        }
    }
}
