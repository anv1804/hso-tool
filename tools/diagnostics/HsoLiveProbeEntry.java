import java.lang.instrument.Instrumentation;
import java.net.URLClassLoader;
import java.net.URL;
import java.nio.file.Paths;

public final class HsoLiveProbeEntry {
    public static void agentmain(String args, Instrumentation inst) throws Exception {
        String[] parts=args.split("\\|",2);
        if(parts.length!=2)throw new IllegalArgumentException("agent.jar|workspace required");
        URL url=Paths.get(parts[0]).toUri().toURL();
        URLClassLoader loader=new URLClassLoader(new URL[]{url},ClassLoader.getSystemClassLoader().getParent());
        Class<?> cls=Class.forName("HsoRemoteSellProbe",true,loader);
        cls.getMethod("agentmain",String.class,Instrumentation.class).invoke(null,parts[1],inst);
    }
}
