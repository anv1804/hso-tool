import java.io.*;
import java.lang.instrument.Instrumentation;
import java.lang.invoke.*;
import java.lang.reflect.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.concurrent.*;

/** Read-only merchant/caravan audit; no movement, purchase, sale or paid pet option. */
public final class HsoDylanContextAudit implements Runnable {
    private final Instrumentation inst;
    private final Path root;
    private final boolean grahamChat;
    private final CountDownLatch saleReady=new CountDownLatch(1);
    private volatile int saleId=Integer.MIN_VALUE;
    private volatile String saleText="";
    private final CountDownLatch cargoMenu=new CountDownLatch(1), cargoInfo=new CountDownLatch(1);
    private Object session, previous, proxy, player, map;
    private Class<?> game, canvas, message;
    private int menuId, cargoOption=-1;
    private volatile boolean requestingCargo;
    private HsoDylanContextAudit(Instrumentation inst,String args){this.inst=inst;String[] parts=args.split("\\|",3);root=Paths.get(parts[1]);grahamChat=parts.length==3&&"graham-chat".equals(parts[2]);}
    public static void agentmain(String args,Instrumentation inst){Thread t=new Thread(new HsoDylanContextAudit(inst,args),"HSO Dylan context audit");t.setDaemon(true);t.start();}
    private static Object call(Method m,Object obj,Object... args)throws Throwable{MethodHandle h=MethodHandles.publicLookup().unreflect(m);if(!Modifier.isStatic(m.getModifiers()))h=h.bindTo(obj);return h.invokeWithArguments(args);}
    private static Object field(Object obj,String name)throws Exception{return obj.getClass().getField(name).get(obj);}
    private static int num(Object obj,String name)throws Exception{return ((Number)field(obj,name)).intValue();}
    private void log(String s){String line=new java.text.SimpleDateFormat("HH:mm:ss.SSS").format(new java.util.Date())+" DYLAN_CONTEXT "+s+"\n";try{Files.write(root.resolve("hso_remote_sell_probe.log"),line.getBytes(StandardCharsets.UTF_8),StandardOpenOption.CREATE,StandardOpenOption.APPEND);}catch(IOException ignored){}System.setProperty("hso.dylanContext.status",s);}
    private Object receive(Object obj,Method method,Object[] args)throws Throwable{
        if(method.getName().equals("onMessage")&&args!=null&&args.length==1){
            Object msg=args[0];int cmd=num(msg,"command");
            if(cmd==-30||cmd==37||cmd==-44||cmd==23||cmd==53||cmd==60||cmd==-32){
                DataInputStream in=(DataInputStream)call(message.getMethod("reader"),msg);in.mark(in.available()+1);
                byte[] raw=new byte[in.available()];in.readFully(raw);in.reset();
                try{
                    if(cmd==-30){int npc=in.readShort(),id=in.readByte(),count=in.readUnsignedByte();
                        if((npc==-56||npc==-57||npc==-58)&&count<=32){StringBuilder labels=new StringBuilder();for(int i=0;i<count;i++){String label=in.readUTF();labels.append('[').append(i).append(':').append(label).append(']');if(npc==-56&&"Hành trang".equalsIgnoreCase(label))cargoOption=i;}log("RX menu npc="+npc+" menu="+id+" labels="+labels);if(npc==-56){menuId=id;cargoMenu.countDown();}}}
                    else if(cmd==37){String text=in.readUTF(),extra=in.readUTF();int type=in.readByte();log("RX info37 type="+type+" text="+text.replace('\n','|')+" extra="+extra);if(requestingCargo)cargoInfo.countDown();}
                    else if(cmd==-44){int id=in.readShort();String name=in.readUTF();in.readInt();in.readInt();in.readShort();log("RX NPC_INFO id="+id+" name="+name+" x="+in.readShort()+" y="+in.readShort());}
                    else if(cmd==23){String title=in.readUTF();int type=in.readByte();if(type>=0||type==-57)log("RX npc/shop type="+type+" text="+title);}
                    else if(cmd==60)log("RX chat60 npc="+in.readByte()+" text="+in.readUTF());
                    else if(cmd==-32){int id=in.readShort(),type=in.readByte();String text=in.readUTF();log("RX dialog id="+id+" type="+type+" text="+text);if(type==50&&id==num(player,"ID")&&text.contains("bán tất cả")){saleId=id;saleText=text;saleReady.countDown();}}
                    else log("RX notice "+in.readUTF());
                }catch(Throwable error){StringBuilder hex=new StringBuilder();for(int i=0;i<Math.min(raw.length,64);i++)hex.append(String.format("%02X",raw[i]&255));log("CAPTURE cmd="+cmd+" bytes="+raw.length+" raw="+hex+" error="+error);}finally{in.reset();}
            }
        }
        return call(method,previous,args==null?new Object[0]:args);
    }
    private void send(int cmd,byte[] data)throws Throwable{
        if(game.getField("player").get(null)!=player||canvas.getField("loadmap").get(null)!=map||!(Boolean)call(session.getClass().getMethod("isConnected"),session))throw new IllegalStateException("Context changed");
        Object msg=MethodHandles.publicLookup().findConstructor(message,MethodType.methodType(void.class,byte.class)).invokeWithArguments((byte)cmd);
        ((DataOutputStream)call(message.getMethod("writer"),msg)).write(data);call(session.getClass().getMethod("sendMessage",message),session,msg);
        StringBuilder hex=new StringBuilder();for(byte b:data)hex.append(String.format("%02X",b&255));log("TX cmd="+cmd+" payload="+hex);
    }
    @Override public void run(){try{
        for(Class<?> c:inst.getAllLoadedClasses())if(c.getName().equals("GameScreen.GameScreen")&&c.getField("player").get(null)!=null){game=c;break;}if(game==null)throw new IllegalStateException("No game");
        ClassLoader loader=game.getClassLoader();canvas=Class.forName("Main.GameCanvas",false,loader);message=Class.forName("net.Message",false,loader);
        player=game.getField("player").get(null);map=canvas.getField("loadmap").get(null);session=call(Class.forName("CLib.Session_ME",false,loader).getMethod("gI"),null);
        log("BEGIN name="+field(player,"name")+" map="+num(map,"idMap")+" x="+num(player,"x")+" y="+num(player,"y")+" role="+num(player,"typePk")+" coin="+field(player,"coin"));
        Object equipment=Class.forName("GameObjects.Item",false,loader).getField("VecEquipPlayer").get(null);Object slot=call(equipment.getClass().getMethod("get",String.class),equipment,"11");
        log("EQUIP slot11="+(slot==null?"empty":"id="+num(slot,"Id")+" template="+num(slot,"IdTem")+" name="+field(slot,"itemName")));
        Object objects=game.getField("Vecplayers").get(null);int n=(Integer)call(objects.getClass().getMethod("size"),objects);
        for(int i=0;i<n;i++){Object value=call(objects.getClass().getMethod("elementAt",int.class),objects,i);if(value==null)continue;
            if(value.getClass().getName().equals("GameObjects.Monstervantieu")){String owner=(String)field(value,"nameowner");if(owner.equals(field(player,"name")))log("OWN_CARAVAN id="+num(value,"ID")+" owner="+owner+" hp="+num(value,"hp")+" x="+num(value,"x")+" y="+num(value,"y")+" template="+num(value,"catalogyMonster"));}}
        previous=field(session,"messageHandler");proxy=Proxy.newProxyInstance(loader,new Class<?>[]{Class.forName("net.IMessageHandler",false,loader)},this::receive);session.getClass().getField("messageHandler").set(session,proxy);
        if(grahamChat){
            if(num(map,"idMap")!=19||!"lronic".equals(field(player,"name"))||((Number)field(player,"coin")).longValue()!=216885L)throw new IllegalStateException("The single-Saphia context changed");
            send(60,new byte[]{(byte)-57});Thread.sleep(1500);
            send(60,new byte[]{(byte)-58});Thread.sleep(1500);
            send(23,new byte[]{(byte)-58});Thread.sleep(200);
            send(-30,new byte[]{(byte)0xFF,(byte)0xC6,0,1});
            boolean fresh=saleReady.await(6500,TimeUnit.MILLISECONDS);
            if(fresh&&saleText.contains("20000 Vàng")&&((Number)field(player,"coin")).longValue()==216885L){
                ByteArrayOutputStream bytes=new ByteArrayOutputStream();DataOutputStream out=new DataOutputStream(bytes);out.writeShort(saleId);out.writeByte(50);out.writeByte(1);
                send(-32,bytes.toByteArray());Thread.sleep(5000);
                log("CHAT_SETUP_RESULT fresh_type50=true coin="+field(player,"coin"));
            }else log("CHAT_SETUP_RESULT fresh_type50="+fresh+" quote="+saleText+" coin="+field(player,"coin")+" no_confirmation_sent");
            return;
        }
        send(-44,new byte[]{(byte)0xFF,(byte)0xC7});Thread.sleep(2000);
        send(23,new byte[]{(byte)-57});Thread.sleep(2000);
        send(23,new byte[]{(byte)-56});
        if(cargoMenu.await(5000,TimeUnit.MILLISECONDS)&&cargoOption>=0){requestingCargo=true;send(-30,new byte[]{(byte)0xFF,(byte)0xC8,(byte)menuId,(byte)cargoOption});cargoInfo.await(5000,TimeUnit.MILLISECONDS);}
        log("RESULT coin="+field(player,"coin")+" map="+num(map,"idMap"));
    }catch(Throwable error){log("ABORT "+error);}finally{try{if(proxy!=null&&field(session,"messageHandler")==proxy)session.getClass().getField("messageHandler").set(session,previous);log("END handler_restored no purchase/sale/movement/paid-options");}catch(Throwable error){log("RESTORE error="+error);}}}
}
