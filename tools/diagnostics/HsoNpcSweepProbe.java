import java.io.*;
import java.lang.instrument.Instrumentation;
import java.lang.invoke.*;
import java.lang.reflect.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.concurrent.*;

/**
 * Sweeps all NPC IDs from -1 to -100 to discover which NPCs respond with Dynamic_Menu
 * and which options actually open a Server Shop (Opcode 23) from the wild map!
 */
public final class HsoNpcSweepProbe implements Runnable {
    private final Instrumentation inst;
    private final Path root;
    private Class<?> game, canvas, messageClass;
    private Object player, map, session, originalHandler, proxyHandler;
    private int currentMapId;

    private CountDownLatch menuLatch;
    private CountDownLatch shopLatch;
    private volatile boolean gotMenu = false;
    private volatile int lastMenuId = -1;
    private volatile int lastMenuNpc = 0;
    private volatile String[] lastMenuOptions = new String[0];
    private volatile boolean gotShop = false;
    private volatile String shopTitle = "";
    private volatile int shopType = -1;

    private HsoNpcSweepProbe(Instrumentation inst, String args) {
        this.inst = inst;
        String[] parts = args.split("\\|", 3);
        root = Paths.get(parts[1]);
    }

    public static synchronized void agentmain(String args, Instrumentation inst) {
        String key = "hso.npcSweepProbe.running";
        if (System.getProperty(key) != null) {
            throw new IllegalStateException("Npc Sweep Probe already running");
        }
        System.setProperty(key, "true");
        Thread t = new Thread(new HsoNpcSweepProbe(inst, args), "HSO NPC Sweep Probe");
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

    private synchronized void log(String s) {
        String line = new java.text.SimpleDateFormat("HH:mm:ss.SSS").format(new java.util.Date())
                + " NPC_SWEEP " + s + "\n";
        try {
            Files.write(root.resolve("hso_remote_sell_probe.log"),
                    line.getBytes(StandardCharsets.UTF_8),
                    StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        } catch (IOException ignored) {}
        System.setProperty("hso.npcSweepProbe.status", s);
    }

    private Object receive(Object obj, Method method, Object[] args) throws Throwable {
        if (method.getName().equals("onMessage") && args != null && args.length == 1) {
            Object msg = args[0];
            int cmd = num(msg, "command");
            if (cmd == -30 || cmd == 23) {
                DataInputStream in = (DataInputStream)call(messageClass.getMethod("reader"), msg);
                in.mark(in.available() + 1);
                try {
                    if (cmd == -30) {
                        int npc = in.readShort();
                        int id = in.readByte();
                        int count = in.readUnsignedByte();
                        String[] opts = new String[count];
                        for (int i = 0; i < count; i++) opts[i] = in.readUTF();
                        lastMenuNpc = npc;
                        lastMenuId = id;
                        lastMenuOptions = opts;
                        gotMenu = true;
                        if (menuLatch != null) menuLatch.countDown();
                    } else if (cmd == 23) {
                        String title = in.readUTF();
                        int type = in.readByte();
                        if (type >= 0) {
                            shopTitle = title;
                            shopType = type;
                            gotShop = true;
                            if (shopLatch != null) shopLatch.countDown();
                        }
                    }
                } catch (Throwable ignored) {
                } finally {
                    in.reset();
                }
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
            currentMapId = num(map, "idMap");

            log("SWEEP START on map=" + currentMapId + " player=" + field(player, "name"));

            originalHandler = field(session, "messageHandler");
            proxyHandler = Proxy.newProxyInstance(loader,
                    new Class<?>[]{Class.forName("net.IMessageHandler", false, loader)},
                    this::receive);
            session.getClass().getField("messageHandler").set(session, proxyHandler);

            // Test candidate NPC IDs that could have shop or functions
            int[] testNpcs = {
                -1, -2, -3, -4, -5, -6, -7, -8, -9, -10,
                -11, -12, -13, -14, -15, -16, -17, -18, -19, -20,
                -21, -22, -25, -26, -30, -32, -35, -36, -40, -42,
                -45, -50, -55, -57, -58, -60, -65, -70, -75, -77,
                -80, -81, -85, -90, -95, -99
            };

            for (int npcId : testNpcs) {
                gotMenu = false;
                menuLatch = new CountDownLatch(1);
                sendPacket(23, new byte[]{(byte)npcId});

                boolean rx = menuLatch.await(700, TimeUnit.MILLISECONDS);
                if (rx && gotMenu && lastMenuNpc == npcId) {
                    StringBuilder sb = new StringBuilder();
                    for (int i = 0; i < lastMenuOptions.length; i++) {
                        sb.append("[").append(i).append(":").append(lastMenuOptions[i]).append("]");
                    }
                    log("NPC " + npcId + " RETURNED MENU: " + sb);

                    // Test each option in the menu to see if any opens a real shop!
                    for (int opt = 0; opt < lastMenuOptions.length; opt++) {
                        String label = lastMenuOptions[opt].toLowerCase();
                        if (label.contains("mua") || label.contains("bán") || label.contains("shop")
                                || label.contains("cửa hàng") || label.contains("chiến binh")
                                || label.contains("sát thủ") || label.contains("pháp sư")
                                || label.contains("xạ thủ") || label.contains("trang bị")) {
                            gotShop = false;
                            shopLatch = new CountDownLatch(1);

                            ByteArrayOutputStream sel = new ByteArrayOutputStream();
                            DataOutputStream selOut = new DataOutputStream(sel);
                            selOut.writeShort(npcId);
                            selOut.writeByte(lastMenuId);
                            selOut.writeByte(opt);
                            sendPacket(-30, sel.toByteArray());

                            boolean shopRx = shopLatch.await(800, TimeUnit.MILLISECONDS);
                            if (shopRx && gotShop) {
                                log(">>> DISCOVERY! NPC " + npcId + " option [" + opt + ":" + lastMenuOptions[opt]
                                        + "] OPENED SHOP: title='" + shopTitle + "' type=" + shopType + " on map " + currentMapId + "!");
                            }
                        }
                    }
                }
                Thread.sleep(50);
            }

            log("SWEEP FINISHED!");

        } catch (Throwable t) {
            log("SWEEP ABORT: " + t);
        } finally {
            try {
                if (proxyHandler != null && session != null
                        && field(session, "messageHandler") == proxyHandler) {
                    session.getClass().getField("messageHandler").set(session, originalHandler);
                    log("CLEANUP: original messageHandler restored");
                }
            } catch (Throwable ignored) {}
            System.clearProperty("hso.npcSweepProbe.running");
        }
    }
}
