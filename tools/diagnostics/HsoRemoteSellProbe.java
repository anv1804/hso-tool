import java.io.*;
import java.lang.instrument.Instrumentation;
import java.lang.reflect.*;
import java.lang.invoke.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.*;

/** Temporary, bounded live diagnostics. No login, purchase, discard or map-change packets. */
public final class HsoRemoteSellProbe implements Runnable, InvocationHandler {
    private static volatile HsoRemoteSellProbe running;
    private final Instrumentation instrumentation;
    private final Path workspace;
    private final BlockingQueue<Event> events = new LinkedBlockingQueue<Event>();
    private volatile boolean active;
    private volatile int requestedNpc = Integer.MAX_VALUE;
    private Object session, originalHandler, wrapper, player, map;
    private Object screenBefore, shopBefore;
    private Class<?> bridge, gameScreen, gameCanvas, loadMap, playerClass, messageClass;
    private int playerId, mapId, area, startX, startY, virtualX, virtualY, width, height;
    private boolean autoAttack, autoPickup, autoSell;
    private byte nativeAutoFire;
    private boolean nativeCurrentAutoFire;
    private final ArrayList<int[]> gates = new ArrayList<int[]>();
    private long begun;
    private volatile int echoX = -1, echoY = -1;
    private volatile int shopType = -1;
    private volatile String saleName;
    private final int saleCategory = 4;
    private final String expectedSampleName = "HP lớn";
    private final boolean merchantShopOnly = true;
    private final boolean graham50VirtualOnly = true;
    private int saleId = -1; // Resolve the previously selected white ring from the live inventory.
    private int quantityBefore;
    private long coinBefore;
    private boolean success;
    private boolean restoring;
    private boolean scanDone;
    private int scanNext;
    private Field scanDoneField, scanNextField;

    private static final class Event {
        int cmd, npc, menu, dialog, dialogType, shop;
        String text;
        String[] options;
    }
    private static final class Merchant {
        final int id, x, y;
        final String name;
        Merchant(int id, int x, int y, String name) { this.id=id; this.x=x; this.y=y; this.name=name; }
    }

    private HsoRemoteSellProbe(Instrumentation inst, Path root) {
        instrumentation = inst;
        workspace = root;
    }

    public static synchronized void agentmain(String args, Instrumentation inst) {
        if (running != null && running.active) return;
        HsoRemoteSellProbe probe = new HsoRemoteSellProbe(inst, Paths.get(args).toAbsolutePath().normalize());
        running = probe;
        probe.log("AGENT_START workspace=" + args);
        Thread thread = new Thread(probe, "HSO bounded remote-shop diagnostic");
        thread.setDaemon(true);
        thread.start();
    }

    private static Object field(Object obj, String name) throws Exception {
        return obj.getClass().getField(name).get(obj);
    }
    private static int number(Object obj, String name) throws Exception { return ((Number)field(obj,name)).intValue(); }
    private static Object staticField(Class<?> cls, String name) throws Exception { return cls.getField(name).get(null); }
    private void bridgeSet(String name, Object value) throws Exception { bridge.getField(name).set(null, value); }

    // MicroEmulator's old MIDlet class loader cannot resolve JDK 17's generated
    // reflection accessors. Method handles avoid generating those game-loader classes.
    private static Object invokeMethod(Method method, Object receiver, Object... args) throws Exception {
        try {
            MethodHandle handle=MethodHandles.publicLookup().unreflect(method);
            if(!Modifier.isStatic(method.getModifiers()))handle=handle.bindTo(receiver);
            return handle.invokeWithArguments(args==null?new Object[0]:args);
        }catch(Throwable ex){if(ex instanceof Exception)throw (Exception)ex;throw new IllegalStateException(ex);}
    }
    private Object newMessage(int command) throws Exception {
        try {return MethodHandles.publicLookup().findConstructor(messageClass,
                MethodType.methodType(void.class,byte.class)).invokeWithArguments((byte)command);}
        catch(Throwable ex){if(ex instanceof Exception)throw (Exception)ex;throw new IllegalStateException(ex);}
    }

    private synchronized void log(String text) {
        System.setProperty("hso.remoteProbe.status", text);
        String line = new java.text.SimpleDateFormat("HH:mm:ss.SSS").format(new Date()) + " " + text + "\n";
        try (FileOutputStream output = new FileOutputStream(workspace.resolve("hso_remote_sell_probe.log").toFile(),true)) {
            output.write(line.getBytes(StandardCharsets.UTF_8));
        } catch (IOException ex) { System.setProperty("hso.remoteProbe.logError", ex.toString()); }
    }

    private void initialize() throws Exception {
        log("INITIALIZING");
        ClassLoader loader = null;
        for (Class<?> cls : instrumentation.getAllLoadedClasses()) {
            if (cls.getName().equals("GameScreen.GameScreen") && staticField(cls,"player") != null) {
                gameScreen = cls; loader = cls.getClassLoader(); break;
            }
        }
        if (loader == null) throw new IllegalStateException("No active game class loader");
        bridge = Class.forName("com.silverknight.HsoBridge", false, loader);
        gameCanvas = Class.forName("Main.GameCanvas", false, loader);
        loadMap = Class.forName("Thread_More.LoadMap", false, loader);
        playerClass = Class.forName("GameObjects.Player", false, loader);
        messageClass = Class.forName("net.Message", false, loader);
        Class<?> sessionClass = Class.forName("CLib.Session_ME", false, loader);
        session = invokeMethod(sessionClass.getMethod("gI"),null);
        if (!(Boolean)invokeMethod(sessionClass.getMethod("isConnected"),session)) throw new IllegalStateException("Not connected");
        if ((Boolean)invokeMethod(bridge.getMethod("isRemoteSellBusy"),null)
                || (Boolean)staticField(bridge,"isStrikeActive")) throw new IllegalStateException("Existing remote transaction active");
        player = staticField(gameScreen,"player"); map = staticField(gameCanvas,"loadmap");
        playerId = number(player,"ID"); mapId = number(map,"idMap");
        area = ((Number)staticField(loadMap,"Area")).intValue();
        startX = virtualX = number(player,"x"); startY = virtualY = number(player,"y");
        width = number(map,"mapW") * ((Number)staticField(loadMap,"wTile")).intValue();
        height = number(map,"mapH") * ((Number)staticField(loadMap,"wTile")).intValue();
        Object points = staticField(loadMap,"vecPointChange");
        int count = (Integer)invokeMethod(points.getClass().getMethod("size"),points);
        for (int i=0; i<count; ++i) {
            Object point = invokeMethod(points.getClass().getMethod("elementAt",int.class),points,i);
            if (point != null) gates.add(new int[]{number(point,"x"),number(point,"y")});
        }
        Object item = merchantShopOnly ? null : inventoryItem();
        if (!merchantShopOnly && (item == null || number(item,"canSell") != 1 || number(item,"isLock") != 0
                || number(item,"numPotion") != 1 || number(item,"priceItem") > 10
                || !expectedSampleName.equals(field(item,"itemName"))))
            throw new IllegalStateException("The diagnostic requires exactly one low-cost HP lớn potion");
        saleName = merchantShopOnly ? "" : (String)field(item,"itemName");
        if(!merchantShopOnly)saleId = number(item,"Id");
        quantityBefore = quantity(); coinBefore = coins();
        autoAttack = (Boolean)staticField(bridge,"isAutoAttack");
        autoPickup = (Boolean)staticField(bridge,"isAutoPickup");
        autoSell = (Boolean)staticField(bridge,"isAutoSell");
        nativeAutoFire = ((Number)staticField(playerClass,"isAutoFire")).byteValue();
        nativeCurrentAutoFire = (Boolean)staticField(playerClass,"isCurAutoFire");
        screenBefore=staticField(gameCanvas,"currentScreen");shopBefore=staticField(gameCanvas,"shopNpc");
        scanDoneField=bridge.getDeclaredField("monsterScanDone");scanDoneField.setAccessible(true);
        scanNextField=bridge.getDeclaredField("monsterScanNextId");scanNextField.setAccessible(true);
        scanDone=scanDoneField.getBoolean(null);scanNext=scanNextField.getInt(null);
        originalHandler = field(session,"messageHandler");
        Class<?> handler = Class.forName("net.IMessageHandler", false, loader);
        wrapper = Proxy.newProxyInstance(loader, new Class<?>[]{handler}, this);
        active = true; begun = System.currentTimeMillis();
        guard();
        sessionClass.getField("messageHandler").set(session, wrapper);
        log("BEGIN player=" + playerId + " map=" + mapId + " area=" + area
                + " local=("+startX+","+startY+") sample="+saleName+" qty="+quantityBefore+" coin="+coinBefore);
    }

    private Object inventoryItem() throws Exception {
        Class<?> itemClass = Class.forName("GameObjects.Item", false, gameScreen.getClassLoader());
        Object inventory = staticField(itemClass,"VecInvetoryPlayer");
        int count=(Integer)invokeMethod(inventory.getClass().getMethod("size"),inventory);
        for (int i=0; i<count; ++i) {
            Object item=invokeMethod(inventory.getClass().getMethod("elementAt",int.class),inventory,i);
            if (item != null && number(item,"ItemCatagory")==saleCategory
                    && (saleId>=0 ? number(item,"Id")==saleId
                    : expectedSampleName.equals(field(item,"itemName")))) return item;
        }
        return null;
    }
    private int quantity() throws Exception {
        if(merchantShopOnly)return 0;
        Object item=inventoryItem();
        if(item!=null && saleName!=null && !saleName.equals(field(item,"itemName")))
            throw new IllegalStateException("Sample inventory ID was reused for another item; cancel diagnostic");
        return item==null ? 0 : saleCategory==3 ? 1 : number(item,"numPotion");
    }
    private long coins() throws Exception { return ((Number)field(player,"coin")).longValue(); }

    private void guard() throws Exception {
        if (!active) return;
        if (!(Boolean)invokeMethod(session.getClass().getMethod("isConnected"),session)
                || staticField(gameScreen,"player") != player || staticField(gameCanvas,"loadmap") != map
                || number(map,"idMap") != mapId || ((Number)staticField(loadMap,"Area")).intValue()!=area)
            throw new IllegalStateException("Game context changed; no cross-map restoration will be sent");
        if (!restoring && (number(player,"hp") <= 0 || number(player,"x") != startX || number(player,"y") != startY))
            throw new IllegalStateException("Player moved or died; diagnostic cancelled");
        if(!restoring)quantity();
        if (!restoring && System.currentTimeMillis()-begun > 360000L) throw new IllegalStateException("Diagnostic time limit");
        bridgeSet("isAutoAttack",false); bridgeSet("isAutoPickup",false); bridgeSet("isAutoSell",false);
        bridgeSet("remoteOrigX",startX); bridgeSet("remoteOrigY",startY);
        bridgeSet("lastRemoteMoveTime",System.currentTimeMillis()); bridgeSet("isStrikeActive",true);
        scanDoneField.setBoolean(null,false);scanNextField.setInt(null,-1);
        playerClass.getField("isAutoFire").set(null,(byte)-1);
        playerClass.getField("isCurAutoFire").set(null,false);
        player.getClass().getField("posTransRoad").set(player,null);
        boolean[] keys=(boolean[])staticField(gameCanvas,"keyMyPressed"); keys[25]=false; keys[5]=false;
    }

    @Override public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        if (method.getName().equals("onMessage") && active) {
            try {
                Object msg=args[0]; int cmd=((Number)field(msg,"command")).intValue();
                DataInputStream input=(DataInputStream)invokeMethod(msg.getClass().getMethod("reader"),msg);
                int n=input.available(); input.mark(n+1); byte[] bytes=new byte[n]; input.readFully(bytes); input.reset();
                Event event=decode(cmd,bytes);
                if (event!=null) {
                    events.offer(event);
                    if (cmd==-30 && event.npc==requestedNpc && !merchantShopOnly) return null;
                    if (cmd==5 && event.npc==playerId) return null;
                    // This round deliberately runs the native shop parser and opens its real UI.
                    if (cmd==-32 && sampleDialog(event)) return null;
                }
            } catch (Exception ex) { log("CAPTURE ERROR " + ex); }
        }
        try { return invokeMethod(method,originalHandler,args); }
        catch (InvocationTargetException ex) { throw ex.getCause(); }
    }

    private Event decode(int cmd, byte[] bytes) throws IOException {
        DataInputStream in=new DataInputStream(new ByteArrayInputStream(bytes)); Event e=new Event(); e.cmd=cmd;
        if (cmd==-30) {
            e.npc=in.readShort(); e.menu=in.readByte(); int count=in.readUnsignedByte();
            if (count>32) return null;
            e.options=new String[count]; for(int i=0;i<count;i++) e.options[i]=in.readUTF();
            e.text=in.readUTF();
            log("RX MENU npc="+e.npc+" menu="+e.menu+" options="+Arrays.toString(e.options)); return e;
        } else if (cmd==5) {
            int id=in.readShort();in.readUTF();int x=in.readShort(),y=in.readShort();
            if(id==playerId){echoX=x;echoY=y;log("RX AUDITED_SELF_POSITION ("+x+","+y+")");e.npc=id;return e;}
        } else if (cmd==-44) {
            int id=in.readShort();String name=in.readUTF();in.readInt();in.readInt();in.readShort();
            int x=in.readShort(),y=in.readShort();log("RX NPC_INFO id="+id+" name="+name+" position=("+x+","+y+")");return e;
        } else if (cmd==23) {
            e.text=in.readUTF(); e.shop=in.readByte();
            if (e.shop==0 || e.shop==1 || e.shop==2 || e.shop==4 || e.shop==8 || e.shop==13 || e.shop==100) {
                shopType=e.shop; log("RX SHOP type="+e.shop+" title="+e.text+" bytes="+bytes.length); return e;
            }
            return null;
        } else if (cmd==-32) {
            e.dialog=in.readShort(); e.dialogType=in.readByte(); e.text=in.readUTF();
            log("RX DIALOG id="+e.dialog+" type="+e.dialogType+" text="+e.text); return e;
        } else if (cmd==16) {
            log("RX INVENTORY bytes="+bytes.length); return e;
        } else if (cmd==4) {
            while(in.available()>=10) {
                int type=in.readByte(); in.readShort(); int id=in.readShort(); int x=in.readShort(),y=in.readShort(); in.readByte();
                if(type==0 && id==playerId) { echoX=x;echoY=y;log("RX SELF_POSITION ("+x+","+y+")"); }
            }
        } else if (cmd==53) {
            e.text=in.readUTF();
            String s=e.text.toLowerCase(Locale.ROOT);
            if (s.contains("bán")||s.contains("cửa hàng")||s.contains("quá xa")||s.contains("khoảng cách")) {
                log("RX NOTICE "+e.text); return e;
            }
        }
        return null;
    }

    private void packet(int command, byte[] payload, String label) throws Exception {
        guard();
        Object message=newMessage(command);
        DataOutputStream writer=(DataOutputStream)invokeMethod(messageClass.getMethod("writer"),message);
        writer.write(payload);
        Class<?> sessionInterface=Class.forName("net.ISession",false,gameScreen.getClassLoader());
        invokeMethod(sessionInterface.getMethod("sendMessage",messageClass),session,message);
        if (command!=4) log("TX cmd="+command+" "+label+" bytes="+hex(payload));
    }
    private static String hex(byte[] b) {
        StringBuilder s=new StringBuilder(); for(byte x:b)s.append(String.format("%02x",x&255));return s.toString();
    }
    private void move(int x,int y) throws Exception {
        if(!safe(x,y))throw new IllegalStateException("Virtual position touches gate/boundary ("+x+","+y+")");
        ByteArrayOutputStream bytes=new ByteArrayOutputStream();DataOutputStream out=new DataOutputStream(bytes);
        out.writeShort(x);out.writeShort(y);packet(4,bytes.toByteArray(),"move");virtualX=x;virtualY=y;
    }
    private boolean safe(int x,int y) {
        if(x<12||y<12||x>=width-12||y>=height-12)return false;
        for(int[] gate:gates)if((x-gate[0])*(x-gate[0])+(y-gate[1])*(y-gate[1])<=48*48)return false;
        return true;
    }
    private boolean walkable(int x,int y) throws Exception {
        if(!safe(x,y))return false;
        int tile=(Integer)invokeMethod(map.getClass().getMethod("getTile",int.class,int.class),map,x,y);
        return tile!=1&&tile!=-1;
    }
    private void pause(long millis) throws Exception {
        long until=System.currentTimeMillis()+millis;
        while(System.currentTimeMillis()<until){guard();Thread.sleep(Math.min(40,Math.max(1,until-System.currentTimeMillis())));}
    }
    private Event await(int command,long timeout) throws Exception {
        long until=System.currentTimeMillis()+timeout;
        while(System.currentTimeMillis()<until){
            guard();Event e=events.poll(40,TimeUnit.MILLISECONDS);
            if(e!=null&&e.cmd==command&&(command!=-30||e.npc==requestedNpc))return e;
        }
        return null;
    }
    private void select(Event menu,int option) throws Exception {
        ByteArrayOutputStream bytes=new ByteArrayOutputStream();DataOutputStream out=new DataOutputStream(bytes);
        out.writeShort(menu.npc);out.writeByte(menu.menu);out.writeByte(option);
        packet(-30,bytes.toByteArray(),"npc="+menu.npc+" menu="+menu.menu+" option="+option+" label="+menu.options[option]);
    }
    private static boolean passiveShopChoice(String text) {
        String s=text.trim().toLowerCase(Locale.ROOT);
        return s.contains("mua bán")||s.contains("mua ban")||s.contains("cửa hàng")||s.equals("shop")
                ||s.equals("shop nguyên liệu")||s.equals("shop thức ăn")||s.equals("shop trứng")
                ||s.equals("chiến binh")||s.equals("sát thủ")||s.equals("pháp sư")||s.equals("xạ thủ")
                ||s.equals("vũ khí")||s.equals("áo")||s.equals("quần")||s.equals("găng tay")||s.equals("giày")
                ||s.equals("nhẫn")||s.equals("dây chuyền")||s.equals("trang bị");
    }
    private Event requestMenu(Merchant npc) throws Exception {
        events.clear();requestedNpc=npc.id;
        packet(23,new byte[]{(byte)npc.id},"getlist_from_npc("+npc.id+")");
        return await(-30,10000);
    }
    private boolean explore(Merchant npc,String strategy) throws Exception {
        log("CASE "+strategy+" npc="+npc.name+" id="+npc.id+" virtual=("+virtualX+","+virtualY+")");
        Event menu=requestMenu(npc);
        if(menu==null){log("RESULT no_menu");return false;}
        for(int option=0;option<menu.options.length;option++){
            if(!passiveShopChoice(menu.options[option]))continue;
            Event fresh=option==0?menu:requestMenu(npc);if(fresh==null)continue;
            if(fresh.options.length<=option||!passiveShopChoice(fresh.options[option]))continue;
            events.clear();shopType=-1;select(fresh,option);
            long until=System.currentTimeMillis()+6500;
            for(int depth=0;depth<3&&System.currentTimeMillis()<until;){
                guard();Event e=events.poll(40,TimeUnit.MILLISECONDS);if(e==null)continue;
                if(e.cmd==23){log("RESULT shop_opened "+strategy+" npc="+npc.name+" type="+e.shop);return trySale(strategy+"/"+npc.name);}
                if(e.cmd==-30&&e.npc==npc.id){
                    int choice=-1;for(int i=0;i<e.options.length;i++)if(passiveShopChoice(e.options[i])){choice=i;break;}
                    if(choice<0){log("RESULT no_passive_submenu "+Arrays.toString(e.options));break;}
                    select(e,choice);depth++;
                }
            }
            log("RESULT no_shop option="+option+" label="+fresh.options[option]+" last_self_echo=("+echoX+","+echoY+")");
        }
        return false;
    }
    private boolean trySale(String label) throws Exception {
        int before=quantity();if(before<=0)return true;
        events.clear();
        ByteArrayOutputStream bytes=new ByteArrayOutputStream();DataOutputStream out=new DataOutputStream(bytes);
        out.writeByte(saleCategory);out.writeShort(saleId);out.writeByte(1);
        packet(18,bytes.toByteArray(),"sell "+saleName+" via "+label);
        long until=System.currentTimeMillis()+4500;boolean inventory=false;
        while(System.currentTimeMillis()<until){
            guard();Event e=events.poll(40,TimeUnit.MILLISECONDS);
            if(e!=null&&e.cmd==16)inventory=true;
            confirmSampleDialog(e);
            int after=quantity();
            if(inventory&&after<before){log("SUCCESS "+label+" quantity="+before+"->"+after+" coin="+coinBefore+"->"+coins());success=true;return true;}
        }
        log("RESULT sale_unconfirmed "+label+" quantity="+quantity()+" coin="+coins());return false;
    }

    private boolean directConfirmationProbe() throws Exception {
        int before=quantity();
        if(before!=1)throw new IllegalStateException("Confirmation probe requires the single white ring");
        events.clear();
        ByteArrayOutputStream request=new ByteArrayOutputStream();DataOutputStream out=new DataOutputStream(request);
        out.writeByte(saleCategory);out.writeShort(saleId);out.writeByte(1);
        packet(18,request.toByteArray(),"explicit sample before synthetic confirmation");
        pause(450);
        Event pending;
        while((pending=events.poll())!=null){
            if(pending.cmd==-32 && !sampleDialog(pending))
                throw new IllegalStateException("Another dialog is pending; no synthetic confirmation will be sent");
            if(sampleDialog(pending)){
                confirmSampleDialog(pending);
                return waitConfirmationResult(before,"actual_sample_dialog");
            }
        }
        // Test the exact observed confirmation type (-7), without assuming it is -category.
        // Keep this experimental packet out of the production sale controller.
        ByteArrayOutputStream answer=new ByteArrayOutputStream();DataOutputStream confirm=new DataOutputStream(answer);
        confirm.writeShort(playerId);confirm.writeByte(-7);confirm.writeByte(1);
        log("CASE observed_confirmation_type_minus7 without server prompt: self="+playerId+" intended_sample_id="+saleId);
        packet(-32,answer.toByteArray(),"synthetic confirmation for explicit sample only");
        return waitConfirmationResult(before,"observed_confirmation_type_minus7");
    }

    private boolean waitConfirmationResult(int before,String label) throws Exception {
        boolean acknowledged=false;long until=System.currentTimeMillis()+4500;
        while(System.currentTimeMillis()<until){
            guard();Event e=events.poll(40,TimeUnit.MILLISECONDS);
            if(e!=null&&e.cmd==16)acknowledged=true;
            confirmSampleDialog(e);
            if(acknowledged&&quantity()<before){
                success=true;log("SUCCESS "+label+" quantity="+before+"->"+quantity()+" coin="+coinBefore+"->"+coins());return true;
            }
        }
        log("RESULT sale_unconfirmed "+label+" quantity="+quantity()+" coin="+coins());return false;
    }

    private boolean sampleDialog(Event e) {
        if(e==null || e.cmd!=-32 || e.text==null || saleName==null || saleName.length()==0)return false;
        String baseName=saleName.toLowerCase(Locale.ROOT).replace("[khoá]", "").replace("[khóa]", "").trim();
        String text=e.text.toLowerCase(Locale.ROOT);
        return text.contains(baseName) && (text.contains("bạn có muốn bán") || text.contains("ban co muon ban"));
    }

    private void confirmSampleDialog(Event e) throws Exception {
        if(!sampleDialog(e))return;
        ByteArrayOutputStream answer=new ByteArrayOutputStream();DataOutputStream output=new DataOutputStream(answer);
        output.writeShort(e.dialog);output.writeByte(e.dialogType);output.writeByte(1);
        packet(-32,answer.toByteArray(),"confirm sample sale");
    }

    private void auditPosition() throws Exception {
        events.clear();ByteArrayOutputStream bytes=new ByteArrayOutputStream();DataOutputStream out=new DataOutputStream(bytes);
        out.writeShort(playerId);packet(5,bytes.toByteArray(),"char_info(self)");
        await(5,2500);
    }

    private Object construct(Class<?> cls, Class<?>[] types, Object... args) throws Exception {
        try{return MethodHandles.publicLookup().findConstructor(cls,MethodType.methodType(void.class,types)).invokeWithArguments(args);}
        catch(Throwable ex){if(ex instanceof Exception)throw (Exception)ex;throw new IllegalStateException(ex);}
    }

    private boolean nativeModalSale() throws Exception {
        ClassLoader loader=gameScreen.getClassLoader();
        Class<?> vectorClass=Class.forName("CLib.mVector",false,loader);
        Class<?> tabClass=Class.forName("InterfaceComponents.TabShopNew",false,loader);
        Class<?> screenClass=Class.forName("GameScreen.TabScreenNew",false,loader);
        Class<?> mainScreenClass=Class.forName("GameScreen.MainScreen",false,loader);
        Class<?> itemClass=Class.forName("GameObjects.Item",false,loader);
        Object inventory=staticField(itemClass,"VecInvetoryPlayer");
        Object previousScreen=staticField(gameCanvas,"currentScreen"), previousShop=staticField(gameCanvas,"shopNpc");
        Object tab=construct(tabClass,new Class<?>[]{vectorClass,byte.class,String.class,int.class,byte.class},
                inventory,(byte)0,"Hành trang - kiểm tra bán",-1,(byte)0);
        Object tabs=construct(vectorClass,new Class<?>[]{});
        invokeMethod(vectorClass.getMethod("addElement",Object.class),tabs,tab);
        Object view=construct(screenClass,new Class<?>[]{});
        invokeMethod(screenClass.getMethod("addMoreTab",vectorClass),view,tabs);
        gameCanvas.getField("shopNpc").set(null,view);
        int before=quantity();events.clear();
        try{
            invokeMethod(screenClass.getMethod("Show",mainScreenClass),view,previousScreen);
            int count=(Integer)invokeMethod(vectorClass.getMethod("size"),inventory);
            for(int i=0;i<count;i++){
                Object item=invokeMethod(vectorClass.getMethod("elementAt",int.class),inventory,i);
                if(item!=null&&number(item,"Id")==saleId&&number(item,"ItemCatagory")==saleCategory){
                    Field select=tabClass.getDeclaredField("idSelect");select.setAccessible(true);select.setInt(tab,i);
                    log("CASE native_shop_modal: invoke original TabShopNew.commandPointer(10,0) for "+saleName);
                    invokeMethod(tabClass.getMethod("commandPointer",int.class,int.class),tab,10,0);
                    boolean inventoryAck=false;long until=System.currentTimeMillis()+4500;
                    while(System.currentTimeMillis()<until){
                        guard();Event e=events.poll(40,TimeUnit.MILLISECONDS);if(e!=null&&e.cmd==16)inventoryAck=true;
                        confirmSampleDialog(e);
                        if(inventoryAck&&quantity()<before){success=true;log("SUCCESS native_shop_modal qty="+before+"->"+quantity()+" coin="+coins());return true;}
                    }
                    log("RESULT native_shop_modal sale_unconfirmed qty="+quantity()+" coin="+coins());return false;
                }
            }
            return false;
        }finally{
            gameCanvas.getField("currentScreen").set(null,previousScreen);
            gameCanvas.getField("shopNpc").set(null,previousShop);
        }
    }

    private boolean nativeActualShopSale(String label) throws Exception {
        pause(250); // Let the collector finish building the shop tabs from the real reply.
        Object view=staticField(gameCanvas,"shopNpc");
        if(view==null || view==shopBefore || staticField(gameCanvas,"currentScreen")!=view)
            throw new IllegalStateException("The real shop reply did not create the native shop screen");
        Object tabs=field(view,"VecTabScreen");
        int count=(Integer)invokeMethod(tabs.getClass().getMethod("size"),tabs);
        Object inventory=staticField(Class.forName("GameObjects.Item",false,gameScreen.getClassLoader()),"VecInvetoryPlayer");
        int items=(Integer)invokeMethod(inventory.getClass().getMethod("size"),inventory);
        int slot=-1;
        for(int i=0;i<items;i++){
            Object item=invokeMethod(inventory.getClass().getMethod("elementAt",int.class),inventory,i);
            if(item!=null && number(item,"ItemCatagory")==saleCategory && number(item,"Id")==saleId){slot=i;break;}
        }
        if(slot<0)return false;
        for(int i=0;i<count;i++){
            Object tab=invokeMethod(tabs.getClass().getMethod("elementAt",int.class),tabs,i);
            if(!tab.getClass().getName().equals("InterfaceComponents.TabShopNew"))continue;
            Field type=tab.getClass().getField("typeTab");
            if(type.getByte(tab)!=0)continue;
            Field selected=tab.getClass().getDeclaredField("idSelect");selected.setAccessible(true);selected.setInt(tab,slot);
            view.getClass().getField("selectTab").setInt(view,i);
            int before=quantity();events.clear();
            log("CASE native_actual_shop_sale "+label+" sample="+saleName+" id="+saleId+" inventory_slot="+slot);
            invokeMethod(tab.getClass().getMethod("commandPointer",int.class,int.class),tab,10,0);
            return waitConfirmationResult(before,label+"/native_actual_shop");
        }
        throw new IllegalStateException("The native shop has no normal inventory tab");
    }

    private boolean petShopRound(int option) throws Exception {
        Merchant pet=new Merchant(-42,startX,startY,"pet_shop");
        Event menu=requestMenu(pet);
        if(menu==null || menu.options.length<=option || !passiveShopChoice(menu.options[option]))return false;
        events.clear();select(menu,option);
        Event shop=await(23,6500);
        if(shop==null){log("RESULT no_pet_shop option="+option);return false;}
        if(nativeActualShopSale("pet_shop_option_"+option))return true;
        return trySale("after_real_shop_parser/option_"+option);
    }

    private void openMerchantShop() throws Exception {
        log("CASE merchant_shop_display_only: no sale or purchase packets");
        Merchant dylan=new Merchant(-57,startX,startY,"Mr Dylan");
        Event menu=requestMenu(dylan);
        if(menu!=null){
            for(int i=0;i<menu.options.length;i++){
                String label=menu.options[i].trim().toLowerCase(Locale.ROOT);
                if(label.equals("mua bán") || label.equals("mua ban") || label.equals("mua đá quý")){
                    events.clear();select(menu,i);
                    Event shop=await(23,6500);
                    pause(300);
                    if(shop!=null){
                        log("MERCHANT_SHOP_OPEN title="+shop.text+" type="+shop.shop+" native_screen="+staticField(gameCanvas,"currentScreen").getClass().getName()+" coin="+coinBefore+"->"+coins());
                        return;
                    }
                    log("RESULT merchant shop not returned after "+label);
                    break;
                }
            }
        } else log("RESULT no Mr Dylan menu");
        Merchant graham=new Merchant(-58,startX,startY,"Graham");
        Event grahamMenu=requestMenu(graham);
        if(grahamMenu!=null){
            log("MERCHANT_MENU_SHOWN Graham options="+Arrays.toString(grahamMenu.options)+" coin="+coinBefore+"->"+coins());
        } else log("RESULT no Graham menu");
    }

    private void virtualGraham50() throws Exception {
        if(mapId!=19 || !"lronic".equals(field(player,"name")) || coins()!=216885L)
            throw new IllegalStateException("The current one-Saphia trial context changed");
        Object values=staticField(gameScreen,"Vecplayers");
        int count=(Integer)invokeMethod(values.getClass().getMethod("size"),values);
        Object bull=null;
        for(int i=0;i<count;i++) {
            Object value=invokeMethod(values.getClass().getMethod("elementAt",int.class),values,i);
            if(value!=null && value.getClass().getName().equals("GameObjects.Monstervantieu")
                    && "lronic".equals(field(value,"nameowner")) && number(value,"ID")==8299)bull=value;
        }
        if(bull==null || number(bull,"hp")<=0)throw new IllegalStateException("Expected purchased cargo's bull is unavailable");
        int[][] points={{150,804},{156,792},{168,792},{144,792},{192,792},{144,768}};
        int[] target=null;
        for(int[] point:points)if(walkable(point[0],point[1])){target=point;break;}
        if(target==null)throw new IllegalStateException("No valid current-map virtual endpoint near Graham's observed coordinates");
        log("CASE type50_virtual_Graham: no purchase; map="+mapId+" local="+startX+","+startY+" endpoint="+target[0]+","+target[1]+" actor_id="+playerId+" bull=8299");
        // Packet-only hypothesis: collision/pathfinding on the current client map
        // cannot establish whether the server accepts Graham's coordinates.
        // safe() still excludes boundaries and change-map gates at every step.
        moveTo(target[0],target[1],24,60,false);
        pause(2500);
        log("VIRTUAL_ENDPOINT bull_client_position="+number(bull,"x")+","+number(bull,"y")+" last_server_self_echo="+echoX+","+echoY+" local="+number(player,"x")+","+number(player,"y"));
        events.clear();requestedNpc=-58;
        packet(23,new byte[]{(byte)-58},"Graham root at virtual endpoint");
        pause(200);
        packet(-30,new byte[]{(byte)0xFF,(byte)0xC6,0,1},"Graham sell gems menu0/index1 at virtual endpoint");
        Event dialog=await(-32,300);
        int id=playerId;
        if(dialog!=null) {
            if(dialog.dialogType!=50 || dialog.dialog!=playerId || !dialog.text.contains("20000 Vàng"))
                throw new IllegalStateException("Unexpected server dialog; no sale confirmation");
            id=dialog.dialog;
        }
        ByteArrayOutputStream bytes=new ByteArrayOutputStream();DataOutputStream out=new DataOutputStream(bytes);
        out.writeShort(id);out.writeByte(50);out.writeByte(1);
        packet(-32,bytes.toByteArray(),"one user-authorized type50 sale confirmation; fresh_dialog="+(dialog!=null));
        boolean acknowledged=false;long until=System.currentTimeMillis()+8000;
        while(System.currentTimeMillis()<until) {
            guard();Event event=events.poll(40,TimeUnit.MILLISECONDS);
            if(event!=null&&event.cmd==16)acknowledged=true;
            if(acknowledged&&coins()==coinBefore+20000L){success=true;break;}
        }
        log("RESULT virtual_type50 credit_confirmed="+success+" inventory_ack="+acknowledged+" coin="+coinBefore+"->"+coins()+" bull_client_position="+number(bull,"x")+","+number(bull,"y"));
    }

    private List<int[]> route(int sx,int sy,int tx,int ty) throws Exception {
        int cols=width/24,rows=height/24,total=cols*rows;
        int start=(sy/24)*cols+sx/24,target=(ty/24)*cols+tx/24;
        int[] previous=new int[total];Arrays.fill(previous,-1);previous[start]=start;
        ArrayDeque<Integer> queue=new ArrayDeque<Integer>();queue.add(start);
        int[] dx={1,-1,0,0},dy={0,0,1,-1};
        while(!queue.isEmpty()&&previous[target]<0){
            int p=queue.remove();int x=p%cols,y=p/cols;
            for(int i=0;i<4;i++){
                int nx=x+dx[i],ny=y+dy[i];if(nx<0||ny<0||nx>=cols||ny>=rows)continue;
                int next=ny*cols+nx;if(previous[next]>=0||!walkable(nx*24+12,ny*24+12))continue;
                previous[next]=p;queue.add(next);
            }
        }
        if(previous[target]<0)return Collections.emptyList();
        ArrayList<int[]> result=new ArrayList<int[]>();
        for(int p=target;p!=start;p=previous[p])result.add(new int[]{(p%cols)*24+12,(p/cols)*24+12});
        Collections.reverse(result);result.add(new int[]{tx,ty});return result;
    }
    private void moveTo(int tx,int ty,int step,int interval,boolean routed) throws Exception {
        if(!safe(tx,ty))throw new IllegalArgumentException("Merchant endpoint outside current map or near a gate");
        if(routed&&!restoring&&echoX>=0&&safe(echoX,echoY)){
            // A rejected jump must not become the origin of the next paced attempt.
            virtualX=echoX;virtualY=echoY;echoX=echoY=-1;
            log("POSITION_ANCHOR reset to actual server correction ("+virtualX+","+virtualY+")");
        }
        List<int[]> path=routed?route(virtualX,virtualY,tx,ty):Arrays.asList(new int[]{tx,ty});
        if(path.isEmpty()&&routed){
            for(int radius=24;radius<=72&&path.isEmpty();radius+=24){
                int[][] offsets={{radius,0},{-radius,0},{0,radius},{0,-radius}};
                for(int[] offset:offsets){
                    int x=tx+offset[0],y=ty+offset[1];
                    if(!safe(x,y))continue;
                    List<int[]> candidate=route(virtualX,virtualY,x,y);
                    if(!candidate.isEmpty()){path=candidate;tx=x;ty=y;break;}
                }
            }
        }
        if(path.isEmpty())throw new IllegalArgumentException("No virtual route to merchant");
        int sent=0;
        for(int[] target:path){
            while(virtualX!=target[0]||virtualY!=target[1]){
                int dx=target[0]-virtualX,dy=target[1]-virtualY;
                double distance=Math.sqrt((double)dx*dx+(double)dy*dy);
                int x=distance<=step?target[0]:virtualX+(int)Math.round(dx*step/distance);
                int y=distance<=step?target[1]:virtualY+(int)Math.round(dy*step/distance);
                move(x,y);sent++;pause(interval);
            }
        }
        log("TX POSITION completed endpoint=("+tx+","+ty+") steps="+sent+" step="+step+" interval="+interval+" routed="+routed);
        pause(250);
    }

    private Merchant[] localNpcMerchants() throws Exception {
        ArrayList<Merchant> result=new ArrayList<Merchant>();
        Object objects=staticField(gameScreen,"Vecplayers");
        int count=(Integer)invokeMethod(objects.getClass().getMethod("size"),objects);
        for(int i=0;i<count;i++){
            Object npc=invokeMethod(objects.getClass().getMethod("elementAt",int.class),objects,i);
            if(npc==null)continue;
            String type=npc.getClass().getName();
            if(!type.equals("GameObjects.NPC")&&!type.equals("GameObjects.NPCserver"))continue;
            int id=type.endsWith("NPCserver")?number(npc,"idImage"):number(npc,"ID");
            String name=(String)field(npc,"name");
            log("LOCAL_NPC id="+id+" objectId="+number(npc,"ID")+" name="+name+" position=("+number(npc,"x")+","+number(npc,"y")+") greeting="+field(npc,"nameGiaotiep"));
            if(!name.equalsIgnoreCase("Khu"))result.add(new Merchant(id,number(npc,"x"),number(npc,"y")+24,name));
        }
        return result.toArray(new Merchant[result.size()]);
    }

    @Override public void run() {
        try {
            initialize();
            if(graham50VirtualOnly){virtualGraham50();return;}
            if(merchantShopOnly){openMerchantShop();return;}
            if (mapId == 1) throw new IllegalStateException("Cross-map diagnostic requires the distant farming map, not the village");
            if(trySale("one_unlocked_potion_direct"))return;
            if(petShopRound(1))return;
            if(petShopRound(2))return;
            log("NO_CONFIRMED_SALE after bounded protocol-supported merchant/menu/position variants");
        } catch(Throwable ex){log("ABORT "+ex);}
        finally {
            if(originalHandler!=null){
                try{
                    if(staticField(gameScreen,"player")==player&&staticField(gameCanvas,"loadmap")==map
                            &&number(map,"idMap")==mapId&&((Number)staticField(loadMap,"Area")).intValue()==area){
                        // First restore the original valid endpoint; no local coordinates are assigned.
                        int x=number(player,"x"),y=number(player,"y");
                        restoring=true;
                        if(active&&safe(x,y)){
                            try{moveTo(x,y,graham50VirtualOnly?24:12,graham50VirtualOnly?60:40,!graham50VirtualOnly);}catch(Exception ex){
                                log("RESTORE direct endpoint: "+ex);move(x,y);
                            }
                        }
                        pause(2000);
                    }
                    log("END success="+success+" sample_qty="+quantity()+" coin="+coins()
                            +" local=("+number(player,"x")+","+number(player,"y")+") map="+number(map,"idMap"));
                }catch(Exception ex){log("CLEANUP ERROR "+ex);}
                finally {
                    try {
                        Object currentShop=staticField(gameCanvas,"shopNpc");
                        if(!merchantShopOnly && currentShop!=shopBefore && staticField(gameCanvas,"currentScreen")==currentShop)
                            gameCanvas.getField("currentScreen").set(null,screenBefore);
                        if(!merchantShopOnly)gameCanvas.getField("shopNpc").set(null,shopBefore);
                    }catch(Exception ex){log("SHOP UI RESTORE ERROR "+ex);}
                    try { if(field(session,"messageHandler")==wrapper)
                        session.getClass().getField("messageHandler").set(session,originalHandler);
                    } catch(Exception ex){log("HANDLER RESTORE ERROR "+ex);}
                    String[] names={"isAutoAttack","isAutoPickup","isAutoSell","isStrikeActive","lastRemoteMoveTime","remoteOrigX","remoteOrigY"};
                    Object[] values={autoAttack,autoPickup,autoSell,false,0L,-1,-1};
                    for(int i=0;i<names.length;i++)try{bridgeSet(names[i],values[i]);}catch(Exception ex){log("FLAG RESTORE ERROR "+names[i]+": "+ex);}
                    try{scanDoneField.setBoolean(null,scanDone);scanNextField.setInt(null,scanNext);}
                    catch(Exception ex){log("SCANNER RESTORE ERROR "+ex);}
                    try{playerClass.getField("isAutoFire").set(null,nativeAutoFire);
                        playerClass.getField("isCurAutoFire").set(null,nativeCurrentAutoFire);}
                    catch(Exception ex){log("NATIVE AUTO RESTORE ERROR "+ex);}
                }
            }
            active=false;
        }
    }
}
