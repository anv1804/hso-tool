import java.io.*;
import java.lang.instrument.Instrumentation;
import java.lang.reflect.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;

public final class HsoVibeRealRun2 implements Runnable {
    private final Instrumentation inst;
    private final Path root;

    public HsoVibeRealRun2(Instrumentation inst, String args) {
        this.inst = inst;
        String[] parts = args.split("\\|", 3);
        this.root = Paths.get(parts[1]);
    }

    public static void agentmain(String args, Instrumentation inst) {
        Thread t = new Thread(new HsoVibeRealRun2(inst, args), "HSO Vibe Real Run 2");
        t.setDaemon(true);
        t.start();
    }

    private void log(String s) {
        String line = new java.text.SimpleDateFormat("HH:mm:ss.SSS").format(new java.util.Date())
                + " [VIBE_RUN2] " + s + "\n";
        try {
            Files.write(root.resolve("hso_remote_sell_probe.log"),
                    line.getBytes(StandardCharsets.UTF_8),
                    StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        } catch (IOException ignored) {}
    }

    @Override
    public void run() {
        try {
            log("STEP 1: find game class");
            Class<?> gameClass = null;
            for (Class<?> c : inst.getAllLoadedClasses()) {
                if (c.getName().equals("GameScreen.GameScreen") && c.getField("player").get(null) != null) {
                    gameClass = c;
                    break;
                }
            }
            if (gameClass == null) {
                log("gameClass not found");
                return;
            }
            log("STEP 2: classloader lookup");
            ClassLoader loader = gameClass.getClassLoader();
            Class<?> sessionClass = Class.forName("CLib.Session_ME", false, loader);
            Object session = sessionClass.getMethod("gI").invoke(null);
            log("STEP 3: session=" + session);

            Class<?> itemClass = Class.forName("GameObjects.Item", false, loader);
            Object vec = itemClass.getField("VecInvetoryPlayer").get(null);
            int size = (Integer)vec.getClass().getMethod("size").invoke(vec);
            log("STEP 4: inventory size=" + size);

            int targetId = -1;
            int targetCat = -1;
            String targetName = "";
            for (int i = 0; i < size; i++) {
                Object it = vec.getClass().getMethod("elementAt", int.class).invoke(vec, i);
                if (it == null) continue;
                int id = ((Number)it.getClass().getField("Id").get(it)).intValue();
                int cat = ((Number)it.getClass().getField("ItemCatagory").get(it)).intValue();
                int lock = ((Number)it.getClass().getField("isLock").get(it)).intValue();
                int tier = ((Number)it.getClass().getField("tier").get(it)).intValue();
                String name = (String)it.getClass().getField("itemName").get(it);
                if (cat == 3 && lock == 0 && tier == 0) {
                    targetId = id;
                    targetCat = cat;
                    targetName = name;
                    break;
                }
            }
            log("STEP 5: targetItem=" + targetName + " id=" + targetId + " cat=" + targetCat);

            // Check GlobalService
            Class<?> gsClass = Class.forName("netcommand.global.GlobalService", false, loader);
            Object gs = gsClass.getMethod("gI").invoke(null);
            log("STEP 6: GlobalService=" + gs);

            // Execute spoof move to Lisa (756, 120)
            log("STEP 7: GlobalService.player_move(756, 120)");
            gsClass.getMethod("player_move", short.class, short.class).invoke(gs, (short)756, (short)120);
            Thread.sleep(50);

            // Execute getlist_from_npc(-3)
            log("STEP 8: GlobalService.getlist_from_npc(-3)");
            gsClass.getMethod("getlist_from_npc", byte.class).invoke(gs, (byte)-3);
            Thread.sleep(300);

            // Execute delete_Item(cat, id, 1)
            log("STEP 9: GlobalService.delete_Item(" + targetCat + ", " + targetId + ", 1)");
            gsClass.getMethod("delete_Item", byte.class, short.class, byte.class)
                    .invoke(gs, (byte)targetCat, (short)targetId, (byte)1);

            log("STEP 10: wait 3 seconds...");
            Thread.sleep(3000);

            // Restore position to 716, 354
            log("STEP 11: GlobalService.player_move(716, 354)");
            gsClass.getMethod("player_move", short.class, short.class).invoke(gs, (short)716, (short)354);
            log("STEP 12: completed!");

        } catch (Throwable t) {
            StringWriter sw = new StringWriter();
            t.printStackTrace(new PrintWriter(sw));
            log("ERROR: " + sw.toString());
        }
    }
}
