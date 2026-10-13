import java.lang.instrument.Instrumentation;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.*;
import java.net.URLClassLoader;
import java.nio.file.Paths;

/** Apply the local Graham-menu display fix to a running client without restarting it. */
public final class HsoGrahamMenuAgent implements InvocationHandler {
    private final Object originalSession;
    private final MethodHandle showMenu;

    private HsoGrahamMenuAgent(Object original, MethodHandle show) {
        originalSession = original;
        showMenu = show;
    }

    private static Object call(Method method, Object target, Object... args) throws Throwable {
        MethodHandle handle = MethodHandles.publicLookup().unreflect(method);
        if (!Modifier.isStatic(method.getModifiers())) handle = handle.bindTo(target);
        return handle.invokeWithArguments(args == null ? new Object[0] : args);
    }

    public static void agentmain(String args, Instrumentation instrumentation) throws Exception {
        try {
            String[] parts = args.split("\\|", 2);
            if (parts.length != 2) throw new IllegalArgumentException("agent.jar|workspace required");
            ClassLoader gameLoader = null;
            for (Class<?> cls : instrumentation.getAllLoadedClasses()) {
                if (cls.getName().equals("GameScreen.GameScreen") && cls.getField("player").get(null) != null) {
                    gameLoader = cls.getClassLoader(); break;
                }
            }
            if (gameLoader == null) throw new IllegalStateException("No active game loader");
            URLClassLoader helperLoader = new URLClassLoader(new java.net.URL[]{
                    Paths.get(parts[1], "patch_bin", "remote_sell_game").toUri().toURL()}, gameLoader);
            Class<?> helper = Class.forName("com.silverknight.GrahamNpcMenu", true, helperLoader);
            MethodHandle show = MethodHandles.publicLookup().unreflect(helper.getMethod("show"));
            Class<?> serviceClass = Class.forName("netcommand.global.GlobalService", false, gameLoader);
            Object service = call(serviceClass.getMethod("gI"), null);
            Field sessionField = serviceClass.getField("session");
            Object session = sessionField.get(service);
            if (Proxy.isProxyClass(session.getClass())
                    && Proxy.getInvocationHandler(session).getClass().getName().equals(HsoGrahamMenuAgent.class.getName())) {
                System.setProperty("hso.grahamMenuPatch", "already installed");
                return;
            }
            Class<?> sessionType = Class.forName("net.ISession", false, gameLoader);
            Object wrapper = Proxy.newProxyInstance(gameLoader, new Class<?>[]{sessionType},
                    new HsoGrahamMenuAgent(session, show));
            sessionField.set(service, wrapper);
            boolean visible = (Boolean)show.invokeWithArguments();
            System.setProperty("hso.grahamMenuPatch", "installed; native menu visible=" + visible);
        } catch (Throwable error) {
            System.setProperty("hso.grahamMenuPatch", "failed: " + error);
            if (error instanceof Exception) throw (Exception)error;
            throw new IllegalStateException(error);
        }
    }

    @Override public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        if (method.getName().equals("sendMessage") && args != null && args.length == 1) {
            Object message = args[0];
            int command = ((Number)message.getClass().getField("command").get(message)).intValue();
            if (command == 23) {
                byte[] payload = (byte[])call(message.getClass().getMethod("getData"), message);
                if (payload.length == 1 && payload[0] == -58) {
                    try {
                        boolean visible = (Boolean)showMenu.invokeWithArguments();
                        System.setProperty("hso.grahamMenuPatch", "button handled; native menu visible=" + visible);
                    } catch (Throwable error) {
                        System.setProperty("hso.grahamMenuPatch", "display failed: " + error);
                    }
                }
            }
        }
        return call(method, originalSession, args);
    }
}
