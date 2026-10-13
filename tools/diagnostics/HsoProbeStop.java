import java.lang.instrument.Instrumentation;

/** Stop only our diagnostic worker; its finally block restores the game handler/flags. */
public final class HsoProbeStop {
    public static void agentmain(String args, Instrumentation inst) {
        for(Thread thread:Thread.getAllStackTraces().keySet())
            if(thread.getName().equals("HSO bounded remote-shop diagnostic"))thread.interrupt();
    }
}
