/*
 * Decompiled with CFR 0.152.
 */
package GameObjects;

import Skill.Monster_Skill;

public class Object_Effect_Skill {
    public short ID;
    public byte tem;
    public int hpShow;
    public int hpLast;
    public int hpPlus;
    public int[] mEffTypePlus = new int[0];
    public int[] mEff_HP_Plus = new int[0];
    public Monster_Skill skillMonster;

    public Object_Effect_Skill(short Id, byte tem) {
        this.ID = Id;
        this.tem = tem;
    }

    public Object_Effect_Skill(short Id, byte tem, Monster_Skill skill) {
        this.ID = Id;
        this.tem = tem;
        this.skillMonster = skill;
    }

    public void setHP(int hpShow, int hpLast) {
        this.hpShow = hpShow;
        this.hpLast = hpLast;
    }
}

