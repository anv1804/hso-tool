import java.lang.instrument.Instrumentation;
import java.net.URLClassLoader;
import java.nio.file.Paths;

public final class HsoDylanContextEntry {
    public static void agentmain(String args, Instrumentation inst) throws Exception {
        String jar=args.split("\\|",2)[0];
        URLClassLoader loader=new URLClassLoader(new java.net.URL[]{Paths.get(jar).toUri().toURL()},ClassLoader.getSystemClassLoader().getParent());
        Class<?> audit=Class.forName("HsoDylanContextAudit",true,loader);
        audit.getMethod("agentmain",String.class,Instrumentation.class).invoke(null,args,inst);
    }
}
