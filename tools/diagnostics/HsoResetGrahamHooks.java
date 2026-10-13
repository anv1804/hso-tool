import java.lang.instrument.Instrumentation;
import java.lang.reflect.*;

/** Remove only this task's live Graham hooks before loading the next helper build. */
public final class HsoResetGrahamHooks {
    private static Object unwrap(Object value) throws Exception {
        while (value != null && Proxy.isProxyClass(value.getClass())) {
            InvocationHandler handler = Proxy.getInvocationHandler(value);
            String name = handler.getClass().getName();
            String fieldName;
            if (name.equals("HsoGrahamMenuAgent")) fieldName = "originalSession";
            else if (name.equals("HsoGrahamShopAgent")) fieldName = "original";
            else break;
            Field delegate = handler.getClass().getDeclaredField(fieldName);
            delegate.setAccessible(true); value = delegate.get(handler);
        }
        return value;
    }

    public static void agentmain(String args, Instrumentation instrumentation) throws Exception {
        for (Class<?> cls : instrumentation.getAllLoadedClasses()) {
            if (cls.getName().equals("netcommand.global.GlobalService") || cls.getName().equals("CLib.Session_ME")) {
                Field instance = cls.getDeclaredField("instance"); instance.setAccessible(true);
                Object owner = instance.get(null);
                if (owner == null) continue;
                Field field = cls.getField(cls.getName().equals("CLib.Session_ME") ? "messageHandler" : "session");
                field.set(owner, unwrap(field.get(owner)));
            }
        }
        System.setProperty("hso.grahamHooksReset", "complete; socket and game state preserved");
    }
}
