import java.io.*;
import java.lang.instrument.Instrumentation;
import java.lang.invoke.*;
import java.lang.reflect.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;

/**
 * Replays the exact original "vibecoded" sell logic from commit b997ac3
 * targeting the REAL item in inventory: Dây chuyền móng vuốt (id=12, cat=3).
 */
public final class HsoVibecodeProbe implements Runnable {
    private final Instrumentation inst;
    private final Path root;
    private Class<?> game, canvas, messageClass;
    private Object player, map, session, originalHandler, proxyHandler;
    private int currentMapId, area, startX, startY;
    private long coinBefore;

    private volatile boolean saleSuccess = false;
    private volatile boolean dialogConfirmed = false;
    private volatile int confirmedDialogId = -1;
    private volatile String lastDialogText = "";

    // Target item: Dây chuyền móng vuốt (id=12, cat=3)
    private int targetId = 12;
    private int targetCat = 3;
    private String targetName = "Dây chuyền móng vuốt";

    private HsoVibecodeProbe(Instrumentation inst, String args) {
        this.inst = inst;
        String[] parts = args.split("\\|", 3);
        root = Paths.get(parts[1]);
    }

    public static synchronized void agentmain(String args, Instrumentation inst) {
        String key = "hso.vibecodeProbe.running";
        if (System.getProperty(key) != null) {
            throw new IllegalStateException("Vibecode Probe already running");
        }
        System.setProperty(key, "true");
        Thread t = new Thread(new HsoVibecodeProbe(inst, args), "HSO Vibecode Probe");
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
                + " VIBECODE_V2 " + s + "\n";
        try {
            Files.write(root.resolve("hso_remote_sell_probe.log"),
                    line.getBytes(StandardCharsets.UTF_8),
                    StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        } catch (IOException ignored) {}
        System.setProperty("hso.vibecodeProbe.status", s);
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
                    log("RECV Dialog_server: '" + text + "' (id=" + id + ", type=" + type + ")");
                    if (text != null && text.toLowerCase().indexOf("bạn có muốn bán") != -1) {
                        log("Auto-confirming sell dialog: id=" + id + ", type=" + type);
                        dialogConfirmed = true;
                        confirmedDialogId = id;

                        // Send dialog confirmation: Opcode -32 [short id, byte type, byte 1]
                        ByteArrayOutputStream confirmBytes = new ByteArrayOutputStream();
                        DataOutputStream confirmOut = new DataOutputStream(confirmBytes);
                        confirmOut.writeShort((short)id);
                        confirmOut.writeByte((byte)type);
                        confirmOut.writeByte(1);
                        sendPacket(-32, confirmBytes.toByteArray());
                        log("TX DIALOG_CONFIRM sent for dialog id=" + id);
                    }
                } else if (cmd == 16) {
                    long currentCoins = coins();
                    log("RX inventory cmd=16 coin=" + currentCoins);
                    if (currentCoins > coinBefore) {
                        saleSuccess = true;
                        log("SALE_CONFIRMED! Coin: " + coinBefore + " -> " + currentCoins
                                + " (+" + (currentCoins - coinBefore) + " Vàng)");
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
                    log("RX Dynamic_Menu -30");
                } else if (cmd == 23) {
                    log("RX cmd=23");
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
            startX = num(player, "x");
            startY = num(player, "y");
            coinBefore = coins();

            log("START name=" + field(player, "name") + " map=" + currentMapId
                    + " area=" + area + " pos=(" + startX + "," + startY + ") coin=" + coinBefore
                    + " target=" + targetName + " id=" + targetId);

            // Install message capture proxy
            originalHandler = field(session, "messageHandler");
            proxyHandler = Proxy.newProxyInstance(loader,
                    new Class<?>[]{Class.forName("net.IMessageHandler", false, loader)},
                    this::receive);
            session.getClass().getField("messageHandler").set(session, proxyHandler);

            // RUN ATTEMPT 1: Original vibecode to Lisa (-3) at (756, 120)
            int npcId = -3;
            int npcX = 756;
            int npcY = 120;
            String npcName = "Lisa";

            log("--- ATTEMPT 1: VIBECODE FLOW TO " + npcName + " (id=" + npcId + ") at (" + npcX + "," + npcY + ") ---");
            log("1. Spoof move to (" + npcX + "," + npcY + ")");
            spoofMoveTo(npcX, npcY);

            log("2. getlist_from_npc(" + npcId + ")");
            sendPacket(23, new byte[]{(byte)npcId});
            Thread.sleep(300);

            log("3. delete_Item(cat=" + targetCat + ", id=" + targetId + ", typedelete=1)");
            ByteArrayOutputStream sellBytes = new ByteArrayOutputStream();
            DataOutputStream sellOut = new DataOutputStream(sellBytes);
            sellOut.writeByte((byte)targetCat);
            sellOut.writeShort((short)targetId);
            sellOut.writeByte(1);
            sendPacket(18, sellBytes.toByteArray());

            // Await dialog / outcome for 3.0 seconds
            long until = System.currentTimeMillis() + 3000;
            while (System.currentTimeMillis() < until) {
                if (saleSuccess || dialogConfirmed) break;
                Thread.sleep(50);
            }

            log("4. restoreVirtualMove to (" + startX + "," + startY + ")");
            restoreVirtualMove(startX, startY);

            // Wait additional 1.5s for inventory packet
            Thread.sleep(1500);

            long coinAfter = coins();
            log("FINAL RESULT: coin=" + coinBefore + " -> " + coinAfter + " (diff=" + (coinAfter - coinBefore) + ")"
                    + " dialogConfirmed=" + dialogConfirmed + " saleSuccess=" + saleSuccess);

        } catch (Throwable t) {
            log("ABORT: " + t);
        } finally {
            try {
                if (proxyHandler != null && session != null
                        && field(session, "messageHandler") == proxyHandler) {
                    session.getClass().getField("messageHandler").set(session, originalHandler);
                    log("CLEANUP: original messageHandler restored");
                }
            } catch (Throwable ignored) {}
            System.clearProperty("hso.vibecodeProbe.running");
        }
    }
}
