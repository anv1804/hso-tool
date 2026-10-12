/*
 * Decompiled with CFR 0.152.
 */
package GameObjects;

import CLib.mGraphics;
import CLib.mSystem;
import GameObjects.MainMonster;
import GameObjects.ObjectData;
import Model.CRes;
import Model.MainImage;
import netcommand.global.GlobalService;

public class MonsterBox
extends MainMonster {
    public MonsterBox(int ID, int Monster, int typeMonster, String name, int x, int y, int maxHP, int lv) {
        this.typeObject = 1;
        this.typeMonster = typeMonster;
        this.ID = ID;
        this.catalogyMonster = Monster;
        this.xAnchor = x;
        this.yAnchor = y;
        this.x = x;
        this.y = y;
        this.name = name;
        this.maxHp = maxHP;
        this.hp = maxHP;
        this.Lv = (byte)lv;
        this.MonWater = 1;
        this.Direction = 0;
        this.nFrame = 5;
        this.hOne = -1;
        this.wOne = -1;
        this.vMax = 3;
        this.limitMove = 60;
        this.timeAutoAction = CRes.random(50, 70);
        this.limitAttack = 50;
        this.xsai = 0;
        this.ysai = -2;
        this.timeLoadInfo = mSystem.currentTimeMillis();
    }

    public void updateAction() {
    }

    public void update() {
        long l;
        long l2;
        super.update();
        if (!this.isInfo && (l2 = (l = mSystem.currentTimeMillis()) - this.timeLoadInfo) >= 5000L) {
            this.timeLoadInfo = l;
            GlobalService.gI().monster_info((short)this.ID);
        }
        this.setDie();
        this.updateAction();
    }

    public boolean isItemBox() {
        return true;
    }

    public void paint(mGraphics g) {
        if (this.isDie) {
            return;
        }
        MainImage mainImage = ObjectData.getImagePartMonster((short)this.catalogyMonster);
        if (mainImage != null && mainImage.img != null) {
            g.drawImage(mainImage.img, this.x, this.y, 33, false);
        }
    }

    public void move_to_XY() {
    }

    public void move_to_XY_Normal() {
    }

    public void moveX() {
    }

    public void moveY() {
    }
}

