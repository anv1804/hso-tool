package com.silverknight;

import CLib.Session_ME;
import CLib.mVector;
import GameScreen.GameScreen;
import GameScreen.TabScreenNew;
import InterfaceComponents.MainTabNew;
import InterfaceComponents.iCommand;
import Main.GameCanvas;
import GameObjects.Item;
import net.Message;
import java.io.*;
import java.lang.reflect.Field;

/** The native Graham menu remains available when its remote menu request is ignored. */
public final class GrahamNpcMenu {
    public static final int NPC_ID = -58;
    private static int menuId;
    private static String title = "Graham";
    // These exact entries and their order were received from this server on 13/10/2026.
    private static String[] options = {"Mua lạc đà", "Bán đá quý", "Đồ thương nhân", "Hướng dẫn thương nhân"};
    private static final TabScreenNew[] shops = new TabScreenNew[32];
    private static final int[] shopOwners = new int[32];
    private static int pendingOption = -1, pendingOwner = -1;
    private static long pendingTime;

    static { java.util.Arrays.fill(shopOwners, -1); }

    private GrahamNpcMenu() {}

    public static synchronized void remember(int npcId, int id, String[] labels, String name) {
        if (npcId != NPC_ID || labels == null || labels.length == 0 || labels.length > 32) return;
        String[] copy = labels.clone();
        for (String label : copy) if (label == null) return;
        if (!java.util.Arrays.equals(options, copy)) {
            java.util.Arrays.fill(shops, null);
            java.util.Arrays.fill(shopOwners, -1);
            pendingOption = -1;
        }
        menuId = id;
        options = copy;
        title = name == null || name.length() == 0 ? "Graham" : name;
    }

    public static synchronized String[] options() { return options.clone(); }
    public static synchronized int menuId() { return menuId; }

    public static synchronized boolean isShopOption(int option) {
        return option >= 0 && option < options.length
                && ("Đồ thương nhân".equalsIgnoreCase(options[option]) || "Mua lạc đà".equalsIgnoreCase(options[option]));
    }

    private static File shopFile(int option) {
        return new File("npc_shop_cache", "Mua lạc đà".equalsIgnoreCase(options[option])
                ? "graham-shop-camel-v2.bin" : "graham-shop-equipment-v2.bin");
    }

    private static int merchantTab(TabScreenNew screen) {
        if (screen == null) return -1;
        for (int i = 0; i < screen.VecTabScreen.size(); ++i) {
            Object tab = screen.VecTabScreen.elementAt(i);
            if (tab instanceof MainTabNew && "Đồ thương nhân".equalsIgnoreCase(((MainTabNew)tab).nameTab)) return i;
        }
        return -1;
    }

    public static synchronized boolean rememberMerchantShop() {
        if (GameScreen.player == null || pendingOwner != GameScreen.player.ID
                || System.currentTimeMillis() - pendingTime > 10000L) return false;
        return rememberMerchantShop(pendingOption);
    }

    public static synchronized boolean rememberMerchantShop(int option) {
        if (!isShopOption(option)) return false;
        if (GameScreen.player == null || merchantTab(GameCanvas.shopNpc) < 0) return false;
        shops[option] = GameCanvas.shopNpc;
        shopOwners[option] = GameScreen.player.ID;
        HsoBridge.logDebug("Graham shop retained from server data: owner=" + shopOwners[option] + " option=" + option);
        saveMerchantCatalog(option);
        return true;
    }

    private static void saveMerchantCatalog(int option) {
        try {
            TabScreenNew merchantShop = shops[option];
            Object tab = merchantShop.VecTabScreen.elementAt(merchantTab(merchantShop));
            Field field = tab.getClass().getDeclaredField("vecShop"); field.setAccessible(true);
            mVector items = (mVector)field.get(tab);
            if (items.size() == 0 || items.size() > 128) return;
            ByteArrayOutputStream bytes = new ByteArrayOutputStream();
            DataOutputStream data = new DataOutputStream(bytes);
            data.writeUTF("Đồ thương nhân"); data.writeByte(0); data.writeShort(items.size());
            for (int i = 0; i < items.size(); ++i) {
                Object value = items.elementAt(i);
                if (!(value instanceof Item) || ((Item)value).ItemCatagory != 4) return;
                data.writeShort(((Item)value).Id);
            }
            File file = shopFile(option);
            if (!file.getParentFile().isDirectory() && !file.getParentFile().mkdirs()) return;
            try (FileOutputStream output = new FileOutputStream(file)) { output.write(bytes.toByteArray()); }
        } catch (Exception error) { HsoBridge.logDebug("Graham shop cache: " + error); }
    }

    public static boolean validMerchantCatalog(byte[] bytes) {
        if (bytes == null || bytes.length > 4096) return false;
        try {
            DataInputStream data = new DataInputStream(new ByteArrayInputStream(bytes));
            if (!"Đồ thương nhân".equals(data.readUTF()) || data.readByte() != 0) return false;
            int count = data.readUnsignedShort();
            if (count < 1 || count > 128 || data.available() != count * 2) return false;
            while (count-- > 0) if (data.readShort() < 0) return false;
            return true;
        } catch (IOException error) { return false; }
    }

    private static boolean restoreMerchantCatalog(int option) {
        try {
            File file = shopFile(option);
            if (!file.isFile() && "Đồ thương nhân".equalsIgnoreCase(options[option])) {
                // Migrate the earlier display-only build's server-provided merchant
                // catalog. A fresh response to this option will replace it.
                file = new File("npc_shop_cache", "graham-merchant-shop.bin");
                if (file.isFile()) HsoBridge.logDebug("Graham: reopening the earlier saved merchant catalog; awaiting server refresh");
            }
            long size = file.length();
            if (!file.isFile() || size <= 0 || size > 4096) return false;
            byte[] bytes = new byte[(int)size];
            try (DataInputStream input = new DataInputStream(new FileInputStream(file))) { input.readFully(bytes); }
            if (!validMerchantCatalog(bytes)) return false;
            GameCanvas.readMessenge.npcInfo(new Message((byte)23, bytes));
            return rememberMerchantShop(option);
        } catch (Exception error) {
            HsoBridge.logDebug("Graham saved catalog unavailable: " + error);
            return false;
        }
    }

    public static synchronized boolean showSubmenu(int npcId, int option) {
        if (npcId != NPC_ID || !isShopOption(option)) return false;
        if (GameScreen.player == null || !Session_ME.gI().isConnected() || HsoBridge.isRemoteSellBusy()
                || GameCanvas.currentDialog != null) return false;
        pendingOption = option; pendingOwner = GameScreen.player.ID; pendingTime = System.currentTimeMillis();
        if ((GameScreen.player.ID != shopOwners[option] || shops[option] == null) && !restoreMerchantCatalog(option)) return false;
        TabScreenNew merchantShop = shops[option];
        int tab = merchantTab(merchantShop);
        if (tab < 0) return false;
        merchantShop.selectTab = tab;
        GameCanvas.shopNpc = merchantShop;
        GameCanvas.menu2.doCloseMenu();
        merchantShop.Show(GameCanvas.game);
        HsoBridge.logDebug("Graham cached shop displayed: owner=" + shopOwners[option] + " option=" + option + " title=Đồ thương nhân; server purchase still required");
        return GameCanvas.currentScreen == merchantShop;
    }

    public static synchronized boolean show() {
        if (GameScreen.player == null || !Session_ME.gI().isConnected()
                || GameCanvas.menu2 == null || HsoBridge.isRemoteSellBusy()) return false;
        mVector commands = new mVector("Graham NPC menu");
        for (String label : options) commands.addElement(new iCommand(label, null));
        // setinfoDynamic retains the NPC/menu IDs. A user's selection follows the
        // original Dynamic_Menu packet path, with its actual option index.
        GameCanvas.menu2.setinfoDynamic(commands, 2, menuId, NPC_ID, title);
        HsoBridge.logDebug("Graham menu displayed locally: npc=" + NPC_ID + " menu=" + menuId);
        return GameCanvas.menu2.isShowMenu;
    }
}
