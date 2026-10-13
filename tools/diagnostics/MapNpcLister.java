import java.io.*;
import java.lang.instrument.Instrumentation;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.Vector;

public class MapNpcLister {
    public static void agentmain(String args, Instrumentation inst) {
        StringBuilder sb = new StringBuilder();
        try {
            Class<?> gs = null;
            for (Class<?> c : inst.getAllLoadedClasses()) {
                if (c.getName().equals("GameScreen.GameScreen")) {
                    try {
                        if (c.getField("player").get(null) != null) {
                            gs = c; break;
                        }
                    } catch (Throwable ignored) {}
                }
            }
            if (gs == null) {
                sb.append("ERROR: GameScreen not found or player is null\n");
            } else {
                ClassLoader loader = gs.getClassLoader();
                Class<?> canvas = Class.forName("Main.GameCanvas", false, loader);
                Object map = canvas.getField("loadmap").get(null);
                int mapId = ((Number)map.getClass().getField("idMap").get(map)).intValue();
                Object pl = gs.getField("player").get(null);
                int px = ((Number)pl.getClass().getField("x").get(pl)).intValue();
                int py = ((Number)pl.getClass().getField("y").get(pl)).intValue();
                String pname = (String)pl.getClass().getField("name").get(pl);
                sb.append("PLAYER: ").append(pname).append(" map=").append(mapId).append(" pos=(").append(px).append(",").append(py).append(")\n");

                Object vec = gs.getField("Vecplayers").get(null);
                if (vec == null) {
                    sb.append("Vecplayers is null\n");
                } else {
                    Vector<?> v = (Vector<?>) vec;
                    sb.append("Total objects in Vecplayers: ").append(v.size()).append("\n");
                    for (int i = 0; i < v.size(); i++) {
                        Object obj = v.get(i);
                        if (obj == null) continue;
                        Class<?> cls = obj.getClass();
                        String cname = cls.getName();
                        try {
                            int id = ((Number) cls.getField("ID").get(obj)).intValue();
                            int x = ((Number) cls.getField("x").get(obj)).intValue();
                            int y = ((Number) cls.getField("y").get(obj)).intValue();
                            String name = (String) cls.getField("name").get(obj);
                            sb.append("[").append(i).append("] class=").append(cname)
                              .append(" name='").append(name).append("' id=").append(id)
                              .append(" pos=(").append(x).append(",").append(y).append(")\n");
                        } catch (Throwable t) {
                            sb.append("[").append(i).append("] class=").append(cname).append(" (error reading fields)\n");
                        }
                    }
                }
            }
        } catch (Throwable t) {
            StringWriter sw = new StringWriter();
            t.printStackTrace(new PrintWriter(sw));
            sb.append("EXCEPTION: ").append(sw.toString()).append("\n");
        }
        try {
            Files.write(Paths.get("E:/hso/npc_list.txt"),
                    sb.toString().getBytes(StandardCharsets.UTF_8),
                    StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
        } catch (IOException ignored) {}
    }
}
