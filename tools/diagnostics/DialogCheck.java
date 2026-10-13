import java.io.*;
import java.lang.instrument.Instrumentation;
import java.lang.reflect.*;

public class DialogCheck {
    public static void agentmain(String args, Instrumentation inst) {
        try {
            Class<?> canvas = null;
            for (Class<?> c : inst.getAllLoadedClasses()) {
                if (c.getName().equals("Main.GameCanvas")) { canvas = c; break; }
            }
            if (canvas == null) return;
            ClassLoader cl = canvas.getClassLoader();
            Object currentDialog = canvas.getField("currentDialog").get(null);
            if (currentDialog != null) {
                StringBuilder sb = new StringBuilder();
                sb.append("[DIALOG_CHECK] Class=").append(currentDialog.getClass().getName()).append("\n");
                for (Field f : currentDialog.getClass().getFields()) {
                    try {
                        f.setAccessible(true);
                        Object val = f.get(currentDialog);
                        if (val instanceof String || val instanceof String[] || val instanceof Number) {
                            sb.append("  ").append(f.getName()).append(" = ").append(val).append("\n");
                        }
                    } catch (Exception ignored) {}
                }
                for (Field f : currentDialog.getClass().getDeclaredFields()) {
                    try {
                        f.setAccessible(true);
                        Object val = f.get(currentDialog);
                        if (val instanceof String || val instanceof String[] || val instanceof Number) {
                            sb.append("  (decl) ").append(f.getName()).append(" = ").append(val).append("\n");
                        }
                    } catch (Exception ignored) {}
                }
                String s = sb.toString();
                System.out.print(s);
                java.nio.file.Files.write(java.nio.file.Paths.get("E:/hso/hso_remote_sell_probe.log"),
                        s.getBytes("UTF-8"),
                        java.nio.file.StandardOpenOption.CREATE, java.nio.file.StandardOpenOption.APPEND);

                // Check left/center/right command on dialog
                Object center = null, left = null, right = null;
                try { center = currentDialog.getClass().getField("center").get(currentDialog); } catch (Exception ignored) {}
                try { left = currentDialog.getClass().getField("left").get(currentDialog); } catch (Exception ignored) {}
                try { right = currentDialog.getClass().getField("right").get(currentDialog); } catch (Exception ignored) {}
                System.out.println("  center=" + center + " left=" + left + " right=" + right);
                if (center != null) {
                    System.out.println("  executing center.perform()...");
                    center.getClass().getMethod("perform").invoke(center);
                } else if (left != null) {
                    System.out.println("  executing left.perform()...");
                    left.getClass().getMethod("perform").invoke(left);
                }
            }
        } catch (Throwable t) {
            t.printStackTrace();
        }
    }
}
