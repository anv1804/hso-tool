import java.lang.instrument.Instrumentation;
import java.net.URLClassLoader;
import java.nio.file.Paths;

/** Load each diagnostic build separately from earlier attached versions. */
public final class HsoDylanReplayEntry {
    public static void agentmain(String args, Instrumentation inst) throws Exception {
        String jar = args.split("\\|", 2)[0];
        URLClassLoader loader = new URLClassLoader(new java.net.URL[]{Paths.get(jar).toUri().toURL()},
                ClassLoader.getSystemClassLoader().getParent());
        Class<?> replay = Class.forName("HsoDylanPurchaseReplay", true, loader);
        replay.getMethod("agentmain", String.class, Instrumentation.class).invoke(null, args, inst);
    }
}
