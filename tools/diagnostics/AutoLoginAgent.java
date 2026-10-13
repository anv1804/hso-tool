import java.io.*;
import java.lang.instrument.Instrumentation;
import java.lang.reflect.*;

public class AutoLoginAgent {
    public static void agentmain(String args, Instrumentation inst) {
        try {
            Class<?> canvas = null;
            for (Class<?> c : inst.getAllLoadedClasses()) {
                if (c.getName().equals("Main.GameCanvas")) { canvas = c; break; }
            }
            if (canvas == null) return;
            ClassLoader cl = canvas.getClassLoader();
            Object currentScreen = canvas.getField("currentScreen").get(null);
            Object login = canvas.getField("login").get(null);
            Object selectChar = canvas.getField("selectChar").get(null);
            Object game = canvas.getField("game").get(null);
            System.out.println("[SCREEN] currentScreen=" + currentScreen);

            if (currentScreen == selectChar && selectChar != null) {
                System.out.println("[AUTO_LOGIN] Calling selectChar.autoSelect()...");
                selectChar.getClass().getMethod("autoSelect").invoke(selectChar);
            } else if (currentScreen == login && login != null) {
                System.out.println("[AUTO_LOGIN] At login screen, pressing center/login...");
                login.getClass().getMethod("doLogin").invoke(login);
            } else if (currentScreen == game) {
                System.out.println("[AUTO_LOGIN] Already in game!");
            }
        } catch (Throwable t) {
            t.printStackTrace();
        }
    }
}
