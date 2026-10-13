import java.io.*;
import java.lang.instrument.Instrumentation;
import java.lang.invoke.*;
import java.lang.reflect.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;

/**
 * Clean independent probe testing the exact original vibecoded sell flow.
 * Reads real item directly from player inventory.
 */
public final class HsoVibeRealTest implements Runnable {
    private final Instrumentation inst;
    private final Path root;
    private Class<?> gameClass, canvasClass, messageClass, itemClass;
    private Object player, map, session, originalHandler, proxyHandler;
    private int currentMapId, area, startX, startY;
    private long coinBefore;

    private volatile boolean saleSuccess = false;
    private volatile boolean dialogReceived = false;
    private volatile int confirmedDialogId = -1;
    private volatile String lastDialogText = "";

    private int targetId = -1;
    private int targetCat = -1;
    private String targetName = "";

    public HsoVibeRealTest(Instrumentation inst, String args) {
        this.inst = inst;
        String[] parts = args.split("\\|", 3);
        this.root = Paths.get(parts[1]);
    }

    public static void agentmain(String args, Instrumentation inst) {
        Thread t = new Thread(new HsoVibeRealTest(inst, args), "HSO Vibe Real Test");
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
                + " [VIBE_REAL] " + s + "\n";
        try {
            Files.write(root.resolve("hso_remote_sell_probe.log"),
                    line.getBytes(StandardCharsets.UTF_8),
                    StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        } catch (IOException ignored) {}
        System.out.println("[VIBE_REAL] " + s);
    }

    private Object receive(Object obj, Method method, Object[] args) throws Throwable {
        if (method.getName().equals("onMessage") && args != null && args.length == 1) {
            Object msg = args[0];
            int cmd = num(msg, "command");
            DataInputStream in = (DataInputStream)call(messageClass.getMethod("reader"), msg);
            in.mark(in.available() + 1);
            try {
                if (cmd == -32) {
                    int id = in.readShort();
                    int type = in.readByte();
                    String text = in.readUTF();
                    lastDialogText = text;
                    dialogReceived = true;
                    log("RECV Dialog_server (-32): '" + text + "' (id=" + id + ", type=" + type + ")");
                    
                    // Auto-confirm sell dialog
                    log("Auto-confirming sell dialog immediately: id=" + id + ", type=" + type);
                    confirmedDialogId = id;
                    ByteArrayOutputStream confirmBytes = new ByteArrayOutputStream();
                    DataOutputStream confirmOut = new DataOutputStream(confirmBytes);
                    confirmOut.writeShort((short)id);
                    confirmOut.writeByte((byte)type);
                    confirmOut.writeByte(1);
                    sendPacket(-32, confirmBytes.toByteArray());
                    log("TX DIALOG_CONFIRM sent for dialog id=" + id);
                } else if (cmd == 16) {
                    long currentCoins = coins();
                    log("RX inventory cmd=16 coin=" + currentCoins);
                    if (currentCoins > coinBefore) {
                        saleSuccess = true;
                        log("SALE_CONFIRMED! Coin: " + coinBefore + " -> " + currentCoins
                                + " (+" + (currentCoins - coinBefore) + " Vang)");
                    }
                } else if (cmd == 37) {
                    String text = in.readUTF();
                    String extra = in.readUTF();
                    int type = in.readByte();
                    log("RX info37 type=" + type + " text='" + text.replace('\n', '|') + "'");
                } else if (cmd == 53) {
                    String text = in.readUTF();
                    log("RX notice53: '" + text + "'");
                } else if (cmd == -30) {
                    log("RX Dynamic_Menu -30 received from server");
                } else if (cmd == 23) {
                    log("RX cmd=23 shop/menu received from server");
                }
            } catch (Throwable error) {
                log("CAPTURE error cmd=" + cmd + ": " + error);
            } finally {
                in.reset();
            }
        }
        return call(method, originalHandler, args == null ? new Object[0] : args);
    }

    private void sendPacket(int cmd, byte[] data) throws Throwable {
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

    private void spoofMoveTo(int targetX, int targetY) throws Throwable {
        int curX = startX;
        int curY = startY;
        int dx = curX - targetX;
        int dy = curY - targetY;
        int dist = (int)Math.sqrt(dx * dx + dy * dy);
        if (dist > 300) {
            int midX = (curX + targetX) / 2;
            int midY = (curY + targetY) / 2;
            ByteArrayOutputStream mid = new ByteArrayOutputStream();
            DataOutputStream midOut = new DataOutputStream(mid);
            midOut.writeShort((short)midX);
            midOut.writeShort((short)midY);
            sendPacket(4, mid.toByteArray());
            Thread.sleep(15);
        }
        ByteArrayOutputStream tgt = new ByteArrayOutputStream();
        DataOutputStream tgtOut = new DataOutputStream(tgt);
        tgtOut.writeShort((short)targetX);
        tgtOut.writeShort((short)targetY);
        sendPacket(4, tgt.toByteArray());
        Thread.sleep(35);
    }

    private void restoreVirtualMove(int restoreX, int restoreY) throws Throwable {
        ByteArrayOutputStream rst = new ByteArrayOutputStream();
        DataOutputStream rstOut = new DataOutputStream(rst);
        rstOut.writeShort((short)restoreX);
        rstOut.writeShort((short)restoreY);
        sendPacket(4, rst.toByteArray());
    }

    @Override
    public void run() {
        try {
            for (Class<?> c : inst.getAllLoadedClasses()) {
                if (c.getName().equals("GameScreen.GameScreen") && c.getField("player").get(null) != null) {
                    gameClass = c;
                    break;
                }
            }
            if (gameClass == null) throw new IllegalStateException("Game not found");
            ClassLoader loader = gameClass.getClassLoader();
            canvasClass = Class.forName("Main.GameCanvas", false, loader);
            messageClass = Class.forName("net.Message", false, loader);
            itemClass = Class.forName("GameObjects.Item", false, loader);
            player = gameClass.getField("player").get(null);
            map = canvasClass.getField("loadmap").get(null);
            session = call(Class.forName("CLib.Session_ME", false, loader).getMethod("gI"), null);
            area = ((Number)Class.forName("Thread_More.LoadMap", false, loader).getField("Area").get(null)).intValue();
            currentMapId = num(map, "idMap");
            startX = num(player, "x");
            startY = num(player, "y");
            coinBefore = coins();

            // Find real target item in player inventory
            Object vec = itemClass.getField("VecInvetoryPlayer").get(null);
            int size = (Integer)vec.getClass().getMethod("size").invoke(vec);
            for (int i = 0; i < size; i++) {
                Object it = vec.getClass().getMethod("elementAt", int.class).invoke(vec, i);
                if (it == null) continue;
                int id = ((Number)it.getClass().getField("Id").get(it)).intValue();
                int cat = ((Number)it.getClass().getField("ItemCatagory").get(it)).intValue();
                int lock = ((Number)it.getClass().getField("isLock").get(it)).intValue();
                int tier = ((Number)it.getClass().getField("tier").get(it)).intValue();
                int color = ((Number)it.getClass().getField("colorNameItem").get(it)).intValue();
                String name = (String)it.getClass().getField("itemName").get(it);

                // Priority: Equipment (cat == 3), unlocked (lock == 0), tier == 0
                if (cat == 3 && lock == 0 && tier == 0) {
                    targetId = id;
                    targetCat = cat;
                    targetName = name;
                    break;
                }
            }

            if (targetId == -1) {
                log("NO ELIGIBLE ITEM FOUND IN INVENTORY! Aborting test.");
                return;
            }

            log("START player=" + field(player, "name") + " map=" + currentMapId
                    + " area=" + area + " pos=(" + startX + "," + startY + ") coin=" + coinBefore
                    + " TARGET_ITEM: '" + targetName + "' id=" + targetId + " cat=" + targetCat);

            // Install message capture proxy
            originalHandler = field(session, "messageHandler");
            proxyHandler = Proxy.newProxyInstance(loader,
                    new Class<?>[]{Class.forName("net.IMessageHandler", false, loader)},
                    this::receive);
            session.getClass().getField("messageHandler").set(session, proxyHandler);

            // EXECUTE VIBECODE FLOW
            int npcId = -3;
            int npcX = 756;
            int npcY = 120;
            String npcName = "Lisa";

            log("=== STEP 1: Spoof move to " + npcName + " at (" + npcX + "," + npcY + ") ===");
            spoofMoveTo(npcX, npcY);

            log("=== STEP 2: Send getlist_from_npc(" + npcId + ") ===");
            sendPacket(23, new byte[]{(byte)npcId});
            Thread.sleep(300);

            log("=== STEP 3: Send delete_Item(cat=" + targetCat + ", id=" + targetId + ", typedelete=1) ===");
            ByteArrayOutputStream sellBytes = new ByteArrayOutputStream();
            DataOutputStream sellOut = new DataOutputStream(sellBytes);
            sellOut.writeByte((byte)targetCat);
            sellOut.writeShort((short)targetId);
            sellOut.writeByte(1);
            sendPacket(18, sellBytes.toByteArray());

            // Await dialog / outcome for 4.0 seconds
            long until = System.currentTimeMillis() + 4000;
            while (System.currentTimeMillis() < until) {
                if (saleSuccess || dialogReceived) break;
                Thread.sleep(50);
            }

            // Restore position
            log("=== STEP 4: Restore virtual position to (" + startX + "," + startY + ") ===");
            restoreVirtualMove(startX, startY);

            // Wait additional 1.5s for inventory / coin update
            Thread.sleep(1500);

            long coinAfter = coins();
            log("=== RESULT SUMMARY ===");
            log("Initial Coin: " + coinBefore + ", Final Coin: " + coinAfter + ", Diff: " + (coinAfter - coinBefore));
            log("Dialog Received: " + dialogReceived + " ('" + lastDialogText + "')");
            log("Sale Confirmed: " + saleSuccess);

        } catch (Throwable t) {
            log("EXCEPTION: " + t);
            t.printStackTrace();
        } finally {
            try {
                if (proxyHandler != null && session != null
                        && field(session, "messageHandler") == proxyHandler) {
                    session.getClass().getField("messageHandler").set(session, originalHandler);
                    log("CLEANUP: original messageHandler restored");
                }
            } catch (Throwable ignored) {}
        }
    }
}
