import java.io.*;
import java.lang.instrument.Instrumentation;
import java.lang.invoke.*;
import java.lang.reflect.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.concurrent.*;

/**
 * Diagnostic probe for Direction 1:
 * Opens Mr Dylan's genuine server shop at Map 19 (Opcode 23 -> -30 -> 23),
 * and while Dylan's shop session is active, sends Opcode 18 (delete_Item, typedelete=1)
 * to test if the server permits selling an inventory item during an in-map shop session.
 */
public final class HsoDylanShopSellProbe implements Runnable {
    private final Instrumentation inst;
    private final Path root;
    private final boolean auditOnly;
    private final String expectedName;
    private Class<?> game, canvas, messageClass;
    private Object player, map, session, originalHandler, proxyHandler;
    private int area, x, y, menuId = -1, option = -1;
    private long coinBefore;
    private final CountDownLatch menuLatch = new CountDownLatch(1);
    private final CountDownLatch shopLatch = new CountDownLatch(1);
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

    private HsoDylanShopSellProbe(Instrumentation inst, String args) {
        this.inst = inst;
        String[] parts = args.split("\\|", 4);
        root = Paths.get(parts[1]);
        expectedName = parts.length >= 3 && parts[2].length() > 0 ? parts[2] : "lronic";
        auditOnly = parts.length >= 4 && "audit".equalsIgnoreCase(parts[3]);
    }

    public static synchronized void agentmain(String args, Instrumentation inst) {
        String key = "hso.dylanShopSell.running";
        if (System.getProperty(key) != null) {
            throw new IllegalStateException("Dylan shop sell probe is already running");
        }
        System.setProperty(key, "true");
        Thread t = new Thread(new HsoDylanShopSellProbe(inst, args), "HSO Dylan shop sell probe");
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
                + " DYLAN_SHOP_SELL " + s + "\n";
        try {
            Files.write(root.resolve("hso_remote_sell_probe.log"),
                    line.getBytes(StandardCharsets.UTF_8),
                    StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        } catch (IOException ignored) {}
        System.setProperty("hso.dylanShopSell.status", s);
    }

    private void guard() throws Throwable {
        if (!(Boolean)call(session.getClass().getMethod("isConnected"), session)
                || game.getField("player").get(null) != player
                || canvas.getField("loadmap").get(null) != map
                || num(map, "idMap") != 19) {
            throw new IllegalStateException("Session disconnected or player left Map 19");
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
                        if (npc == -57 && count <= 32) {
                            StringBuilder labels = new StringBuilder();
                            for (int i = 0; i < count; i++) {
                                String label = in.readUTF();
                                labels.append('[').append(i).append(':').append(label).append(']');
                                if ("Mua bán".equalsIgnoreCase(label)) option = i;
                            }
                            menuId = id;
                            gotMenu = option >= 0;
                            log("RX menu npc=-57 menu=" + menuId + " option=" + option + " labels=" + labels);
                            menuLatch.countDown();
                        }
                    } else if (cmd == 23) {
                        String title = in.readUTF();
                        int type = in.readByte();
                        if (type == -57) {
                            log("RX Dylan chat: " + title);
                        } else if (type >= 0) {
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
        Object chosen = null;

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

            // Select an unlocked item with tier 0.
            // Prefer equipment (cat 3) or potions/materials (cat 4, 7) that are safe.
            if (chosen == null && lock == 0 && tier == 0 && canSell == 1) {
                // Do not sell items with high tier or locked
                if (cat == 3 || cat == 4 || cat == 7) {
                    chosen = it;
                    targetId = id;
                    targetCat = cat;
                    targetName = name;
                    targetQtyBefore = qty;
                    targetTier = tier;
                    targetLock = lock;
                }
            }
        }

        if (chosen != null) {
            log("TARGET_SELECTED: id=" + targetId + " cat=" + targetCat + " name='" + targetName
                    + "' qty=" + targetQtyBefore);
            return true;
        }
        return false;
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
            x = num(player, "x");
            y = num(player, "y");
            coinBefore = coins();

            log("INIT name=" + field(player, "name") + " map=" + num(map, "idMap")
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

            if (num(map, "idMap") != 19) {
                throw new IllegalStateException("Current map is " + num(map, "idMap") + ", expected Map 19 (Cửa Biển)");
            }
            if (!expectedName.equals(field(player, "name"))) {
                throw new IllegalStateException("Player name is '" + field(player, "name") + "', expected '" + expectedName + "'");
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

            log("STEP 1: Request Dylan Menu (Opcode 23, NPC -57)");
            sendPacket(23, new byte[]{(byte)-57});
            if (!menuLatch.await(5000, TimeUnit.MILLISECONDS) || !gotMenu) {
                throw new IllegalStateException("Dylan dynamic menu not received within 5s");
            }

            log("STEP 2: Select 'Mua bán' (Opcode -30, menu=" + menuId + ", option=" + option + ")");
            ByteArrayOutputStream selectBytes = new ByteArrayOutputStream();
            DataOutputStream selectOut = new DataOutputStream(selectBytes);
            selectOut.writeShort(-57);
            selectOut.writeByte(menuId);
            selectOut.writeByte(option);
            sendPacket(-30, selectBytes.toByteArray());

            if (!shopLatch.await(5000, TimeUnit.MILLISECONDS) || !gotShop) {
                throw new IllegalStateException("Dylan server shop (Opcode 23) not received within 5s");
            }

            log("STEP 3: SHOP IS ACTIVE! Dylan shop confirmed: title='" + shopTitle + "'");
            log("STEP 4: Testing Opcode 18 (DEL_ITEM) within active Dylan shop session...");
            Thread.sleep(200); // Brief pause for server state alignment

            // Send Opcode 18: [byte cat, short id, byte 1]
            ByteArrayOutputStream sellBytes = new ByteArrayOutputStream();
            DataOutputStream sellOut = new DataOutputStream(sellBytes);
            sellOut.writeByte((byte)targetCat);
            sellOut.writeShort((short)targetId);
            sellOut.writeByte(1); // typedelete = 1 (Sell for gold)
            sellSent = true;
            sendPacket(18, sellBytes.toByteArray());
            log("TX DEL_ITEM: Sold item '" + targetName + "' id=" + targetId + " cat=" + targetCat + " typedelete=1");

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
            System.clearProperty("hso.dylanShopSell.running");
        }
    }
}
