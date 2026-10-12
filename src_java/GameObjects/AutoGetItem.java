/*
 * Decompiled with CFR 0.152.
 */
package GameObjects;

public class AutoGetItem {
    byte valueColorItem;
    byte isGetMoney;
    byte isGetPotion;
    public static byte POI_NHAT_HET = 0;
    public static byte POI_NHAT_MAU = 1;
    public static byte POI_NHAT_NANG_LUONG = (byte)2;
    public static byte POI_KHONG_NHAT = (byte)3;
    public boolean isremove = false;

    public AutoGetItem(byte valuecolor, byte potion, byte money) {
        this.valueColorItem = valuecolor;
        this.isGetMoney = money;
        this.isGetPotion = potion;
    }
}

