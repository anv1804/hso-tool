import java.io.*;
import java.lang.instrument.Instrumentation;
import java.lang.invoke.*;
import java.lang.reflect.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.concurrent.*;

/** One explicitly requested Saphia purchase, using fresh server menu/shop data. */
public final class HsoDylanPurchaseReplay implements Runnable {
    private final Instrumentation inst;
    private final Path root;
    private final String expectedName;
    private final long expectedBalance;
    private final boolean testGrahamSale;
    private Class<?> game, canvas, messageClass;
    private Object player, map, session, original, wrapper;
    private int area, x, y, menuId, option = -1;
    private long coinBefore;
    private volatile int inventoryReplies;
    private volatile String stockNotice = "";
    private volatile boolean saleRequested;
    private volatile int saleDialogId = Integer.MIN_VALUE;
    private volatile String saleDialogText = "";
    private volatile int caravanExits;
    private int caravanId;
    private boolean saleConfirmationSent;
    private final CountDownLatch saleDialogReady = new CountDownLatch(1);
    private final CountDownLatch menuReady = new CountDownLatch(1), shopReady = new CountDownLatch(1);

    private HsoDylanPurchaseReplay(Instrumentation inst, String args) {
        this.inst = inst;
        String[] parts = args.split("\\|", 6);
        root = Paths.get(parts[1]);
        expectedName = parts.length >= 4 ? parts[2] : "lronic";
        expectedBalance = parts.length >= 4 ? Long.parseLong(parts[3]) : 236885L;
        testGrahamSale = parts.length == 6 && "graham50".equals(parts[5]);
    }

    public static synchronized void agentmain(String args, Instrumentation inst) {
        String[] parts = args.split("\\|", 6);
        String requestId = parts.length >= 5 ? parts[4] : "first";
        if (!requestId.matches("[a-zA-Z0-9_-]+")) throw new IllegalArgumentException("Invalid request ID");
        String key = "hso.dylanReplay.started." + requestId;
        if (System.getProperty(key) != null) throw new IllegalStateException("This request already started; do not buy twice");
        HsoDylanPurchaseReplay replay = new HsoDylanPurchaseReplay(inst, args);
        System.setProperty(key, "true");
        Thread worker = new Thread(replay, "HSO one Saphia replay");
        worker.setDaemon(true); worker.start();
    }

    private static Object call(Method method, Object owner, Object... args) throws Throwable {
        MethodHandle handle = MethodHandles.publicLookup().unreflect(method);
        if (!Modifier.isStatic(method.getModifiers())) handle = handle.bindTo(owner);
        return handle.invokeWithArguments(args);
    }
    private static Object field(Object owner, String name) throws Exception { return owner.getClass().getField(name).get(owner); }
    private static int number(Object owner, String name) throws Exception { return ((Number)field(owner,name)).intValue(); }
    private static Object privateField(Object owner, String name) throws Exception {
        Field field = owner.getClass().getDeclaredField(name); field.setAccessible(true); return field.get(owner);
    }
    private long coins() throws Exception { return ((Number)field(player,"coin")).longValue(); }
    private synchronized void log(String value) {
        String line = new java.text.SimpleDateFormat("HH:mm:ss.SSS").format(new java.util.Date()) + " DYLAN_REPLAY " + value + "\n";
        try { Files.write(root.resolve("hso_remote_sell_probe.log"), line.getBytes(StandardCharsets.UTF_8), StandardOpenOption.CREATE, StandardOpenOption.APPEND); }
        catch (IOException ignored) {}
        System.setProperty("hso.dylanReplay.status", value);
    }
    private void guard() throws Throwable {
        if (!(Boolean)call(session.getClass().getMethod("isConnected"),session)
                || game.getField("player").get(null) != player || canvas.getField("loadmap").get(null) != map
                || number(map,"idMap") != 19 || number(player,"x") != x || number(player,"y") != y
                || ((Number)Class.forName("Thread_More.LoadMap",false,game.getClassLoader()).getField("Area").get(null)).intValue() != area)
            throw new IllegalStateException("Player/map/position changed; no further packet will be sent");
    }

    private Object receive(Object proxy, Method method, Object[] args) throws Throwable {
        boolean menu = false, shop = false, inventory = false;
        if (method.getName().equals("onMessage") && args != null && args.length == 1) {
            Object message = args[0]; int command = number(message,"command");
            if (command == 23 || command == -30 || command == -32 || command == 53 || command == 16 || command == 8) {
                DataInputStream data = (DataInputStream)call(message.getClass().getMethod("reader"),message);
                data.mark(data.available()+1);
                try {
                    if (command == -30) {
                        int npc = data.readShort(), id = data.readByte(), count = data.readUnsignedByte();
                        if (npc == -57 && count <= 32) {
                            for (int i=0;i<count;++i) if ("Mua bán".equalsIgnoreCase(data.readUTF())) option=i;
                            menuId=id; menu=option>=0;
                            log("RX menu npc=-57 menu="+menuId+" option="+option);
                        }
                    } else if (command == 23) {
                        String title=data.readUTF(); int type=data.readByte();
                        if (type == -57) { stockNotice=title; log("RX Dylan chat "+title); }
                        else if (type == 1 && (title.equals("Của hàng đá quý") || title.equals("Cửa hàng đá quý"))) {
                            shop=true; log("RX shop cmd=23 type=1 title="+title);
                            data.reset(); byte[] payload=new byte[data.available()]; data.readFully(payload);
                            Path directory=root.resolve("patch_bin/dylan_replay"); Files.createDirectories(directory);
                            Files.write(directory.resolve("server-shop-payload.bin"),payload);
                        }
                    } else if (command == 16) inventory=true;
                    else if (command == -32) {
                        int id=data.readShort(), type=data.readByte(); String text=data.readUTF();
                        log("RX dialog id="+id+" type="+type+" text="+text);
                        if(saleRequested && type==50 && id==number(player,"ID") && text.contains("bán tất cả")) {
                            saleDialogId=id; saleDialogText=text; saleDialogReady.countDown();
                        }
                    } else if(command==8) {
                        int id=data.readShort(); log("RX exit id="+id); if(id==caravanId)++caravanExits;
                    }
                    else log("RX notice "+data.readUTF());
                } catch (Throwable error) { log("CAPTURE error="+error); }
                finally { data.reset(); }
            }
        }
        Object result=call(method,original,args==null?new Object[0]:args);
        if(menu)menuReady.countDown();
        if(shop)shopReady.countDown();
        if(inventory){++inventoryReplies;log("RX inventory cmd=16 coin="+coins());}
        return result;
    }

    private void packet(int command, byte[] payload) throws Throwable {
        guard();
        Object message=MethodHandles.publicLookup().findConstructor(messageClass,MethodType.methodType(void.class,byte.class)).invokeWithArguments((byte)command);
        DataOutputStream writer=(DataOutputStream)call(messageClass.getMethod("writer"),message); writer.write(payload);
        StringBuilder hex=new StringBuilder(); for(byte b:payload)hex.append(String.format("%02X",b&255));
        log("TX cmd="+command+" payload="+hex);
        call(session.getClass().getMethod("sendMessage",messageClass),session,message);
        call(messageClass.getMethod("cleanup"),message);
    }

    private Object findSaphia() throws Throwable {
        Object screen=canvas.getField("shopNpc").get(null), tabs=field(screen,"VecTabScreen");
        int count=(Integer)call(tabs.getClass().getMethod("size"),tabs);
        for(int i=0;i<count;++i){
            Object tab=call(tabs.getClass().getMethod("elementAt",int.class),tabs,i);
            String title=(String)field(tab,"nameTab");
            if (!title.equals("Của hàng đá quý") && !title.equals("Cửa hàng đá quý")) continue;
            if (((Number)privateField(tab,"IDTab")).intValue()!=1) throw new IllegalStateException("Unexpected shop type");
            Object items=privateField(tab,"vecShop"); int size=(Integer)call(items.getClass().getMethod("size"),items);
            for(int j=0;j<size;++j){
                Object item=call(items.getClass().getMethod("elementAt",int.class),items,j);
                if("Saphia".equalsIgnoreCase((String)field(item,"itemName")))return item;
            }
        }
        throw new IllegalStateException("Fresh server shop has no exact Saphia item");
    }

    private Object ownedCaravan() throws Throwable {
        Object objects=game.getField("Vecplayers").get(null);
        int count=(Integer)call(objects.getClass().getMethod("size"),objects);
        for(int i=0;i<count;i++) {
            Object object=call(objects.getClass().getMethod("elementAt",int.class),objects,i);
            if(object!=null && object.getClass().getName().equals("GameObjects.Monstervantieu")
                    && field(object,"nameowner").equals(field(player,"name"))
                    && number(object,"hp")>0 && !(Boolean)field(object,"isRemove")) return object;
        }
        return null;
    }

    private void remoteGrahamSale() throws Throwable {
        guard(); Object caravan=ownedCaravan();
        if(caravan==null || number(caravan,"ID")!=caravanId
                || Math.abs(number(caravan,"x")-x)>=75 || Math.abs(number(caravan,"y")-y)>=75)
            throw new IllegalStateException("The purchased cargo's owned transport changed or moved away");
        long saleBefore=coins(); int repliesBefore=inventoryReplies;
        saleRequested=true;
        log("REMOTE_SALE_BEGIN map=19 npc=-58 user_authorized_type=50 player_id="+number(player,"ID")+" caravan="+caravanId+" coin="+saleBefore);
        packet(23,new byte[]{(byte)-58});
        Thread.sleep(200);
        packet(-30,new byte[]{(byte)0xFF,(byte)0xC6,0,1});
        boolean realDialog=saleDialogReady.await(300,TimeUnit.MILLISECONDS);
        if(realDialog && !saleDialogText.contains("20000 Vàng"))
            throw new IllegalStateException("Server sale quote differs from the observed single-Saphia quote; no confirmation");
        int id=realDialog?saleDialogId:number(player,"ID");
        log("CONFIRM_SOURCE="+(realDialog?"fresh_server_type50":"current_player_id_mapping_verified_by_native_type50")+" id="+id);
        ByteArrayOutputStream answer=new ByteArrayOutputStream();DataOutputStream out=new DataOutputStream(answer);
        out.writeShort(id);out.writeByte(50);out.writeByte(1);
        packet(-32,answer.toByteArray());
        saleConfirmationSent=true;
        long until=System.currentTimeMillis()+10000;
        while(System.currentTimeMillis()<until) {
            guard();
            if(inventoryReplies>repliesBefore && coins()==saleBefore+20000L)break;
            Thread.sleep(100);
        }
        Object remaining=ownedCaravan();
        log("REMOTE_SALE_RESULT fresh_dialog="+realDialog+" coin="+saleBefore+"->"+coins()+" inventory_updates="+(inventoryReplies-repliesBefore)
                +" caravan_exit_packets="+caravanExits+" same_caravan_remains="+(remaining!=null&&number(remaining,"ID")==caravanId)
                +" credit_confirmed="+(inventoryReplies>repliesBefore&&coins()==saleBefore+20000L));
    }

    @Override public void run() {
        boolean purchaseSent=false;
        try {
            for(Class<?> cls:inst.getAllLoadedClasses()) if(cls.getName().equals("GameScreen.GameScreen")&&cls.getField("player").get(null)!=null){game=cls;break;}
            if(game==null)throw new IllegalStateException("No live game");
            ClassLoader loader=game.getClassLoader(); canvas=Class.forName("Main.GameCanvas",false,loader);
            messageClass=Class.forName("net.Message",false,loader);
            session=call(Class.forName("CLib.Session_ME",false,loader).getMethod("gI"),null);
            player=game.getField("player").get(null);map=canvas.getField("loadmap").get(null);
            area=((Number)Class.forName("Thread_More.LoadMap",false,loader).getField("Area").get(null)).intValue();
            x=number(player,"x");y=number(player,"y");coinBefore=coins();
            if(!expectedName.equals(field(player,"name")) || coinBefore!=expectedBalance || number(player,"typePk")!=11)
                throw new IllegalStateException("Account or balance differs from the user's observed purchase state");
            guard();
            if(testGrahamSale) {
                Object caravan=ownedCaravan();
                if(caravan==null || Math.abs(number(caravan,"x")-x)>=75 || Math.abs(number(caravan,"y")-y)>=75)
                    throw new IllegalStateException("No living owned transport nearby before purchase");
                caravanId=number(caravan,"ID");
                log("TRANSPORT id="+caravanId+" template="+number(caravan,"catalogyMonster")+" owner="+field(caravan,"nameowner"));
            }
            Class<?> bridge=Class.forName("com.silverknight.HsoBridge",false,loader);
            if((Boolean)call(bridge.getMethod("isRemoteSellBusy"),null))throw new IllegalStateException("Sale already active");
            original=field(session,"messageHandler");
            Class<?> handler=Class.forName("net.IMessageHandler",false,loader);
            wrapper=Proxy.newProxyInstance(loader,new Class<?>[]{handler},this::receive);
            session.getClass().getField("messageHandler").set(session,wrapper);
            log("BEGIN player="+field(player,"name")+" map=19 area="+area+" local="+x+","+y+" coin="+coinBefore);
            packet(23,new byte[]{(byte)-57});
            if(!menuReady.await(6500,TimeUnit.MILLISECONDS))throw new IllegalStateException("No server menu");
            ByteArrayOutputStream select=new ByteArrayOutputStream();DataOutputStream out=new DataOutputStream(select);
            out.writeShort(-57);out.writeByte(menuId);out.writeByte(option);packet(-30,select.toByteArray());
            if(!shopReady.await(6500,TimeUnit.MILLISECONDS))throw new IllegalStateException("No actual server shop");
            guard();Object item=findSaphia();int id=number(item,"Id");long price=((Number)field(item,"priceItem")).longValue();
            if(price!=10000L || number(item,"priceType")!=0 || coins()!=coinBefore || canvas.getField("currentDialog").get(null)!=null)
                throw new IllegalStateException("Price/currency/balance/dialog changed; do not purchase");
            log("VALIDATED Saphia id="+id+" category="+number(item,"ItemCatagory")+" typebuy=1 quantity=1 price="+price);
            String stockBefore=stockNotice;int repliesBefore=inventoryReplies;
            ByteArrayOutputStream buy=new ByteArrayOutputStream();out=new DataOutputStream(buy);
            out.writeByte(1);out.writeShort(id);out.writeShort(1);
            purchaseSent=true;packet(24,buy.toByteArray());
            long until=System.currentTimeMillis()+18000;
            while(System.currentTimeMillis()<until){
                guard();
                if(testGrahamSale && inventoryReplies>repliesBefore && coins()==coinBefore-10000L)break;
                if(inventoryReplies>repliesBefore && coins()==coinBefore-10000L && !stockBefore.equals(stockNotice))break;
                Thread.sleep(100);
            }
            log("RESULT purchase_sent_once=true coin="+coinBefore+"->"+coins()+" inventory_replies="+(inventoryReplies-repliesBefore)+" stock_before="+stockBefore+" stock_after="+stockNotice);
            if(testGrahamSale) {
                if(inventoryReplies<=repliesBefore || coins()!=coinBefore-10000L)throw new IllegalStateException("Purchase not confirmed; do not send sale confirmation");
                remoteGrahamSale();
            }
        }catch(Throwable error){log("ABORT purchase_sent="+purchaseSent+" error="+error);}
        finally {
            try {
                if(wrapper!=null && field(session,"messageHandler")==wrapper)session.getClass().getField("messageHandler").set(session,original);
                log("END hook_restored; purchase_attempts="+(purchaseSent?1:0)+" sale_confirmations="+(saleConfirmationSent?1:0)+"; no movement");
            }catch(Throwable error){log("RESTORE error="+error);}
        }
    }
}
