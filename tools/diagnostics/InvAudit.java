import java.io.*;
import java.lang.instrument.Instrumentation;
import java.lang.invoke.*;
import java.lang.reflect.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;

public class InvAudit implements Runnable {
    private final Instrumentation inst;
    private final Path root;

    public InvAudit(Instrumentation inst, String args) {
        this.inst = inst;
        String[] parts = args.split("\\|", 3);
        root = Paths.get(parts[1]);
    }

    public static void agentmain(String args, Instrumentation inst) {
        new Thread(new InvAudit(inst, args)).start();
    }

    @Override
    public void run() {
        try {
            Class<?> gs = null;
            for (Class<?> c : inst.getAllLoadedClasses()) {
                if (c.getName().equals("GameScreen.GameScreen") && c.getField("player").get(null) != null) {
                    gs = c; break;
                }
            }
            if (gs == null) return;
            ClassLoader loader = gs.getClassLoader();
            Class<?> itemClass = Class.forName("GameObjects.Item", false, loader);
            Object vec = itemClass.getField("VecInvetoryPlayer").get(null);
            int size = (Integer) vec.getClass().getMethod("size").invoke(vec);

            StringBuilder sb = new StringBuilder();
            sb.append("=== INVENTORY AUDIT (").append(size).append(" items) ===\n");
            for (int i = 0; i < size; i++) {
                Object it = vec.getClass().getMethod("elementAt", int.class).invoke(vec, i);
                if (it == null) continue;
                int id = ((Number) it.getClass().getField("Id").get(it)).intValue();
                int cat = ((Number) it.getClass().getField("ItemCatagory").get(it)).intValue();
                int lock = ((Number) it.getClass().getField("isLock").get(it)).intValue();
                int tier = ((Number) it.getClass().getField("tier").get(it)).intValue();
                int color = ((Number) it.getClass().getField("colorNameItem").get(it)).intValue();
                String name = (String) it.getClass().getField("itemName").get(it);
                sb.append("[").append(i).append("] id=").append(id)
                  .append(" cat=").append(cat)
                  .append(" color=").append(color)
                  .append(" lock=").append(lock)
                  .append(" tier=").append(tier)
                  .append(" name='").append(name).append("'\n");
            }
            Files.write(root.resolve("current_inventory.txt"),
                    sb.toString().getBytes(StandardCharsets.UTF_8),
                    StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
        } catch (Throwable t) {
            t.printStackTrace();
        }
    }
}
