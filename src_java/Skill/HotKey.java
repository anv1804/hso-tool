/*
 * Decompiled with CFR 0.152.
 */
package Skill;

public class HotKey {
    public short id;
    public short idIconPotion;
    public byte type;
    public byte typePotion;
    public static byte NULL = (byte)-1;
    public static byte SKILL = 0;
    public static byte POTION = 1;

    public void setHotKey(int id, int type, byte typePotion) {
        this.id = (short)id;
        this.type = (byte)type;
        this.typePotion = typePotion;
    }

    public void setIdIcon(short id) {
        this.idIconPotion = id;
    }
}

