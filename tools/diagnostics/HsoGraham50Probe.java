import java.io.*;
import java.lang.instrument.Instrumentation;
import java.lang.invoke.*;
import java.lang.reflect.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;

/** One user-supplied type-50 caravan sale confirmation; no movement or other confirmations. */
public final class HsoGraham50Probe implements Runnable {
    private final Instrumentation inst;
    private final Path root;
    private Class<?> game,canvas;
    private Object session,previous,wrapper,player,map;
    private long before;
    private volatile int inventoryUpdates,caravanExit;
    private int caravanId;
    private HsoGraham50Probe(Instrumentation inst,String args){this.inst=inst;root=Paths.get(args.split("\\|",2)[1]);}
    public static synchronized void agentmain(String args,Instrumentation inst){
        if(System.getProperty("hso.graham50.started")!=null)throw new IllegalStateException("This one-shot confirmation already ran");
        System.setProperty("hso.graham50.started","true");Thread t=new Thread(new HsoGraham50Probe(inst,args),"HSO user type50 confirmation");t.setDaemon(true);t.start();
    }
    private static Object call(Method m,Object obj,Object... args)throws Throwable{MethodHandle h=MethodHandles.publicLookup().unreflect(m);if(!Modifier.isStatic(m.getModifiers()))h=h.bindTo(obj);return h.invokeWithArguments(args);}
    private static Object field(Object obj,String name)throws Exception{return obj.getClass().getField(name).get(obj);}
    private static int num(Object obj,String name)throws Exception{return ((Number)field(obj,name)).intValue();}
    private long coins()throws Exception{return ((Number)field(player,"coin")).longValue();}
    private synchronized void log(String s){String line=new java.text.SimpleDateFormat("HH:mm:ss.SSS").format(new java.util.Date())+" GRAHAM50 "+s+"\n";try{Files.write(root.resolve("hso_remote_sell_probe.log"),line.getBytes(StandardCharsets.UTF_8),StandardOpenOption.CREATE,StandardOpenOption.APPEND);}catch(IOException ignored){}System.setProperty("hso.graham50.status",s);}
    private Object receive(Object obj,Method method,Object[] args)throws Throwable{
        boolean inventory=false;
        if(method.getName().equals("onMessage")&&args!=null&&args.length==1){Object m=args[0];int cmd=num(m,"command");
            if(cmd==16||cmd==37||cmd==-32||cmd==8||cmd==53){
                DataInputStream in=(DataInputStream)call(m.getClass().getMethod("reader"),m);in.mark(in.available()+1);
                try{if(cmd==16)inventory=true;
                    else if(cmd==37)log("RX info37 text="+in.readUTF().replace('\n','|')+" extra="+in.readUTF()+" type="+in.readByte());
                    else if(cmd==-32)log("RX dialog id="+in.readShort()+" type="+in.readByte()+" text="+in.readUTF());
                    else if(cmd==8){int id=in.readShort();log("RX exit id="+id);if(id==caravanId)++caravanExit;}
                    else log("RX notice "+in.readUTF());
                }catch(Throwable error){log("CAPTURE error="+error);}finally{in.reset();}
            }
        }
        Object result=call(method,previous,args==null?new Object[0]:args);
        if(inventory){++inventoryUpdates;log("RX inventory coin="+coins());}return result;
    }
    @Override public void run(){boolean sent=false;try{
        for(Class<?> c:inst.getAllLoadedClasses())if(c.getName().equals("GameScreen.GameScreen")&&c.getField("player").get(null)!=null){game=c;break;}
        if(game==null)throw new IllegalStateException("No game");ClassLoader loader=game.getClassLoader();canvas=Class.forName("Main.GameCanvas",false,loader);
        player=game.getField("player").get(null);map=canvas.getField("loadmap").get(null);session=call(Class.forName("CLib.Session_ME",false,loader).getMethod("gI"),null);
        if(!"lronic".equals(field(player,"name"))||num(player,"ID")!=-8209||num(map,"idMap")!=19||num(player,"typePk")!=11||coins()!=206885L||!(Boolean)call(session.getClass().getMethod("isConnected"),session))throw new IllegalStateException("Trial context changed");
        Object read=canvas.getField("readMessenge").get(null);Field dialog=read.getClass().getDeclaredField("idDialog");dialog.setAccessible(true);Field type=read.getClass().getDeclaredField("typeDialog");type.setAccessible(true);
        int id=dialog.getShort(read),lastType=type.getByte(read);
        if(id!=num(player,"ID"))throw new IllegalStateException("Saved server dialog is not for the current character");
        Object objects=game.getField("Vecplayers").get(null);int count=(Integer)call(objects.getClass().getMethod("size"),objects);Object caravan=null;
        for(int i=0;i<count;i++){Object o=call(objects.getClass().getMethod("elementAt",int.class),objects,i);if(o!=null&&o.getClass().getName().equals("GameObjects.Monstervantieu")&&field(o,"nameowner").equals(field(player,"name")))caravan=o;}
        if(caravan==null||num(caravan,"ID")!=8286||num(caravan,"hp")<=0||Math.abs(num(caravan,"x")-num(player,"x"))>=75||Math.abs(num(caravan,"y")-num(player,"y"))>=75)throw new IllegalStateException("Expected living owned bull is not nearby");
        caravanId=num(caravan,"ID");before=coins();
        Class<?> bridge=Class.forName("com.silverknight.HsoBridge",false,loader);if((Boolean)call(bridge.getMethod("isRemoteSellBusy"),null))throw new IllegalStateException("Another sale active");
        previous=field(session,"messageHandler");wrapper=Proxy.newProxyInstance(loader,new Class<?>[]{Class.forName("net.IMessageHandler",false,loader)},this::receive);session.getClass().getField("messageHandler").set(session,wrapper);
        log("BEGIN map=19 player=-8209 caravan="+caravanId+" coin="+before+" saved_server_dialog_id="+id+" saved_dialog_type="+lastType+" test_type=50 supplied_by_user");
        Class<?> serviceClass=Class.forName("netcommand.global.GlobalService",false,loader);Object service=call(serviceClass.getMethod("gI"),null);
        call(serviceClass.getMethod("remoteSellConfirm",int.class,int.class),service,id,50);sent=true;
        log("TX cmd=-32 payload="+String.format("%04X3201",id&65535)+" dialog_id="+id+" type=50 value=1 count=1");
        long until=System.currentTimeMillis()+10000;
        while(System.currentTimeMillis()<until){if(game.getField("player").get(null)!=player||num(map,"idMap")!=19)throw new IllegalStateException("Context changed after send");if(inventoryUpdates>0&&coins()>before)break;Thread.sleep(100);}
        log("RESULT coin="+before+"->"+coins()+" inventory_updates="+inventoryUpdates+" bull_exit_packets="+caravanExit+" credit_confirmed="+(inventoryUpdates>0&&coins()>before));
    }catch(Throwable error){log("ABORT confirmation_sent="+sent+" error="+error);}finally{try{if(wrapper!=null&&field(session,"messageHandler")==wrapper)session.getClass().getField("messageHandler").set(session,previous);log("END handler_restored confirmations="+(sent?1:0)+" no movement/map-change/other-dialogs");}catch(Throwable error){log("RESTORE error="+error);}}}
}
