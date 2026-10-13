import java.io.*;
import java.lang.instrument.Instrumentation;
import java.lang.invoke.*;
import java.lang.reflect.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;

/** One user-triggered remote trial: rewrite one menu selection, then buy one known Saphia. */
public final class HsoDylanRemoteClickReplay implements Runnable {
    private final Instrumentation inst;
    private final Path root;
    private final CountDownLatch clicked=new CountDownLatch(1), shopReady=new CountDownLatch(1);
    private final AtomicBoolean used=new AtomicBoolean();
    private Object service, session, player, map, senderBefore, receiverBefore, sender, receiver;
    private Class<?> game, canvas, serviceClass, messageClass;
    private int mapId, area, x, y;
    private long before;
    private volatile boolean selectionSent;
    private volatile int inventories;

    private HsoDylanRemoteClickReplay(Instrumentation inst,String args){this.inst=inst;root=Paths.get(args.split("\\|",2)[1]);}
    public static synchronized void agentmain(String args,Instrumentation inst){
        if(System.getProperty("hso.dylanRemoteClick.armed")!=null)throw new IllegalStateException("This trial was already armed");
        System.setProperty("hso.dylanRemoteClick.armed","true");
        Thread t=new Thread(new HsoDylanRemoteClickReplay(inst,args),"HSO one remote Dylan click");t.setDaemon(true);t.start();
    }
    private static Object call(Method method,Object owner,Object... args)throws Throwable{
        MethodHandle h=MethodHandles.publicLookup().unreflect(method);if(!Modifier.isStatic(method.getModifiers()))h=h.bindTo(owner);
        return h.invokeWithArguments(args);
    }
    private static Object field(Object owner,String name)throws Exception{return owner.getClass().getField(name).get(owner);}
    private static int number(Object owner,String name)throws Exception{return ((Number)field(owner,name)).intValue();}
    private long coins()throws Exception{return ((Number)field(player,"coin")).longValue();}
    private synchronized void log(String text){
        String line=new java.text.SimpleDateFormat("HH:mm:ss.SSS").format(new java.util.Date())+" DYLAN_REMOTE_CLICK "+text+"\n";
        try{Files.write(root.resolve("hso_remote_sell_probe.log"),line.getBytes(StandardCharsets.UTF_8),StandardOpenOption.CREATE,StandardOpenOption.APPEND);}catch(IOException ignored){}
        System.setProperty("hso.dylanRemoteClick.status",text);
    }
    private void guard()throws Throwable{
        if(!(Boolean)call(session.getClass().getMethod("isConnected"),session)||game.getField("player").get(null)!=player
                ||canvas.getField("loadmap").get(null)!=map||number(map,"idMap")!=mapId||number(player,"x")!=x||number(player,"y")!=y
                ||((Number)Class.forName("Thread_More.LoadMap",false,game.getClassLoader()).getField("Area").get(null)).intValue()!=area)
            throw new IllegalStateException("Account/map/position changed");
    }
    private Object message(int command,byte[] payload)throws Throwable{
        Object m=MethodHandles.publicLookup().findConstructor(messageClass,MethodType.methodType(void.class,byte.class)).invokeWithArguments((byte)command);
        ((DataOutputStream)call(messageClass.getMethod("writer"),m)).write(payload);return m;
    }
    private Object outbound(Object proxy,Method method,Object[] args)throws Throwable{
        if(method.getName().equals("sendMessage")&&args!=null&&args.length==1&&number(args[0],"command")==-30){
            byte[] bytes=(byte[])call(messageClass.getMethod("getData"),args[0]);
            if(bytes.length==4&&bytes[0]==(byte)0xFF&&bytes[1]==(byte)0xC7&&bytes[3]==0&&used.compareAndSet(false,true)){
                try{
                    guard();
                    if(coins()!=before)throw new IllegalStateException("Balance changed before click");
                    log("USER_CLICK npc=-57 original_menu="+(bytes[2]&255)+" index=0");
                    Object replacement=message(-30,new byte[]{(byte)0xFF,(byte)0xC7,2,0});
                    Object result=call(method,senderBefore,replacement);
                    selectionSent=true;log("TX cmd=-30 payload=FFC70200 map="+mapId);
                    clicked.countDown();return result;
                }catch(Throwable error){log("CLICK_ABORT "+error);clicked.countDown();}
            }
        }
        return call(method,senderBefore,args==null?new Object[0]:args);
    }
    private Object inbound(Object proxy,Method method,Object[] args)throws Throwable{
        boolean shop=false,inventory=false;
        if(method.getName().equals("onMessage")&&args!=null&&args.length==1){
            Object m=args[0];int cmd=number(m,"command");
            if(cmd==23||cmd==-30||cmd==16||cmd==-32||cmd==53){
                DataInputStream data=(DataInputStream)call(messageClass.getMethod("reader"),m);data.mark(data.available()+1);
                try{
                    if(cmd==-30){int npc=data.readShort(),menu=data.readByte(),count=data.readUnsignedByte();
                        if(npc==-57&&count<=32){StringBuilder labels=new StringBuilder();for(int i=0;i<count;i++)labels.append('[').append(data.readUTF()).append(']');log("RX menu npc=-57 menu="+menu+" labels="+labels);}}
                    else if(cmd==23){String title=data.readUTF();int type=data.readByte();
                        if(type==-57)log("RX Dylan chat "+title);
                        if(type>=0){log("RX shop type="+type+" title="+title);shop=selectionSent&&type==1&&(title.equals("Của hàng đá quý")||title.equals("Cửa hàng đá quý"));}}
                    else if(cmd==16)inventory=true;
                    else if(cmd==-32)log("RX dialog id="+data.readShort()+" type="+data.readByte()+" text="+data.readUTF());
                    else log("RX notice "+data.readUTF());
                }catch(Throwable error){log("CAPTURE error="+error);}finally{data.reset();}
            }
        }
        Object result=call(method,receiverBefore,args==null?new Object[0]:args);
        if(shop)shopReady.countDown();if(inventory){++inventories;log("RX inventory coin="+coins());}return result;
    }
    @Override public void run(){
        boolean purchaseSent=false;
        try{
            for(Class<?> c:inst.getAllLoadedClasses())if(c.getName().equals("GameScreen.GameScreen")&&c.getField("player").get(null)!=null){game=c;break;}
            if(game==null)throw new IllegalStateException("No game");ClassLoader loader=game.getClassLoader();
            canvas=Class.forName("Main.GameCanvas",false,loader);serviceClass=Class.forName("netcommand.global.GlobalService",false,loader);messageClass=Class.forName("net.Message",false,loader);
            service=call(serviceClass.getMethod("gI"),null);session=call(Class.forName("CLib.Session_ME",false,loader).getMethod("gI"),null);
            player=game.getField("player").get(null);map=canvas.getField("loadmap").get(null);mapId=number(map,"idMap");
            area=((Number)Class.forName("Thread_More.LoadMap",false,loader).getField("Area").get(null)).intValue();x=number(player,"x");y=number(player,"y");before=coins();
            if(mapId!=8||!"lronic".equals(field(player,"name"))||before!=216885L||number(player,"typePk")!=11)throw new IllegalStateException("Different trial context");
            guard();Class<?> bridge=Class.forName("com.silverknight.HsoBridge",false,loader);
            if((Boolean)call(bridge.getMethod("isRemoteSellBusy"),null))throw new IllegalStateException("Existing sale active");
            senderBefore=field(service,"session");receiverBefore=field(session,"messageHandler");
            sender=Proxy.newProxyInstance(loader,new Class<?>[]{Class.forName("net.ISession",false,loader)},this::outbound);
            receiver=Proxy.newProxyInstance(loader,new Class<?>[]{Class.forName("net.IMessageHandler",false,loader)},this::inbound);
            serviceClass.getField("session").set(service,sender);session.getClass().getField("messageHandler").set(session,receiver);
            log("ARMED map="+mapId+" area="+area+" local="+x+","+y+" coin="+before+" waiting_for_user_click_seconds=180");
            if(!clicked.await(180,TimeUnit.SECONDS)||!selectionSent)throw new IllegalStateException("No eligible user selection was forwarded");
            boolean receivedShop=shopReady.await(6500,TimeUnit.MILLISECONDS);guard();
            if(coins()!=before)throw new IllegalStateException("Balance changed; avoid a duplicate purchase");
            log("SHOP_ACK="+receivedShop+"; replaying the known validated Saphia packet once");
            int replies=inventories;Object buy=message(24,new byte[]{1,0x0E,0x06,0,1});
            guard();call(session.getClass().getMethod("sendMessage",messageClass),session,buy);purchaseSent=true;
            log("TX cmd=24 payload=010E060001 quantity=1 maximum_expected_cost=10000");
            long until=System.currentTimeMillis()+10000;
            while(System.currentTimeMillis()<until){guard();if(inventories>replies&&coins()==before-10000L)break;Thread.sleep(100);}
            log("RESULT actual_shop_received="+receivedShop+" coin="+before+"->"+coins()+" inventory_replies="+(inventories-replies)+" debit_confirmed="+(inventories>replies&&coins()==before-10000L));
        }catch(Throwable error){log("ABORT purchase_sent="+purchaseSent+" error="+error);}
        finally{try{
            if(sender!=null&&field(service,"session")==sender)serviceClass.getField("session").set(service,senderBefore);
            if(receiver!=null&&field(session,"messageHandler")==receiver)session.getClass().getField("messageHandler").set(session,receiverBefore);
            log("END hooks_restored purchase_attempts="+(purchaseSent?1:0)+" no movement or fake shop responses");
        }catch(Throwable error){log("RESTORE error="+error);}}
    }
}
