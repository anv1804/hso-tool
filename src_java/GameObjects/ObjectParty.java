/*
 * Decompiled with CFR 0.152.
 */
package GameObjects;

public class ObjectParty {
    public short Lv;
    public String name;
    public byte idArea;
    public int idMap;
    public int x;
    public int y;
    public int hp;
    public int maxhp;
    public boolean isRemove = false;
    public boolean ischeck = true;

    public ObjectParty(String name, short Lv, int idMap, byte idarea) {
        this.idArea = idarea;
        this.idMap = idMap;
        this.name = name;
        this.Lv = Lv;
    }
}

