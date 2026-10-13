import java.io.*;
import java.lang.instrument.Instrumentation;
import java.lang.invoke.*;
import java.lang.reflect.*;
import java.net.URLClassLoader;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;

/** Preserve and reopen the server-provided merchant shop in the current live session. */
public final class HsoGrahamShopAgent implements InvocationHandler {
    private final Object original;
    private final boolean outbound;
    private final Class<?> helper, canvas, game;
    private final Path logPath;

    private HsoGrahamShopAgent(Object original, boolean outbound, Class<?> helper,
                              Class<?> canvas, Class<?> game, Path root) {
        this.original=original;this.outbound=outbound;this.helper=helper;
        this.canvas=canvas;this.game=game;logPath=root.resolve("hso_remote_sell_probe.log");
    }

    private static Object call(Method method,Object target,Object... args) throws Throwable {
        MethodHandle handle=MethodHandles.publicLookup().unreflect(method);
        if(!Modifier.isStatic(method.getModifiers()))handle=handle.bindTo(target);
        return handle.invokeWithArguments(args==null?new Object[0]:args);
    }

    private void log(String value) {
        System.setProperty("hso.grahamShopPatch",value);
        String line=new java.text.SimpleDateFormat("HH:mm:ss.SSS").format(new java.util.Date())+" GRAHAM_SHOP "+value+"\n";
        try(FileOutputStream stream=new FileOutputStream(logPath.toFile(),true)){
            stream.write(line.getBytes(StandardCharsets.UTF_8));
        }catch(IOException ignored){}
    }

    public static void agentmain(String args,Instrumentation instrumentation) throws Exception {
        try{
            String[] parts=args.split("\\|",2);Path root=Paths.get(parts[1]);
            ClassLoader loader=null;Class<?> game=null;
            for(Class<?> cls:instrumentation.getAllLoadedClasses()){
                if(cls.getName().equals("GameScreen.GameScreen")&&cls.getField("player").get(null)!=null){
                    loader=cls.getClassLoader();game=cls;break;
                }
            }
            if(loader==null)throw new IllegalStateException("No live game");
            Class<?> canvas=Class.forName("Main.GameCanvas",false,loader);
            URLClassLoader support=new URLClassLoader(new java.net.URL[]{root.resolve("patch_bin/remote_sell_game").toUri().toURL()},loader);
            Class<?> helper=Class.forName("com.silverknight.GrahamNpcMenu",true,support);
            Class<?> serviceClass=Class.forName("netcommand.global.GlobalService",false,loader);
            Object service=call(serviceClass.getMethod("gI"),null);
            Field field=serviceClass.getField("session");Object original=field.get(service);
            // Replace this task's earlier display-only hook rather than stacking it.
            if(Proxy.isProxyClass(original.getClass())){
                InvocationHandler previous=Proxy.getInvocationHandler(original);
                if(previous.getClass().getName().equals("HsoGrahamMenuAgent")){
                    Field delegate=previous.getClass().getDeclaredField("originalSession");delegate.setAccessible(true);
                    original=delegate.get(previous);
                }
            }
            HsoGrahamShopAgent sender=new HsoGrahamShopAgent(original,true,helper,canvas,game,root);
            Class<?> sessionType=Class.forName("net.ISession",false,loader);
            field.set(service,Proxy.newProxyInstance(loader,new Class<?>[]{sessionType},sender));
            Class<?> sessionClass=Class.forName("CLib.Session_ME",false,loader);
            Object session=call(sessionClass.getMethod("gI"),null);
            Field handlerField=sessionClass.getField("messageHandler");
            Class<?> handlerType=Class.forName("net.IMessageHandler",false,loader);
            HsoGrahamShopAgent receiver=new HsoGrahamShopAgent(handlerField.get(session),false,helper,canvas,game,root);
            handlerField.set(session,Proxy.newProxyInstance(loader,new Class<?>[]{handlerType},receiver));
            // The user's latest real shop reply followed Graham option 0 in map 8.
            boolean cached=(Boolean)call(helper.getMethod("rememberMerchantShop",int.class),null,0);
            sender.log("installed; existing_server_shop_retained="+cached);
            // Open the requested read-only shop UI through the same native selection packet.
            call(serviceClass.getMethod("Dynamic_Menu",short.class,byte.class,byte.class),service,(short)-58,(byte)0,(byte)2);
        }catch(Throwable error){
            System.setProperty("hso.grahamShopPatch","failed: "+error);
            if(error instanceof Exception)throw (Exception)error;throw new IllegalStateException(error);
        }
    }

    @Override public Object invoke(Object proxy,Method method,Object[] args) throws Throwable {
        if(outbound&&method.getName().equals("sendMessage")&&args!=null&&args.length==1){
            Object msg=args[0];int cmd=((Number)msg.getClass().getField("command").get(msg)).intValue();
            if(cmd==23||cmd==-30||cmd==24){
                byte[] data=(byte[])call(msg.getClass().getMethod("getData"),msg);
                DataInputStream input=new DataInputStream(new ByteArrayInputStream(data));
                if(cmd==23&&data.length==1&&data[0]==-58)call(helper.getMethod("show"),null);
                if(cmd==-30&&data.length==4){
                    int npc=input.readShort(),menu=input.readByte(),option=input.readUnsignedByte();
                    if(npc==-58){
                        boolean shown=(Boolean)call(helper.getMethod("showSubmenu",int.class,int.class),null,npc,option);
                        Object map=canvas.getField("loadmap").get(null);
                        log("TX select npc="+npc+" menu="+menu+" option="+option+" map="+map.getClass().getField("idMap").get(map)+" retained_shop_shown="+shown);
                    }
                }
                if(cmd==24&&data.length==5)log("TX purchase type="+input.readByte()+" id="+input.readShort()+" quantity="+input.readUnsignedShort());
            }
        }
        int incoming=0;String title=null;
        if(!outbound&&method.getName().equals("onMessage")&&args!=null&&args.length==1){
            Object msg=args[0];incoming=((Number)msg.getClass().getField("command").get(msg)).intValue();
            if(incoming==23){
                DataInputStream reader=(DataInputStream)call(msg.getClass().getMethod("reader"),msg);
                reader.mark(reader.available()+1);
                try{title=reader.readUTF();int type=reader.readByte();log("RX shop title="+title+" type="+type);}
                finally{reader.reset();}
            }
        }
        Object result=call(method,original,args);
        if(incoming==23&&"Đồ thương nhân".equalsIgnoreCase(title))call(helper.getMethod("rememberMerchantShop"),null);
        if(incoming==16){
            Object player=game.getField("player").get(null);
            if(player!=null)log("RX inventory coin="+player.getClass().getField("coin").get(player));
        }
        return result;
    }
}
