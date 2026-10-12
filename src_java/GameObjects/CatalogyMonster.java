/*
 * Decompiled with CFR 0.152.
 */
package GameObjects;

public class CatalogyMonster {
    int id;
    int lv;
    int maxHp;
    int typeMove;
    public String name;

    public CatalogyMonster(int id, int lv, int maxHp, int typeMove, String name) {
        this.id = id;
        this.lv = lv;
        this.maxHp = maxHp;
        this.typeMove = typeMove;
        this.name = name;
    }
}

