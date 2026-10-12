/*
 * Decompiled with CFR 0.152.
 */
package GameObjects;

import CLib.mFont;
import CLib.mGraphics;
import GameObjects.Item;
import GameObjects.MainInfoItem;
import GameObjects.MainObject;
import GameObjects.Player;
import Main.GameCanvas;
import Model.CRes;
import Model.T;
import Skill.HotKey;

public class MainItem
extends Item {
    public static final int FOOT = 8;
    public static final int ATB_FOOT_SNOW = 67;

    public MainItem(int ID, int IconId, String name, String content, int numPotion, int typeMainItem, long price, byte typePotion, byte moneytype, byte issell, byte istrade) {
        this.Id = ID;
        this.imageId = IconId;
        this.itemName = name;
        this.typePotion = typePotion;
        this.numPotion = numPotion;
        this.ItemCatagory = typeMainItem;
        this.priceItem = price;
        this.priceType = moneytype;
        this.canSell = issell;
        this.canTrade = istrade;
        this.mMoreContent(content);
        this.IndexSort = 0;
    }

    public MainItem clonePotion() {
        MainItem mainItem = new MainItem();
        mainItem.Id = this.Id;
        mainItem.imageId = this.imageId;
        mainItem.itemName = this.itemName;
        mainItem.typePotion = this.typePotion;
        mainItem.numPotion = this.numPotion;
        mainItem.ItemCatagory = this.ItemCatagory;
        mainItem.priceItem = this.priceItem;
        mainItem.priceType = this.priceType;
        mainItem.canSell = this.priceType;
        mainItem.canTrade = this.priceType;
        if (this.content != null) {
            mainItem.mMoreContent(this.content);
        }
        this.IndexSort = 0;
        return mainItem;
    }

    public void mMoreContent(String str) {
        super.mMoreContent(str);
    }

    public MainItem() {
    }

    public MainItem clone() {
        MainItem mainItem = new MainItem();
        mainItem.itemNameExcludeLv = this.itemNameExcludeLv;
        mainItem.itemName = this.itemName;
        mainItem.imageId = this.imageId;
        mainItem.Id = this.Id;
        mainItem.tier = this.tier;
        mainItem.colorNameItem = this.colorNameItem;
        mainItem.classcharItem = this.classcharItem;
        mainItem.ItemCatagory = this.ItemCatagory;
        mainItem.mInfo = this.mInfo;
        mainItem.type_Only_Item = this.type_Only_Item;
        mainItem.IdTem = this.IdTem;
        mainItem.priceItem = this.priceItem;
        mainItem.LvItem = this.LvItem;
        mainItem.canSell = this.canSell;
        mainItem.canTrade = this.canTrade;
        mainItem.timeUse = this.timeUse;
        mainItem.isLock = this.isLock;
        mainItem.timeDem = this.timeDem;
        mainItem.numPotion = this.numPotion;
        mainItem.priceType = this.priceType;
        mainItem.mInfo = this.mInfo;
        mainItem.IndexSort = this.IndexSort;
        mainItem.infoHop = new String[]{"", ""};
        return mainItem;
    }

    public static MainItem MainItem_Item(int ID, String itemName, int imageId, byte plusItem, int colorNameItem, int charclass, int typeMain, MainInfoItem[] minfo, int typeOnly, boolean isTem, short idTem, long price, short LvItem, byte issell, byte istrade, int timeUse, byte typeMoney, byte isLock) {
        return new MainItem(ID, itemName, imageId, plusItem, colorNameItem, charclass, typeMain, minfo, typeOnly, isTem, idTem, price, LvItem, issell, istrade, timeUse, typeMoney, isLock);
    }

    public static MainItem MainItem_Item(int ID, String itemName, int imageId, byte plusItem, int colorNameItem, int charclass, int typeMain, MainInfoItem[] minfo, int typeOnly, boolean isTem, short idTem, long price, short LvItem, byte issell, byte istrade, int timeUse, byte priceType, byte isLock, int timeDefault, long currentTime) {
        return new MainItem(ID, itemName, imageId, plusItem, colorNameItem, charclass, typeMain, minfo, typeOnly, isTem, idTem, price, LvItem, issell, istrade, timeUse, priceType, isLock, timeDefault, currentTime);
    }

    public MainItem(int ID, String itemName, int imageId, byte tier, int colorNameItem, int charclass, int typeMain, MainInfoItem[] minfo, int typeOnly, boolean isTem, short idTem, long price, short level, byte canSell, byte canTrade, int timeUse, byte priceType, byte isLock, int timeDefault, long currentTime) {
        this.itemNameExcludeLv = itemName;
        this.itemName = itemName;
        this.imageId = imageId;
        this.Id = ID;
        this.tier = tier;
        if (this.tier > 0) {
            this.itemName = this.itemName + " +" + this.tier;
        }
        this.colorNameItem = colorNameItem;
        this.classcharItem = charclass;
        this.ItemCatagory = typeMain;
        this.mInfo = minfo;
        this.type_Only_Item = typeOnly;
        this.IdTem = idTem;
        this.priceItem = price;
        this.LvItem = level;
        this.canSell = canSell;
        this.canTrade = canTrade;
        this.timeUse = timeUse;
        this.isLock = isLock;
        this.timeDefaultItemFashion = timeDefault;
        this.currentTimeItemFashion = currentTime;
        this.timeDem = timeUse > 0 ? GameCanvas.timeNow : 0L;
        this.priceType = priceType;
        if (this.mInfo != null && this.mInfo.length > 0 || this.priceItem > 0L || this.timeDem > 0L) {
            if (this.mInfo != null && this.mInfo.length > 0) {
                minfo = CRes.selectionSort(minfo);
            }
            if (!isTem) {
                this.setContentItem();
            } else {
                this.setContentTem();
            }
        }
        this.IndexSort = (byte)10;
    }

    public MainItem(int ID, String itemName, int imageId, byte tier, int colorNameItem, int charclass, int typeMain, MainInfoItem[] minfo, int typeOnly, boolean isTem, short idTem, long price, short level, byte canSell, byte canTrade, int timeUse, byte typeMoney, byte isLock) {
        this.itemNameExcludeLv = itemName;
        this.itemName = itemName;
        this.imageId = imageId;
        this.Id = ID;
        this.tier = tier;
        if (this.tier > 0) {
            this.itemName = this.itemName + " +" + this.tier;
        }
        this.colorNameItem = colorNameItem;
        this.classcharItem = charclass;
        this.ItemCatagory = typeMain;
        this.mInfo = minfo;
        this.type_Only_Item = typeOnly;
        this.IdTem = idTem;
        this.priceItem = price;
        this.LvItem = level;
        this.canSell = canSell;
        this.canTrade = canTrade;
        this.timeUse = timeUse;
        this.isLock = isLock;
        this.timeDem = timeUse > 0 ? GameCanvas.timeNow : 0L;
        this.priceType = typeMoney;
        if (this.mInfo != null && this.mInfo.length > 0 || this.priceItem > 0L || this.timeDem > 0L) {
            if (this.mInfo != null && this.mInfo.length > 0) {
                minfo = CRes.selectionSort(minfo);
            }
            if (!isTem) {
                this.setContentItem();
            } else {
                this.setContentTem();
            }
        }
        this.IndexSort = (byte)10;
        this.infoHop = new String[]{"", ""};
    }

    public static MainItem MainItem_Toc(int ID, String itemName, int imageId, byte plusItem, int typeMain, MainInfoItem[] minfo, boolean isTem, short idTem, long price, byte priceType, byte issell, byte istrade) {
        return new MainItem(ID, itemName, imageId, plusItem, typeMain, minfo, isTem, idTem, price, priceType, issell, istrade);
    }

    public MainItem(int ID, String itemName, int imageId, byte typeMain, int price, MainInfoItem[] priceType, boolean content, short value, long typeItem, byte num, byte sell, byte trade) {
        this.itemName = itemName;
        this.imageId = imageId;
        this.Id = ID;
        this.tier = typeMain;
        this.ItemCatagory = price;
        this.mInfo = priceType;
        this.IdTem = value;
        this.priceItem = typeItem;
        this.priceType = num;
        this.canSell = sell;
        this.canTrade = trade;
        if (this.mInfo != null) {
            priceType = CRes.selectionSort(priceType);
            this.setContentHair();
        }
        this.IndexSort = 1;
    }

    public MainItem(int ID, long price) {
        this.Id = ID;
        this.priceItem = price;
        this.priceType = 1;
        this.ItemCatagory = 8;
        this.itemName = T.iconclan;
        this.mMoreContent(T.contentclan);
        this.IndexSort = 1;
    }

    public static MainItem MainItem_Material(int ID, String itemName, int imageId, int typeMain, long price, byte priceType, String content, int value, byte typeitem, short num, byte Sell, byte Trade) {
        return new MainItem(ID, itemName, imageId, typeMain, price, priceType, content, value, typeitem, num, Sell, Trade);
    }

    public MainItem cloneNguyenLieu() {
        MainItem mainItem = new MainItem();
        mainItem.itemName = this.itemName;
        mainItem.imageId = this.imageId;
        mainItem.Id = this.Id;
        mainItem.ItemCatagory = this.ItemCatagory;
        mainItem.priceItem = this.priceItem;
        mainItem.priceType = this.priceType;
        mainItem.value = this.value;
        mainItem.canSell = this.canSell;
        mainItem.canTrade = this.canTrade;
        mainItem.content = this.content;
        mainItem.typeMaterial = this.typeMaterial;
        if (this.content != null) {
            mainItem.mMoreContent(this.content);
        }
        mainItem.IndexSort = (byte)2;
        return mainItem;
    }

    public MainItem(int ID, String itemName, int imageId, int typeMain, long price, byte priceType, String content, int value, byte typeItem, short num, byte sell, byte trade) {
        this.itemName = itemName;
        this.imageId = imageId;
        this.Id = ID;
        this.ItemCatagory = typeMain;
        this.priceItem = price;
        this.priceType = priceType;
        this.value = value;
        this.numPotion = num;
        this.canSell = sell;
        this.canTrade = trade;
        this.content = content;
        this.typeMaterial = typeItem;
        if (content != null) {
            this.mMoreContent(content);
        }
        this.IndexSort = (byte)2;
    }

    public MainItem(int name, String idIcon, int numPo, String item_potion, byte lvup, byte color) {
        this.Id = name;
        this.imageId = name;
        this.itemName = idIcon;
        this.numPotion = numPo;
        this.ItemCatagory = 5;
        this.canSell = lvup;
        this.canTrade = color;
        this.mMoreContent(item_potion);
        this.IndexSort = (byte)3;
    }

    public MainItem(int ID, String name, int numPo, int idIcon, byte item_potion, String contentPotion, MainInfoItem[] minfo, byte colorname, byte charclass, short lvMin, byte lvup, byte issell, byte istrade) {
        this.Id = ID;
        this.itemName = name;
        this.numPotion = numPo;
        this.imageId = idIcon;
        this.ItemCatagory = item_potion;
        this.colorNameItem = colorname;
        this.mInfo = minfo;
        this.classcharItem = charclass;
        this.LvItem = lvMin;
        this.canSell = issell;
        this.canTrade = istrade;
        this.tier = lvup;
        if (this.tier > 0) {
            this.itemName = this.itemName + " +" + this.tier;
        }
        if (contentPotion != null) {
            this.mMoreContent(contentPotion);
        } else {
            this.setContentItem();
        }
        this.IndexSort = (byte)4;
    }

    public MainItem(String name, int idIcon, int numPo, byte item_potion, byte lvup, byte color) {
        this.colorNameItem = color;
        this.itemName = name;
        this.numPotion = numPo;
        this.imageId = idIcon;
        this.ItemCatagory = item_potion;
        this.tier = lvup;
        if (this.tier > 0) {
            this.itemName = this.itemName + " +" + this.tier;
        }
    }

    public void setContentHair() {
        int n;
        if (this.itemName == null) {
            this.itemName = null;
            return;
        }
        int n2 = this.mInfo.length;
        this.mcontent = new String[n2];
        this.mColor = new int[n2];
        int n3 = mFont.tahoma_7b_white.getWidth(this.itemName);
        if (n3 > this.sizeW - 10) {
            this.sizeW = n3 + 10;
        }
        for (n = 0; n < n2; ++n) {
            MainInfoItem mainInfoItem = this.mInfo[n];
            this.mcontent[n] = Item.nameInfoItem[mainInfoItem.id] + ": " + Item.getPercent(Item.isPercentInfoItem[mainInfoItem.id], mainInfoItem.value);
            this.mColor[n] = Item.colorInfoItem[mainInfoItem.id];
            n3 = mFont.tahoma_7_black.getWidth(this.mcontent[n]);
            if (n3 <= this.sizeW - 10) continue;
            this.sizeW = n3 + 10;
        }
        n = 0;
        if (++n > 0) {
            this.mPlusContent = new String[n];
            this.mPlusColor = new int[n];
            n = 0;
            if (this.priceItem > 0L) {
                this.mPlusContent[n] = T.price + ": " + this.priceItem + this.getPriceType();
                this.mPlusColor[n] = 2;
                ++n;
            } else {
                this.mPlusContent[n] = T.dasohuu;
                this.mPlusColor[n] = 2;
                ++n;
            }
        } else {
            this.mPlusContent = null;
            this.mPlusColor = null;
        }
    }

    public static String getDotNumber(long m) {
        String string = "" + m;
        int n = string.length() / 3;
        if (string.length() % 3 == 0) {
            --n;
        }
        for (int i = 0; i < n; ++i) {
            int n2 = string.length() - (i + 1) * 3 - i;
            string = string.substring(0, n2) + "." + string.substring(n2, string.length());
        }
        return string;
    }

    public static String getNopercent(int value) {
        if (value % 100 == 0) {
            return value / 100 + "";
        }
        if (value % 10 == 0) {
            return value / 100 + "." + value % 100 / 10;
        }
        return value / 100 + "." + value % 100 / 10 + "" + value % 10;
    }

    public void paint(mGraphics g) {
    }

    public void paintColorItem(mGraphics g, int x, int y, int w) {
    }

    public void paintItem(mGraphics g, int x, int y, int w, int lech, int numlech) {
        super.paintItem(g, x, y, w, lech, numlech);
    }

    public void paintItem_not_num(mGraphics g, int n, int n2, int n3, int n4, int n5) {
        super.paintItem_notnum(g, n, n2, n3, n4, n5);
    }

    public void paintItemNew(mGraphics g, int x, int y, int w, int lech, int numlech) {
        super.paintItemNew(g, x, y, w, lech, numlech);
    }

    public static void setAddHotKey(byte typePo, boolean isStop) {
        HotKey hotKey = null;
        if (typePo == 0) {
            hotKey = Player.mhotkey[Player.levelTab][4];
        } else if (typePo == 1) {
            hotKey = Player.mhotkey[Player.levelTab][3];
        }
        if (hotKey == null || hotKey.type != HotKey.NULL) {
            hotKey = new HotKey();
            hotKey.type = HotKey.NULL;
        }
        for (int i = 0; i < Item.VecInvetoryPlayer.size(); ++i) {
            MainItem mainItem = (MainItem)Item.VecInvetoryPlayer.elementAt(i);
            if (mainItem.ItemCatagory != 4 || mainItem.typePotion != typePo || !(hotKey.id < mainItem.Id && isStop || hotKey.id > mainItem.Id && !isStop) && hotKey.type != HotKey.NULL) continue;
            hotKey.setHotKey(mainItem.Id, HotKey.POTION, typePo);
        }
    }

    public void set_can_Sell(byte can_sell_for_other_player) {
        this.can_sell_for_other_player = can_sell_for_other_player;
    }

    public void setCanShell_notCanTrade(byte type) {
        this.CanShell_CanNotTrade = type;
    }

    public boolean isMaterialHopNguyenLieu() {
        return this.ItemCatagory == 7 && MainObject.isMaHopNguyenLieu(this.Id);
    }
}

