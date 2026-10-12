/*
 * Decompiled with CFR 0.152.
 */
package GameObjects;

import CLib.mGraphics;
import CLib.mImage;
import GameObjects.Item;
import GameObjects.MainObject;
import GameObjects.ObjectData;
import GameScreen.PaintInfoGameScreen;
import Main.GameCanvas;
import Model.CRes;
import Model.MainImage;

public class Item_Drop
extends MainObject {
    long timeAp;
    int timeRemove;
    int timefly;

    public Item_Drop(int ID, byte type, String name, int x, int y, short IdIcon, byte color) {
        this.ID = ID;
        this.typeObject = type;
        this.name = name;
        if (x < 48) {
            x = 48;
        }
        if (x > GameCanvas.loadmap.maxWMap - 48) {
            x = GameCanvas.loadmap.maxWMap - 48;
        }
        if (y < 48) {
            y = 48;
        }
        if (y > GameCanvas.loadmap.maxHMap - 48) {
            y = GameCanvas.loadmap.maxHMap - 48;
        }
        this.x = x;
        this.y = y;
        this.imageId = IdIcon;
        this.colorName = color;
        this.vx = CRes.random_Am(1, 5);
        this.vy = -CRes.random(3, 10);
        this.vMax = 16;
        this.time = CRes.random(3, 9);
        this.timeAp = GameCanvas.timeNow;
        this.timeRemove = 60;
        this.isSend = false;
    }

    public void paint(mGraphics g) {
        MainImage mainImage = null;
        switch (this.typeObject) {
            case 3: {
                mainImage = ObjectData.getImageItem((short)this.imageId);
                break;
            }
            case 4: {
                mainImage = ObjectData.getImagePotion((short)this.imageId);
                break;
            }
            case 5: {
                mainImage = ObjectData.getImageQuestItem((short)this.imageId);
                break;
            }
            case 7: {
                mainImage = ObjectData.getImageMaterial((short)this.imageId);
            }
        }
        if (mainImage.img != null) {
            if (this.hOne == 0) {
                this.hOne = mImage.getImageHeight(mainImage.img.image);
            }
            g.drawImage(mainImage.img, this.x, this.y, 33, false);
            if (this.typeObject == 3 && this.colorName > 1) {
                Item.fraeffitemdrop.drawFrame((this.colorName - 1) * 3 + GameCanvas.gameTick / 3 % 3, this.x + 6, this.y - 14, 0, 3, g);
            }
        }
        if (this.isWater) {
            g.drawRegion(MainObject.water, 0, GameCanvas.gameTick / 2 % 2 * 17, 28, 17, 0, this.x, this.y - 2 + this.dyWater, 3, false);
        }
        if (PaintInfoGameScreen.isLevelPoint) {
            byte by = 0;
            if (this.typeObject == 3) {
                by = this.colorName;
            }
            this.paintName(g, by);
        }
    }

    public void paintAvatarFocus(mGraphics g, int xp, int yp) {
        MainImage mainImage = null;
        switch (this.typeObject) {
            case 3: {
                mainImage = ObjectData.getImageItem((short)this.imageId);
                break;
            }
            case 4: {
                mainImage = ObjectData.getImagePotion((short)this.imageId);
                break;
            }
            case 5: {
                mainImage = ObjectData.getImageQuestItem((short)this.imageId);
                break;
            }
            case 7: {
                mainImage = ObjectData.getImageMaterial((short)this.imageId);
            }
        }
        if (mainImage.img != null) {
            if (this.hOne == 0) {
                this.hOne = mImage.getImageHeight(mainImage.img.image);
            }
            g.drawImage(mainImage.img, xp - 1, yp, 3, false);
        }
    }

    public void update() {
        if (this.time > 0) {
            this.x += this.vx;
            this.y += this.vy;
            this.vy += 2;
            --this.time;
        }
        if (this.time == 0) {
            int n = GameCanvas.loadmap.getTile(this.x, this.y);
            if (n == 2) {
                this.isWater = true;
            }
            this.isRunAttack = false;
            this.time = -1;
        }
        if (this.isRunAttack) {
            ++this.timefly;
            this.x += this.vx;
            this.y += this.vy;
            if (this.timefly >= this.timeHuyKill) {
                this.isRemove = true;
                this.isRunAttack = false;
            }
        }
        if (this.isSend) {
            ++this.timeGet;
            if (this.timeGet > 40) {
                this.isSend = false;
                this.timeGet = 0;
            }
        }
        if ((GameCanvas.timeNow - this.timeAp) / 1000L >= (long)this.timeRemove) {
            this.isRemove = true;
        }
    }
}

