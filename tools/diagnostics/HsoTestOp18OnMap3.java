import java.io.*;
import java.lang.instrument.Instrumentation;
import java.lang.invoke.*;
import java.lang.reflect.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.concurrent.*;

/**
 * Diagnostic probe to test sending Opcode 18 directly on Map 3:
 * Captures all server responses (-32, 37, 53, 16, 18, 60, -30, 23).
 * If Opcode -32 arrives, confirms it immediately.
 */
public final class HsoTestOp18OnMap3 implements Runnable {
    private final Instrumentation inst;
    private final Path root;
    private Class<?> game, canvas, messageClass;
    private Object player, map, session, originalHandler, proxyHandler;
    private int currentMapId, area, x, y;
    private long coinBefore;

    private CountDownLatch rxLatch = new CountDownLatch(1);
    private volatile boolean rxAny = false;
    private volatile int lastDialogId = Integer.MIN_VALUE;
    private volatile int lastDialogType = -1;
    private volatile String lastDialogText = "";
    private volatile boolean saleSuccess = false;

    // Target item
    private int targetId = 28;
    private int targetCat = 3;
    private String targetName = "Áo vải xanh";

    private HsoTestOp18OnMap3(Instrumentation inst, String args) {
        this.inst = inst;
        String[] parts = args.split("\\|", 3);
        root = Paths.get(parts[1]);
    }

    public static synchronized void agentmain(String args, Instrumentation inst) {
        String key = "hso.testOp18.running";
        if (System.getProperty(key) != null) {
            throw new IllegalStateException("TestOp18 is already running");
        }
        System.setProperty(key, "true");
        Thread t = new Thread(new HsoTestOp18OnMap3(inst, args), "HSO Test Op18 Map3 Probe");
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
                + " DIRECT_OP18 " + s + "\n";
        try {
            Files.write(root.resolve("hso_remote_sell_probe.log"),
                    line.getBytes(StandardCharsets.UTF_8),
                    StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        } catch (IOException ignored) {}
        System.setProperty("hso.testOp18.status", s);
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
            DataInputStream in = (DataInputStream)call(messageClass.getMethod("reader"), msg);
            in.mark(in.available() + 1);
            try {
                rxAny = true;
                if (cmd == -32) {
                    int id = in.readShort();
                    int type = in.readByte();
                    String text = in.readUTF();
                    lastDialogId = id;
                    lastDialogType = type;
                    lastDialogText = text;
                    log("RX dialog cmd=-32 id=" + id + " type=" + type + " text='" + text + "'");
                    rxLatch.countDown();
                } else if (cmd == 16) {
                    long currentCoins = coins();
                    log("RX inventory cmd=16 coin=" + currentCoins);
                    if (currentCoins > coinBefore) {
                        saleSuccess = true;
                        log("SALE_CONFIRMED! Coin increased: " + coinBefore + " -> " + currentCoins);
                    }
                    rxLatch.countDown();
                } else if (cmd == 37) {
                    String text = in.readUTF();
                    String extra = in.readUTF();
                    int type = in.readByte();
                    log("RX info37 type=" + type + " text='" + text.replace('\n', '|') + "' extra='" + extra + "'");
                    rxLatch.countDown();
                } else if (cmd == 53) {
                    String text = in.readUTF();
                    log("RX notice53: '" + text + "'");
                    rxLatch.countDown();
                } else if (cmd == 23) {
                    String title = in.readUTF();
                    int type = in.readByte();
                    log("RX cmd=23 type=" + type + " text='" + title + "'");
                } else if (cmd == 60) {
                    log("RX chat_npc60");
                } else {
                    log("RX cmd=" + cmd);
                }
            } catch (Throwable error) {
                log("RX parse error cmd=" + cmd + ": " + error);
            } finally {
                in.reset();
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
                    + " area=" + area + " pos=(" + x + "," + y + ") coin=" + coinBefore
                    + " isMapLang=" + game.getField("isMapLang").get(null));

            // Verify item in inventory
            Class<?> itemClass = Class.forName("GameObjects.Item", false, loader);
            Object vecInventory = itemClass.getField("VecInvetoryPlayer").get(null);
            int size = (Integer)call(vecInventory.getClass().getMethod("size"), vecInventory);
            boolean found = false;
            for (int i = 0; i < size; i++) {
                Object it = call(vecInventory.getClass().getMethod("elementAt", int.class), vecInventory, i);
                if (it == null) continue;
                if (num(it, "Id") == targetId && num(it, "ItemCatagory") == targetCat) {
                    found = true;
                    targetName = (String)field(it, "itemName");
                    break;
                }
            }
            if (!found) {
                log("Target item id=" + targetId + " not found in inventory!");
                return;
            }

            // Install message capture proxy
            originalHandler = field(session, "messageHandler");
            proxyHandler = Proxy.newProxyInstance(loader,
                    new Class<?>[]{Class.forName("net.IMessageHandler", false, loader)},
                    this::receive);
            session.getClass().getField("messageHandler").set(session, proxyHandler);

            // TEST 1: Send Opcode 18 directly without opening any NPC shop!
            log("TEST 1: Sending Opcode 18 directly for " + targetName + " id=" + targetId + " cat=" + targetCat + " typedelete=1");
            ByteArrayOutputStream sellBytes = new ByteArrayOutputStream();
            DataOutputStream sellOut = new DataOutputStream(sellBytes);
            sellOut.writeByte((byte)targetCat);
            sellOut.writeShort((short)targetId);
            sellOut.writeByte(1); // typedelete = 1
            sendPacket(18, sellBytes.toByteArray());

            // Await response for 4 seconds
            boolean rx = rxLatch.await(4000, TimeUnit.MILLISECONDS);
            if (!rx) {
                log("TEST 1: Server gave NO response within 4.0s to raw Opcode 18!");
            } else if (lastDialogId != Integer.MIN_VALUE) {
                log("TEST 1: DIALOG ARRIVED! Confirming dialog id=" + lastDialogId + " type=" + lastDialogType);
                Thread.sleep(150);
                ByteArrayOutputStream confirmBytes = new ByteArrayOutputStream();
                DataOutputStream confirmOut = new DataOutputStream(confirmBytes);
                confirmOut.writeShort((short)lastDialogId);
                confirmOut.writeByte((byte)lastDialogType);
                confirmOut.writeByte(1);
                sendPacket(-32, confirmBytes.toByteArray());

                // Wait for coin update
                for (int i = 0; i < 30; i++) {
                    Thread.sleep(100);
                    if (saleSuccess) break;
                }
            }

            long coinAfter = coins();
            log("RESULT: coin=" + coinBefore + " -> " + coinAfter + " (diff=" + (coinAfter - coinBefore) + ")"
                    + " saleSuccess=" + saleSuccess);

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
            System.clearProperty("hso.testOp18.running");
        }
    }
}
