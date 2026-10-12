/*
 * Decompiled with CFR 0.152.
 */
package GameObjects;

import CLib.mGraphics;
import CLib.mImage;
import CLib.mVector;
import GameObjects.DataEffect;
import GameObjects.Item;
import GameObjects.MainInfoItem;
import GameObjects.MainObject;
import GameObjects.ObjectData;
import GameObjects.Pet;
import Main.GameCanvas;
import Model.CRes;
import Model.MainImage;

public class PetItem
extends Item {
    public byte petImageId;
    public byte nFrameImgPet;
    public int type;
    public short experience;
    public int age;
    public int frame;
    public short growpoint;
    public short maxgrow;
    public short spirit;
    public short health;
    public short agility;
    public short strength;
    public short maxtiemnang;
    public MainInfoItem petAttack;
    public short[] mvaluetiemnang;
    public boolean isPetTool;

    public PetItem() {
    }

    public PetItem(int ID, String itemName, int iconId, int itemNameColor, int classTypeEquipment, int catagory, MainInfoItem[] mainInfo, int type, short level, int petType, byte frameNumberImg, byte petImageId) {
        this.itemNameExcludeLv = itemName;
        this.itemName = itemName;
        this.imageId = iconId;
        this.Id = ID;
        this.tier = 0;
        if (this.tier > 0) {
            this.itemName = this.itemName + " +" + this.tier;
        }
        this.colorNameItem = itemNameColor;
        this.classcharItem = classTypeEquipment;
        this.ItemCatagory = catagory;
        this.mInfo = mainInfo;
        this.type_Only_Item = type;
        this.IdTem = (short)-1;
        this.priceItem = 0L;
        this.LvItem = level;
        this.canSell = 0;
        this.canTrade = 0;
        this.timeUse = 0;
        this.timeDem = this.timeUse > 0 ? GameCanvas.timeNow : 0L;
        this.priceType = 0;
        this.type = petType;
        if ((mVector)Pet.PET_DATA.get("" + petImageId) != null) {
            this.isPetTool = true;
        }
        this.nFrameImgPet = frameNumberImg;
        this.petImageId = petImageId;
        if (this.mInfo != null && this.mInfo.length > 0 || this.priceItem > 0L || this.timeDem > 0L) {
            if (this.mInfo != null && this.mInfo.length > 0) {
                mainInfo = CRes.selectionSort(mainInfo);
            }
            this.setContentItem();
        }
        this.IndexSort = (byte)10;
        if (this.mInfo != null && this.mInfo.length > 0) {
            for (int i = 0; i < this.mInfo.length; ++i) {
                MainInfoItem mainInfoItem = this.mInfo[i];
                if (mainInfoItem.id > 6 || mainInfoItem.value <= 0) continue;
                this.petAttack = mainInfoItem;
            }
        }
    }

    public void setInfoPetOld(int ID, String itemName, int iconId, int itemNameColor, int classTypeEquipment, int catagory, MainInfoItem[] mainInfo, int type, short level, int petType, byte frameNumberImg, byte petImageId) {
        this.itemNameExcludeLv = itemName;
        this.itemName = itemName;
        this.imageId = iconId;
        this.Id = ID;
        this.tier = 0;
        if (this.tier > 0) {
            this.itemName = this.itemName + " +" + this.tier;
        }
        this.colorNameItem = itemNameColor;
        this.classcharItem = classTypeEquipment;
        this.ItemCatagory = catagory;
        this.mInfo = mainInfo;
        this.type_Only_Item = type;
        this.IdTem = (short)-1;
        this.priceItem = 0L;
        this.LvItem = level;
        this.canSell = 0;
        this.canTrade = 0;
        this.timeUse = 0;
        this.timeDem = this.timeUse > 0 ? GameCanvas.timeNow : 0L;
        this.priceType = 0;
        this.type = petType;
        this.nFrameImgPet = frameNumberImg;
        this.petImageId = petImageId;
        if (this.mInfo != null && this.mInfo.length > 0 || this.priceItem > 0L || this.timeDem > 0L) {
            if (this.mInfo != null && this.mInfo.length > 0) {
                mainInfo = CRes.selectionSort(mainInfo);
            }
            this.setContentItem();
        }
        this.IndexSort = (byte)10;
        if (this.mInfo != null && this.mInfo.length > 0) {
            for (int i = 0; i < this.mInfo.length; ++i) {
                MainInfoItem mainInfoItem = this.mInfo[i];
                if (mainInfoItem.id > 6 || mainInfoItem.value <= 0) continue;
                this.petAttack = mainInfoItem;
            }
        }
    }

    public void setTimeUser(int timeDefault, long currentTime) {
        this.timeDefaultItemFashion = timeDefault;
        this.currentTimeItemFashion = currentTime;
    }

    public void setInfoPet(int age, short growPoint, short str, short agi, short hea, short spi, short maxgrow, short maxtiemnang, short experience) {
        this.age = age;
        this.growpoint = growPoint;
        this.maxgrow = maxgrow;
        this.strength = str;
        this.agility = agi;
        this.health = hea;
        this.spirit = spi;
        this.maxtiemnang = maxtiemnang;
        this.experience = experience;
        this.mvaluetiemnang = new short[4];
        this.mvaluetiemnang[0] = this.strength;
        this.mvaluetiemnang[1] = this.agility;
        this.mvaluetiemnang[2] = this.health;
        this.mvaluetiemnang[3] = this.spirit;
    }

    public void paintItem(mGraphics g, int x, int y, int w, int lech, int numlech) {
        super.paintItem(g, x, y, w, lech, numlech);
    }

    public void paintShowPet(mGraphics g, int x, int y, int w, int lech, int numlech, int animIdx) {
        MainImage mainImage = ObjectData.getImagePet(this.petImageId);
        if (this.isPetTool) {
            if (mainImage.img != null) {
                int n7 = 0;
                if (this.isFly()) {
                    n7 = 15;
                }
                g.drawImage(MainObject.shadow, x, y + 10, 3, true);
                this.paintPetTool(g, mainImage, x, y + n7);
                this.doChangeFrmaeBoss();
            }
        } else {
            if (this.type != 2) {
                g.drawImage(MainObject.shadow, x, y + 10, 3, true);
                y -= 10;
            }
            if (mainImage.img != null) {
                int n8 = mImage.getImageHeight(mainImage.img.image) / this.nFrameImgPet;
                int n9 = mImage.getImageWidth(mainImage.img.image) / 2;
                g.drawRegion(mainImage.img, n9 * (this.getframe(animIdx) / 3), n8 * (this.getframe(animIdx) % 3), n9, n8, 0, x, y + lech, 33, true);
            }
        }
    }

    public void doChangeFrmaeBoss() {
        mVector mVector2 = new mVector();
        mVector mVector3 = (mVector)Pet.PET_DATA.get("" + this.petImageId);
        if (mVector3 != null) {
            mVector2 = mVector3;
        }
        DataEffect dataEffect = null;
        if (mVector2.size() > 0) {
            dataEffect = (DataEffect)mVector2.elementAt(0);
            int n = 0;
            this.frame = (byte)((this.frame + 1) % dataEffect.getAnim((int)n, (int)0).frame.length);
        }
    }

    public void paintPetTool(mGraphics g, MainImage img, int x, int y) {
        mVector mVector2 = new mVector();
        mVector mVector3 = (mVector)Pet.PET_DATA.get("" + this.petImageId);
        if (mVector3 != null) {
            mVector2 = mVector3;
        }
        if (mVector2.size() == 0) {
            return;
        }
        try {
            DataEffect dataEffect = (DataEffect)mVector2.elementAt(0);
            if (dataEffect != null) {
                int n3 = 0;
                int n4 = 0;
                mVector mVector4 = (mVector)Pet.PET_DATA.get("" + this.petImageId);
                if (mVector4 != null && img != null && img.img != null) {
                    dataEffect.paintPet(g, dataEffect.getFrame(this.frame, n3, 0), x, y - n4, 0, img.img);
                }
            }
        }
        catch (Exception exception) {
            // empty catch block
        }
    }

    public int getframe(int animIdx) {
        switch (this.type) {
            case 0: {
                return Pet.mOwl[animIdx][0][GameCanvas.gameTick % Pet.mOwl[animIdx][0].length];
            }
            case 1: {
                return Pet.mBat[animIdx][0][GameCanvas.gameTick % Pet.mBat[animIdx][0].length];
            }
            case 2: {
                return Pet.mWoftAnimFrame[animIdx][0][GameCanvas.gameTick % Pet.mWoftAnimFrame[animIdx][0].length];
            }
            case 3: {
                return Pet.mEagle[animIdx][0][GameCanvas.gameTick % Pet.mEagle[animIdx][0].length];
            }
        }
        return Pet.mElfAnimFrame[animIdx][0][GameCanvas.gameTick % Pet.mElfAnimFrame[animIdx][0].length];
    }

    public boolean isFly() {
        DataEffect dataEffect;
        mVector mVector2 = new mVector();
        mVector mVector3 = (mVector)Pet.PET_DATA.get("" + this.petImageId);
        if (mVector3 != null) {
            mVector2 = mVector3;
        }
        return (dataEffect = (DataEffect)mVector2.elementAt(0)) != null && dataEffect.isFly <= -5;
    }
}

