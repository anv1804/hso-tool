import java.lang.instrument.Instrumentation;
import java.net.URLClassLoader;
import java.nio.file.Paths;

/** Load fresh live-hook code without redefining any game class. */
public final class HsoGrahamShopEntry {
    public static void agentmain(String args, Instrumentation instrumentation) throws Exception {
        String jar = args.split("\\|", 2)[0];
        URLClassLoader loader = new URLClassLoader(new java.net.URL[]{Paths.get(jar).toUri().toURL()},
                ClassLoader.getSystemClassLoader().getParent());
        Class<?> agent = Class.forName("HsoGrahamShopAgent", true, loader);
        agent.getMethod("agentmain", String.class, Instrumentation.class).invoke(null, args, instrumentation);
    }
}
