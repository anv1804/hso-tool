import java.io.*;
import java.lang.instrument.Instrumentation;
import java.lang.invoke.*;
import java.lang.reflect.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.concurrent.*;

/**
 * Diagnostic Probe to test Pet Shop (NPC ID: -42) on Map 3:
 * 1. Request menu from NPC -42.
 * 2. Select Option 1 (Shop thức ăn) or Option 2 (Shop trứng).
 * 3. Once Server confirms shop open (Opcode 23), send Opcode 18 to sell Áo vải xanh (id=28, cat=3, typedelete=1).
 * 4. Await Opcode -32 or Opcode 16 or Opcode 37/53.
 */
public final class HsoPetShopProbe implements Runnable {
    private final Instrumentation inst;
    private final Path root;
    private Class<?> game, canvas, messageClass;
    private Object player, map, session, originalHandler, proxyHandler;
    private int currentMapId, area, x, y;
    private long coinBefore;

    private CountDownLatch menuLatch = new CountDownLatch(1);
    private CountDownLatch shopLatch = new CountDownLatch(1);
    private CountDownLatch resultLatch = new CountDownLatch(1);

    private volatile boolean gotMenu = false;
    private volatile int lastMenuId = -1;
    private volatile int targetOption = -1;
    private volatile boolean gotShop = false;
    private volatile String shopTitle = "";
    private volatile int lastDialogId = Integer.MIN_VALUE;
    private volatile int lastDialogType = -1;
    private volatile String lastDialogText = "";
    private volatile boolean saleSuccess = false;

    // Target item
    private int targetId = 28;
    private int targetCat = 3;
    private String targetName = "Áo vải xanh";

    private HsoPetShopProbe(Instrumentation inst, String args) {
        this.inst = inst;
        String[] parts = args.split("\\|", 3);
        root = Paths.get(parts[1]);
    }

    public static synchronized void agentmain(String args, Instrumentation inst) {
        String key = "hso.petShopProbe.running";
        if (System.getProperty(key) != null) {
            throw new IllegalStateException("Pet Shop Probe already running");
        }
        System.setProperty(key, "true");
        Thread t = new Thread(new HsoPetShopProbe(inst, args), "HSO Pet Shop Probe");
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
                + " PET_SHOP_PROBE " + s + "\n";
        try {
            Files.write(root.resolve("hso_remote_sell_probe.log"),
                    line.getBytes(StandardCharsets.UTF_8),
                    StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        } catch (IOException ignored) {}
        System.setProperty("hso.petShopProbe.status", s);
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
            if (cmd == -30 || cmd == 23 || cmd == 16 || cmd == -32 || cmd == 37 || cmd == 53) {
                DataInputStream in = (DataInputStream)call(messageClass.getMethod("reader"), msg);
                in.mark(in.available() + 1);
                try {
                    if (cmd == -30) {
                        int npc = in.readShort();
                        int id = in.readByte();
                        int count = in.readUnsignedByte();
                        StringBuilder labels = new StringBuilder();
                        int shopOpt = -1;
                        for (int i = 0; i < count; i++) {
                            String label = in.readUTF();
                            labels.append('[').append(i).append(':').append(label).append(']');
                            String lower = label.toLowerCase();
                            if (lower.contains("shop thức ăn") || lower.contains("thức ăn") || lower.contains("shop")) {
                                if (shopOpt < 0) shopOpt = i;
                            }
                        }
                        lastMenuId = id;
                        targetOption = shopOpt >= 0 ? shopOpt : 1;
                        gotMenu = true;
                        log("RX menu npc=" + npc + " id=" + id + " options=" + labels + " -> selectedOption=" + targetOption);
                        menuLatch.countDown();
                    } else if (cmd == 23) {
                        String title = in.readUTF();
                        int type = in.readByte();
                        if (type >= 0) {
                            shopTitle = title;
                            gotShop = true;
                            log("RX shop cmd=23 type=" + type + " title='" + title + "'");
                            shopLatch.countDown();
                        } else {
                            log("RX chat NPC " + type + ": '" + title + "'");
                        }
                    } else if (cmd == 16) {
                        long currentCoins = coins();
                        log("RX inventory cmd=16 coin=" + currentCoins);
                        if (currentCoins > coinBefore) {
                            saleSuccess = true;
                            log("SALE_CONFIRMED! Coin increased: " + coinBefore + " -> " + currentCoins
                                    + " (+" + (currentCoins - coinBefore) + " Vàng)");
                        }
                        resultLatch.countDown();
                    } else if (cmd == -32) {
                        int id = in.readShort();
                        int type = in.readByte();
                        String text = in.readUTF();
                        lastDialogId = id;
                        lastDialogType = type;
                        lastDialogText = text;
                        log("RX dialog cmd=-32 id=" + id + " type=" + type + " text='" + text + "'");
                        resultLatch.countDown();
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

            // Install message capture proxy
            originalHandler = field(session, "messageHandler");
            proxyHandler = Proxy.newProxyInstance(loader,
                    new Class<?>[]{Class.forName("net.IMessageHandler", false, loader)},
                    this::receive);
            session.getClass().getField("messageHandler").set(session, proxyHandler);

            // STEP 1: Request menu from Pet Shop (-42)
            log("STEP 1: Requesting menu from NPC -42 (Pet Shop)...");
            sendPacket(23, new byte[]{(byte)-42});

            if (!menuLatch.await(4000, TimeUnit.MILLISECONDS) || !gotMenu) {
                log("Pet Shop (-42) did not return menu within 4.0s");
                return;
            }

            // STEP 2: Select option (Shop thức ăn)
            log("STEP 2: Selecting option " + targetOption + " from menuId=" + lastMenuId);
            ByteArrayOutputStream sel = new ByteArrayOutputStream();
            DataOutputStream selOut = new DataOutputStream(sel);
            selOut.writeShort(-42);
            selOut.writeByte(lastMenuId);
            selOut.writeByte(targetOption);
            sendPacket(-30, sel.toByteArray());

            if (!shopLatch.await(4000, TimeUnit.MILLISECONDS) || !gotShop) {
                log("Pet Shop option " + targetOption + " did not return shop within 4.0s");
                return;
            }

            log("STEP 3: SERVER SHOP CONFIRMED OPEN: '" + shopTitle + "'! Testing Opcode 18 sale...");
            Thread.sleep(200);

            // STEP 4: Send Opcode 18 sale for Áo vải xanh
            ByteArrayOutputStream sellBytes = new ByteArrayOutputStream();
            DataOutputStream sellOut = new DataOutputStream(sellBytes);
            sellOut.writeByte((byte)targetCat);
            sellOut.writeShort((short)targetId);
            sellOut.writeByte(1); // typedelete = 1
            sendPacket(18, sellBytes.toByteArray());
            log("TX DEL_ITEM: Sent sell request for " + targetName + " id=" + targetId + " cat=" + targetCat);

            // STEP 5: Await Dialog -32 or inventory update
            boolean rx = resultLatch.await(4000, TimeUnit.MILLISECONDS);
            if (rx && lastDialogId != Integer.MIN_VALUE) {
                log("CONFIRMATION DIALOG RECEIVED! id=" + lastDialogId + " type=" + lastDialogType + " text='" + lastDialogText + "'");
                Thread.sleep(150);
                ByteArrayOutputStream confirmBytes = new ByteArrayOutputStream();
                DataOutputStream confirmOut = new DataOutputStream(confirmBytes);
                confirmOut.writeShort((short)lastDialogId);
                confirmOut.writeByte((byte)lastDialogType);
                confirmOut.writeByte(1);
                sendPacket(-32, confirmBytes.toByteArray());
                log("TX DIALOG_CONFIRM sent!");

                // Wait for coin update
                for (int i = 0; i < 30; i++) {
                    Thread.sleep(100);
                    if (saleSuccess) break;
                }
            } else if (!rx) {
                log("No dialog or inventory response within 4.0s of Opcode 18 in Pet Shop!");
            }

            long coinAfter = coins();
            log("SUMMARY: coin=" + coinBefore + " -> " + coinAfter + " (diff=" + (coinAfter - coinBefore) + ")"
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
            System.clearProperty("hso.petShopProbe.running");
        }
    }
}
