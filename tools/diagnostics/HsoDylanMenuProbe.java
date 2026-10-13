import java.io.*;
import java.lang.instrument.Instrumentation;
import java.lang.invoke.*;
import java.lang.reflect.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/** Bounded menu-only diagnostic: no movement, purchases, sales or dialog answers. */
public final class HsoDylanMenuProbe implements Runnable {
    private final Instrumentation instrumentation;
    private final Path logFile;
    private Object service, session, player, map, senderBefore, receiverBefore, sender, receiver;
    private Class<?> serviceClass, canvasClass, gameClass;
    private int initialMap, initialArea, menuId, option = -1;
    private volatile boolean gotShop;
    private final CountDownLatch gotMenu = new CountDownLatch(1);

    private HsoDylanMenuProbe(Instrumentation inst, String args) {
        instrumentation = inst;
        String[] parts = args.split("\\|", 2);
        logFile = Paths.get(parts[1]).resolve("hso_remote_sell_probe.log");
    }

    public static void agentmain(String args, Instrumentation inst) {
        Thread worker = new Thread(new HsoDylanMenuProbe(inst, args), "HSO Dylan menu-only diagnostic");
        worker.setDaemon(true);
        worker.start();
    }

    private static Object call(Method method, Object owner, Object... args) throws Throwable {
        MethodHandle handle = MethodHandles.publicLookup().unreflect(method);
        if (!Modifier.isStatic(method.getModifiers())) handle = handle.bindTo(owner);
        return handle.invokeWithArguments(args);
    }

    private static int number(Object owner, String name) throws Exception {
        return ((Number)owner.getClass().getField(name).get(owner)).intValue();
    }

    private synchronized void log(String text) {
        String line = new java.text.SimpleDateFormat("HH:mm:ss.SSS").format(new java.util.Date())
                + " DYLAN_PROBE " + text + "\n";
        try { Files.write(logFile, line.getBytes(StandardCharsets.UTF_8), StandardOpenOption.CREATE, StandardOpenOption.APPEND); }
        catch (IOException ignored) {}
        System.setProperty("hso.dylanProbe.status", text);
    }

    private Object proxy(Class<?> type, Object delegate, boolean outbound) {
        return Proxy.newProxyInstance(type.getClassLoader(), new Class<?>[]{type}, (obj, method, args) -> {
            if (args != null && args.length == 1 && (outbound && method.getName().equals("sendMessage")
                    || !outbound && method.getName().equals("onMessage"))) {
                try {
                    Object message = args[0];
                    int command = number(message, "command");
                    if (outbound && (command == 23 || command == -30)) {
                        byte[] bytes = (byte[])call(message.getClass().getMethod("getData"), message);
                        DataInputStream data = new DataInputStream(new ByteArrayInputStream(bytes));
                        if (command == 23 && bytes.length == 1 && bytes[0] == -57) log("TX root npc=-57 payload=C7");
                        if (command == -30 && bytes.length == 4 && data.readShort() == -57)
                            log("TX select npc=-57 menu=" + data.readByte() + " option=" + data.readUnsignedByte() + " payload=FFC7" + String.format("%02X%02X", bytes[2] & 255, bytes[3] & 255));
                    }
                    if (!outbound && (command == -30 || command == 23 || command == -32 || command == 53)) {
                        DataInputStream data = (DataInputStream)call(message.getClass().getMethod("reader"), message);
                        data.mark(data.available() + 1);
                        try {
                            if (command == -30) {
                                int npc = data.readShort(), menu = data.readByte(), count = data.readUnsignedByte();
                                if (npc == -57 && count <= 32) {
                                    int selected = -1;
                                    StringBuilder labels = new StringBuilder();
                                    for (int i = 0; i < count; ++i) {
                                        String label = data.readUTF(); labels.append('[').append(i).append(':').append(label).append(']');
                                        if ("Mua bán".equalsIgnoreCase(label)) selected = i;
                                    }
                                    menuId = menu; option = selected;
                                    log("RX root npc=" + npc + " menu=" + menu + " options=" + labels);
                                    gotMenu.countDown();
                                }
                            } else if (command == 23) {
                                String title = data.readUTF(); int shopType = data.readByte();
                                if (shopType >= 0) { gotShop = true; log("RX shop type=" + shopType + " title=" + title); }
                            } else if (command == -32) {
                                log("RX dialog id=" + data.readShort() + " type=" + data.readByte() + " text=" + data.readUTF());
                            } else log("RX notice " + data.readUTF());
                        } finally { data.reset(); }
                    }
                } catch (Throwable error) { log("CAPTURE error=" + error); }
            }
            return call(method, delegate, args == null ? new Object[0] : args);
        });
    }

    private void guard() throws Throwable {
        if (!(Boolean)call(session.getClass().getMethod("isConnected"), session)
                || gameClass.getField("player").get(null) != player || canvasClass.getField("loadmap").get(null) != map
                || number(map, "idMap") != initialMap
                || ((Number)Class.forName("Thread_More.LoadMap", false, gameClass.getClassLoader()).getField("Area").get(null)).intValue() != initialArea)
            throw new IllegalStateException("Game context changed");
    }

    private void waitForShop() throws Throwable {
        long until = System.currentTimeMillis() + 6500;
        while (!gotShop && System.currentTimeMillis() < until) { guard(); Thread.sleep(100); }
    }

    @Override public void run() {
        try {
            for (Class<?> cls : instrumentation.getAllLoadedClasses()) {
                if (cls.getName().equals("GameScreen.GameScreen") && cls.getField("player").get(null) != null) { gameClass = cls; break; }
            }
            if (gameClass == null) throw new IllegalStateException("No live game");
            ClassLoader loader = gameClass.getClassLoader();
            canvasClass = Class.forName("Main.GameCanvas", false, loader);
            serviceClass = Class.forName("netcommand.global.GlobalService", false, loader);
            service = call(serviceClass.getMethod("gI"), null);
            session = call(Class.forName("CLib.Session_ME", false, loader).getMethod("gI"), null);
            player = gameClass.getField("player").get(null); map = canvasClass.getField("loadmap").get(null);
            initialMap = number(map, "idMap");
            initialArea = ((Number)Class.forName("Thread_More.LoadMap", false, loader).getField("Area").get(null)).intValue();
            guard();
            Class<?> bridge = Class.forName("com.silverknight.HsoBridge", false, loader);
            if ((Boolean)call(bridge.getMethod("isRemoteSellBusy"), null)) throw new IllegalStateException("Sale already active");
            senderBefore = serviceClass.getField("session").get(service);
            receiverBefore = session.getClass().getField("messageHandler").get(session);
            sender = proxy(Class.forName("net.ISession", false, loader), senderBefore, true);
            receiver = proxy(Class.forName("net.IMessageHandler", false, loader), receiverBefore, false);
            serviceClass.getField("session").set(service, sender);
            session.getClass().getField("messageHandler").set(session, receiver);
            log("BEGIN map=" + initialMap + " area=" + initialArea + " local=" + number(player,"x") + "," + number(player,"y") + " coin=" + player.getClass().getField("coin").get(player));
            call(serviceClass.getMethod("remoteSellMenu", int.class), service, -57);
            if (!gotMenu.await(6500, TimeUnit.MILLISECONDS) || option < 0) throw new IllegalStateException("No matching server menu");
            guard(); gotShop = false;
            log("CASE native Dynamic_Menu");
            call(serviceClass.getMethod("Dynamic_Menu", short.class, byte.class, byte.class), service, (short)-57, (byte)menuId, (byte)option);
            waitForShop();
            log("RESULT native shop_received=" + gotShop);
            if (!gotShop) {
                guard();
                log("CASE independent Message via remoteSellShop");
                call(serviceClass.getMethod("remoteSellShop", int.class, int.class, int.class), service, -57, menuId, option);
                waitForShop();
                log("RESULT independent shop_received=" + gotShop);
            }
        } catch (Throwable error) { log("ABORT " + error); }
        finally {
            try {
                if (sender != null && serviceClass.getField("session").get(service) == sender) serviceClass.getField("session").set(service, senderBefore);
                if (receiver != null && session.getClass().getField("messageHandler").get(session) == receiver) session.getClass().getField("messageHandler").set(session, receiverBefore);
                log("END hooks_restored; no movement or transaction packets sent");
            } catch (Throwable error) { log("RESTORE error=" + error); }
        }
    }
}
