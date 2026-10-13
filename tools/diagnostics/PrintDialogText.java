import java.io.*;
import java.lang.instrument.Instrumentation;
import java.lang.reflect.*;

public class PrintDialogText {
    public static void agentmain(String args, Instrumentation inst) {
        try {
            Class<?> canvas = null;
            for (Class<?> c : inst.getAllLoadedClasses()) {
                if (c.getName().equals("Main.GameCanvas")) { canvas = c; break; }
            }
            if (canvas == null) return;
            ClassLoader cl = canvas.getClassLoader();
            Object currentDialog = canvas.getField("currentDialog").get(null);
            if (currentDialog == null) {
                System.out.println("[DIALOG] No active dialog!");
                return;
            }
            Field fStatus = null;
            try {
                fStatus = currentDialog.getClass().getDeclaredField("status");
                fStatus.setAccessible(true);
                System.out.println("[DIALOG] status = '" + fStatus.get(currentDialog) + "'");
            } catch (Exception ignored) {}

            Field fStr = null;
            try {
                Class<?> md = currentDialog.getClass().getSuperclass();
                fStr = md.getDeclaredField("strinfo");
                fStr.setAccessible(true);
                String[] arr = (String[])fStr.get(currentDialog);
                if (arr != null) {
                    for (int i = 0; i < arr.length; i++) {
                        System.out.println("[DIALOG] strinfo[" + i + "] = '" + arr[i] + "'");
                    }
                }
            } catch (Exception ignored) {}

            // Check if there are buttons
            Object cmdCenter = null, cmdLeft = null, cmdRight = null;
            try { cmdCenter = currentDialog.getClass().getField("center").get(currentDialog); } catch (Exception ignored) {}
            try { cmdLeft = currentDialog.getClass().getField("left").get(currentDialog); } catch (Exception ignored) {}
            try { cmdRight = currentDialog.getClass().getField("right").get(currentDialog); } catch (Exception ignored) {}
            System.out.println("[DIALOG] center=" + cmdCenter + " left=" + cmdLeft + " right=" + cmdRight);
        } catch (Throwable t) {
            t.printStackTrace();
        }
    }
}
