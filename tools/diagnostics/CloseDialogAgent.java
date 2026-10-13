import java.io.*;
import java.lang.instrument.Instrumentation;
import java.lang.reflect.*;

public class CloseDialogAgent {
    public static void agentmain(String args, Instrumentation inst) {
        try {
            Class<?> canvas = null;
            for (Class<?> c : inst.getAllLoadedClasses()) {
                if (c.getName().equals("Main.GameCanvas")) { canvas = c; break; }
            }
            if (canvas == null) return;
            System.out.println("[CLOSE_DIALOG] Calling GameCanvas.end_Dialog()...");
            canvas.getMethod("end_Dialog").invoke(null);
            System.out.println("[CLOSE_DIALOG] Dialog closed!");
        } catch (Throwable t) {
            t.printStackTrace();
        }
    }
}
