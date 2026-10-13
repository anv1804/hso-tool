import java.io.*;
import java.lang.instrument.*;
import java.nio.file.*;

public class RedefineAgent {
    public static void agentmain(String args, Instrumentation inst) {
        try {
            Class<?> bridge = null, readMsg = null;
            for (Class<?> c : inst.getAllLoadedClasses()) {
                if (c.getName().equals("com.silverknight.HsoBridge")) bridge = c;
                if (c.getName().equals("netcommand.global.ReadMessenge")) readMsg = c;
            }
            if (bridge == null || readMsg == null) {
                System.out.println("[REDEFINE] Target classes not found");
                return;
            }
            byte[] bridgeBytes = Files.readAllBytes(Paths.get("E:/hso/bin/com/silverknight/HsoBridge.class"));
            byte[] readMsgBytes = Files.readAllBytes(Paths.get("E:/hso/bin/netcommand/global/ReadMessenge.class"));
            ClassDefinition[] defs = new ClassDefinition[] {
                new ClassDefinition(bridge, bridgeBytes),
                new ClassDefinition(readMsg, readMsgBytes)
            };
            inst.redefineClasses(defs);
            System.out.println("[REDEFINE] HOT RELOAD SUCCESSFUL!");
            Files.write(Paths.get("E:/hso/hso_remote_sell_probe.log"),
                    ("[HOT_RELOAD] SUCCESS at " + new java.util.Date() + "\n").getBytes("UTF-8"),
                    StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        } catch (Throwable t) {
            String msg = "[REDEFINE] FAILED: " + t + "\n";
            System.out.print(msg);
            try {
                Files.write(Paths.get("E:/hso/hso_remote_sell_probe.log"),
                        msg.getBytes("UTF-8"),
                        StandardOpenOption.CREATE, StandardOpenOption.APPEND);
            } catch (Exception ignored) {}
        }
    }
}
