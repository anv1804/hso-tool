import java.io.*;
import java.lang.instrument.Instrumentation;
import java.lang.reflect.*;

public class CheckState {
    public static void agentmain(String args, Instrumentation inst) {
        try {
            Class<?> canvas = null;
            for (Class<?> c : inst.getAllLoadedClasses()) {
                if (c.getName().equals("Main.GameCanvas")) { canvas = c; break; }
            }
            if (canvas == null) return;
            ClassLoader cl = canvas.getClassLoader();
            Object currentScreen = canvas.getField("currentScreen").get(null);
            Object currentDialog = canvas.getField("currentDialog").get(null);
            Object subDialog = canvas.getField("subDialog").get(null);
            Object selectChar = canvas.getField("selectChar").get(null);

            Class<?> scClass = Class.forName("GameScreen.SelectCharScreen", false, cl);
            Object vec = scClass.getField("VecSelectChar").get(null);
            int vecSize = vec != null ? (Integer)vec.getClass().getMethod("size").invoke(vec) : -1;
            boolean isSelectOk = (Boolean)scClass.getField("isSelectOk").get(null);

            Class<?> sessionClass = Class.forName("CLib.Session_ME", false, cl);
            Object session = sessionClass.getMethod("gI").invoke(null);
            boolean connected = (Boolean)sessionClass.getMethod("isConnected").invoke(session);

            String info = "[STATE] Screen=" + currentScreen + " Dialog=" + currentDialog
                    + " SubDialog=" + subDialog + " VecSize=" + vecSize + " isSelectOk=" + isSelectOk
                    + " Connected=" + connected + "\n";
            System.out.print(info);
            java.nio.file.Files.write(java.nio.file.Paths.get("E:/hso/hso_remote_sell_probe.log"),
                    info.getBytes("UTF-8"),
                    java.nio.file.StandardOpenOption.CREATE, java.nio.file.StandardOpenOption.APPEND);

            // If there's an ok dialog or msg dialog, dismiss or perform
            if (currentDialog != null) {
                System.out.println("[STATE] Dialog active: " + currentDialog);
            }
            // If at selectChar, call doSelect()
            if (currentScreen == selectChar && vecSize > 0) {
                System.out.println("[STATE] Calling doSelect()...");
                scClass.getMethod("doSelect").invoke(selectChar);
            }
        } catch (Throwable t) {
            t.printStackTrace();
        }
    }
}
