import java.lang.instrument.Instrumentation;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.*;

/** Retain the original server shop from the earlier live hook before it is unloaded. */
public final class HsoRecoverGrahamCatalog {
    public static void agentmain(String args, Instrumentation instrumentation) throws Exception {
        try {
            Class<?> canvas = null, game = null, serviceClass = null;
            for (Class<?> cls : instrumentation.getAllLoadedClasses()) {
                if (cls.getName().equals("Main.GameCanvas")) canvas = cls;
                if (cls.getName().equals("GameScreen.GameScreen") && cls.getField("player").get(null) != null) game = cls;
                if (cls.getName().equals("netcommand.global.GlobalService")) serviceClass = cls;
            }
            Field instance = serviceClass.getDeclaredField("instance"); instance.setAccessible(true);
            Object service = instance.get(null), player = game.getField("player").get(null);
            int ownerId = player.getClass().getField("ID").getInt(player);
            Object proxy = serviceClass.getField("session").get(service);
            InvocationHandler handler = Proxy.getInvocationHandler(proxy);
            Field helperField = handler.getClass().getDeclaredField("helper"); helperField.setAccessible(true);
            Class<?> currentHelper = (Class<?>)helperField.get(handler);
            Field shopField = canvas.getField("shopNpc"); Object previousShop = shopField.get(null);
            Object candidate = null;
            for (Class<?> cls : instrumentation.getAllLoadedClasses()) {
                if (!cls.getName().equals("com.silverknight.GrahamNpcMenu") || cls == currentHelper) continue;
                try {
                    Field shop = cls.getDeclaredField("merchantShop"); shop.setAccessible(true);
                    Field owner = cls.getDeclaredField("shopOwnerId"); owner.setAccessible(true);
                    Object value = shop.get(null);
                    // The oldest live shop hook retained the option-2 response. Later
                    // hooks hold the current option-0 screen, so require a distinct view.
                    if (value != null && value != previousShop && owner.getInt(null) == ownerId) candidate = value;
                } catch (NoSuchFieldException ignored) {}
            }
            if (candidate == null) {
                System.setProperty("hso.grahamCatalogRecovery", "original option-2 screen unavailable"); return;
            }
            try {
                shopField.set(null, candidate);
                Object saved = MethodHandles.publicLookup().unreflect(currentHelper.getMethod("rememberMerchantShop", int.class))
                        .invokeWithArguments(2);
                System.setProperty("hso.grahamCatalogRecovery", "original server option-2 catalog retained=" + saved);
            } finally { shopField.set(null, previousShop); }
        } catch (Throwable error) {
            System.setProperty("hso.grahamCatalogRecovery", "failed: " + error);
            if (error instanceof Exception) throw (Exception)error;
            throw new IllegalStateException(error);
        }
    }
}
