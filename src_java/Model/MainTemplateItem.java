/*
 * Decompiled with CFR 0.152.
 */
package Model;

import CLib.mHashtable;
import CLib.mVector;
import GameObjects.Item;
import netcommand.global.GlobalService;

public class MainTemplateItem {
    public int ID;
    public int IconId;
    public String name;
    int timenull;
    public static boolean isload = true;
    public int typeItem;
    public int idPartItem;
    public int classItem;
    public int[] mValueItem;
    public byte[] mValueName;
    public int sellPotion;
    public int valuePotion;
    public byte moneyType;
    public long PricePoition;
    public byte typePotion;
    public String contentPotion;
    public byte typeSell;
    public byte typeTrade;
    public static final byte TYPE_AO = 0;
    public static final byte TYPE_QUAN = 1;
    public static final byte TYPE_NON = 2;
    public static final byte TYPE_GANGTAY = 3;
    public static final byte TYPE_NHAN = 4;
    public static final byte TYPE_DAYCHUYEN = 5;
    public static final byte TYPE_GIAY = 6;
    public static final byte TYPE_CANH = 7;
    public static final byte TYPE_VUKHI1 = 8;
    public static final byte TYPE_VUKHI2 = 9;
    public static final byte TYPE_VUKHI3 = 10;
    public static final byte TYPE_VUKHI4 = 11;
    public static final byte TYPE_PET = 14;
    public static final byte I_VUKHI = 0;
    public static final byte I_AO = 1;
    public static final byte I_GANGTAY = 2;
    public static final byte I_NHAN1 = 3;
    public static final byte I_DAYCHUYEN = 4;
    public static final byte I_PET = 5;
    public static final byte I_NON = 6;
    public static final byte I_QUAN = 7;
    public static final byte I_GIAY = 8;
    public static final byte I_NHAN2 = 9;
    public static final byte I_CANH = 10;
    public static final byte I_HEAD = 12;
    public static final byte I_EYE = 13;
    public static final byte I_HAIR = 14;
    boolean canTrade = false;
    public static int[] mItem_Equip_Tem = new int[]{-1, 0, 3, 4, 5, 12, 2, 1, 6, 4, 7, -2, -2, -2, -2, -2, -2, -2, -2, -2, -2, -2, -2, -2, -2};
    public static int[] mItem_Rotate_Tem_Equip = new int[]{1, 7, 6, 2, -1, 4, 8, 10, 0, 0, 0, 0, 5};
    public static mHashtable hashPotionTem = new mHashtable();
    public static mHashtable hashMaterialTem = new mHashtable();
    public static mHashtable hashMaterialTem2 = new mHashtable();
    public static mHashtable hashItemTem = new mHashtable();
    public static mHashtable hashPetTem = new mHashtable();

    public MainTemplateItem() {
    }

    public MainTemplateItem(short ID, String name, byte typeItem, byte idPartItem, byte classitem, short iconId, int[] mvalue, byte[] mvaluename) {
        this.ID = ID;
        this.IconId = iconId;
        this.name = name;
        this.typeItem = typeItem;
        this.idPartItem = idPartItem;
        this.classItem = classitem;
        this.mValueItem = mvalue;
        this.mValueName = mvaluename;
    }

    public MainTemplateItem(short ID, short IconId, long price, String name, String content, byte typepotion, byte moneytype, byte sell, short value, boolean canTrade) {
        this.ID = ID;
        this.IconId = IconId;
        this.PricePoition = price;
        this.name = name;
        this.contentPotion = content;
        this.typePotion = typepotion;
        this.moneyType = moneytype;
        this.sellPotion = sell;
        this.valuePotion = value;
        this.canTrade = canTrade;
    }

    public static void removeUpdateItemInventory(int type) {
        for (int i = 0; i < Item.VecInvetoryPlayer.size(); ++i) {
            Item item = (Item)Item.VecInvetoryPlayer.elementAt(i);
            if (item.ItemCatagory != type) continue;
            Item.VecInvetoryPlayer.removeElement(item);
            --i;
        }
    }

    public static void removeUpdateItemChest(int type) {
        for (int i = 0; i < Item.VecChestPlayer.size(); ++i) {
            Item item = (Item)Item.VecChestPlayer.elementAt(i);
            if (item.ItemCatagory != type) continue;
            Item.VecInvetoryPlayer.removeElement(item);
            --i;
        }
    }

    public static void removeUpdateItemVec(int type, mVector vec) {
        for (int i = 0; i < vec.size(); ++i) {
            Item item = (Item)vec.elementAt(i);
            if (item.ItemCatagory != type) continue;
            vec.removeElement(item);
            --i;
        }
    }

    public static MainTemplateItem getItemTem(short id) {
        MainTemplateItem mainTemplateItem = (MainTemplateItem)hashItemTem.get("" + id);
        if (mainTemplateItem == null) {
            mainTemplateItem = new MainTemplateItem();
            hashItemTem.put("" + id, mainTemplateItem);
            GlobalService.gI().getItemTem(id);
        }
        if (mainTemplateItem.name == null) {
            ++mainTemplateItem.timenull;
            if (mainTemplateItem.timenull >= 200) {
                GlobalService.gI().getItemTem(id);
                mainTemplateItem.timenull = 0;
            }
        }
        return mainTemplateItem;
    }

    public static MainTemplateItem getPotionTem(short id) {
        MainTemplateItem mainTemplateItem = (MainTemplateItem)hashPotionTem.get("" + id);
        return mainTemplateItem;
    }
}

