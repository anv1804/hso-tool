/*
 * Decompiled with CFR 0.152.
 */
package GameObjects;

import CLib.mHashtable;
import CLib.mImage;
import CLib.mVector;
import GameObjects.Item;
import GameObjects.MainMonster;
import GameObjects.Pet;
import GameScreen.GameScreen;
import Main.GameCanvas;
import Model.CRes;
import Model.MainImage;
import Skill.Skill;
import java.util.Enumeration;
import netcommand.global.GlobalService;

public class ObjectData {
    public static final short ITEMMAP = 0;
    public static final short MONSTER = 1000;
    public static final short ITEM = 2000;
    public static final short NPC = 3000;
    public static final short POTION = 4000;
    public static final short QUEST_ITEM = 5000;
    public static final short MATERIAL = 5500;
    public static final short SKILL = 6000;
    public static final short ICON_CLAN = 7000;
    public static final short ICON_ARCHE_CLAN = 9500;
    public static final short CAPCHAR = 9999;
    public static final short ICON_PET = 10000;
    public static final short IMAGE_PET = 10200;
    public static final short IMAGE_TILE = 10400;
    public static final short IMAGE_TILE_WATER = 10500;
    public static final short IMAGE_TILE_SMALL = 10600;
    public static final short IMAGE_MOUNT = 10700;
    public static final short ITEM_2 = 13000;
    public static mVector vecSaveImage = new mVector("ObjectData vecSaveImage");

    public static MainImage getImagePartNPC(short id) {
        MainImage mainImage = (MainImage)GameScreen.HashImageNPC.get("" + id);
        if (mainImage == null) {
            mImage mImage2;
            mainImage = new MainImage();
            GameScreen.HashImageNPC.put("" + id, mainImage);
            String string = "/resLocal/npc/" + id + ".png";
            if (id >= 500) {
                int n = id - 500;
                string = "/resLocal/npc/icon/" + n + ".png";
            }
            mainImage.img = (mImage2 = mImage.createImage(string)) == null ? ObjectData.getFromRms((short)(id + 3000)) : mImage2;
        }
        if (mainImage.img == null) {
            ++mainImage.timeImageNull;
            if (mainImage.timeImageNull >= 200) {
                GlobalService.gI().load_image((short)(id + 3000));
                mainImage.timeImageNull = 0;
            }
        }
        return mainImage;
    }

    public static MainImage getImagePartItemMap(short id) {
        MainImage mainImage = (MainImage)GameScreen.HashImageItemMap.get("" + id);
        if (mainImage == null) {
            mainImage = new MainImage();
            GameScreen.HashImageItemMap.put("" + id, mainImage);
            mImage mImage2 = mImage.createImage("/resLocal/tree/" + id + ".png");
            mainImage.img = mImage2 == null ? ObjectData.getFromRms((short)(id + 0)) : mImage2;
        } else if (!GameCanvas.isTouch) {
            mainImage.count = (int)(GameCanvas.timeNow / 1000L);
        }
        if (mainImage.img == null) {
            ++mainImage.timeImageNull;
            if (mainImage.timeImageNull >= 200) {
                GlobalService.gI().load_image((short)(id + 0));
                mainImage.timeImageNull = 0;
            }
        }
        return mainImage;
    }

    public static MainImage getImagePartMonster(short id) {
        MainImage mainImage = (MainImage)MainMonster.HashImageMonster.get("" + id);
        if (mainImage == null) {
            mainImage = new MainImage();
            MainMonster.HashImageMonster.put("" + id, mainImage);
            mImage mImage2 = mImage.createImage("/resLocal/mons/" + id + ".png");
            mainImage.img = mImage2 == null ? ObjectData.getFromRms((short)(id + 1000)) : mImage2;
        } else if (!GameCanvas.isTouch) {
            mainImage.count = (int)(GameCanvas.timeNow / 1000L);
        }
        if (mainImage.img == null) {
            ++mainImage.timeImageNull;
            if (mainImage.timeImageNull >= 200) {
                GlobalService.gI().load_image((short)(id + 1000));
                mainImage.timeImageNull = 0;
            }
        }
        return mainImage;
    }

    public static MainImage getImageItem(short id) {
        MainImage mainImage = (MainImage)Item.HashImageItem.get("" + id);
        if (mainImage == null) {
            mainImage = new MainImage();
            Item.HashImageItem.put("" + id, mainImage);
            mImage mImage2 = mImage.createImage("/resLocal/icon/icon_" + id + ".png");
            mainImage.img = mImage2 == null ? ObjectData.getFromRms((short)(id + 2000)) : mImage2;
        }
        if (mainImage.img == null) {
            ++mainImage.timeImageNull;
            if (mainImage.timeImageNull >= 200) {
                GlobalService.gI().load_image((short)(id + 2000));
                mainImage.timeImageNull = 0;
            }
        }
        return mainImage;
    }

    public static MainImage getImagePotion(short id) {
        MainImage mainImage = (MainImage)Item.HashImagePotion.get("" + id);
        if (mainImage == null) {
            mainImage = new MainImage();
            Item.HashImagePotion.put("" + id, mainImage);
            String string = "/resLocal/potion/potionIcon_" + (id + 1 >= 10 ? id + 1 + "" : "0" + (id + 1)) + ".png";
            mImage mImage2 = mImage.createImage(string);
            mainImage.img = mImage2 == null ? ObjectData.getFromRms((short)(id + 4000)) : mImage2;
        }
        if (mainImage.img == null) {
            ++mainImage.timeImageNull;
            if (mainImage.timeImageNull >= 200) {
                GlobalService.gI().load_image((short)(id + 4000));
                mainImage.timeImageNull = 0;
            }
        }
        return mainImage;
    }

    public static MainImage getImageQuestItem(short id) {
        MainImage mainImage = (MainImage)Item.HashImageQuestItem.get("" + id);
        if (mainImage == null) {
            mainImage = new MainImage();
            Item.HashImageQuestItem.put("" + id, mainImage);
            mImage mImage2 = mImage.createImage("/resLocal/iconq/" + id + ".png");
            mainImage.img = mImage2 == null ? ObjectData.getFromRms((short)(id + 5000)) : mImage2;
        }
        if (mainImage.img == null) {
            ++mainImage.timeImageNull;
            if (mainImage.timeImageNull >= 200) {
                GlobalService.gI().load_image((short)(id + 5000));
                mainImage.timeImageNull = 0;
            }
        }
        return mainImage;
    }

    public static MainImage getImageMaterial(short id) {
        MainImage mainImage = (MainImage)Item.HashImageMaterial.get("" + id);
        if (mainImage == null) {
            mainImage = new MainImage();
            Item.HashImageMaterial.put("" + id, mainImage);
            mImage mImage2 = mImage.createImage("/resLocal/material/" + id + ".png");
            mainImage.img = mImage2 == null ? ObjectData.getFromRms((short)(id + 5500)) : mImage2;
        }
        if (mainImage.img == null) {
            ++mainImage.timeImageNull;
            if (mainImage.timeImageNull >= 200) {
                GlobalService.gI().load_image((short)(id + 5500));
                mainImage.timeImageNull = 0;
            }
        }
        return mainImage;
    }

    public static MainImage getImageSkill(short id) {
        MainImage mainImage = (MainImage)Skill.hashImageSkill.get("" + id);
        if (mainImage == null) {
            mainImage = new MainImage();
            Skill.hashImageSkill.put("" + id, mainImage);
            String string = id < 10 ? "/resLocal/skill/iconSkill_0" + id + ".png" : "/resLocal/skill/iconSkill_" + id + ".png";
            mImage mImage2 = mImage.createImage(string);
            mainImage.img = mImage2 == null ? ObjectData.getFromRms((short)(id + 6000)) : mImage2;
        }
        if (mainImage.img == null) {
            ++mainImage.timeImageNull;
            if (mainImage.timeImageNull >= 200) {
                GlobalService.gI().load_image((short)(id + 6000));
                mainImage.timeImageNull = 0;
            }
        }
        return mainImage;
    }

    public static MainImage getImageIconClan(short id) {
        MainImage mainImage = (MainImage)Item.HashImageIconClan.get("" + id);
        if (mainImage == null) {
            mImage mImage2;
            mainImage = new MainImage();
            Item.HashImageIconClan.put("" + id, mainImage);
            String string = "/resLocal/iconclan/" + id + ".png";
            if (id >= 500) {
                int n = id - 500;
                string = "/resLocal/iconclan/shop/" + n + ".png";
            }
            mainImage.img = (mImage2 = mImage.createImage(string)) == null ? ObjectData.getFromRms((short)(id + 7000)) : mImage2;
        } else if (!GameCanvas.isTouch) {
            mainImage.count = (int)(GameCanvas.timeNow / 1000L);
        }
        if (mainImage.img == null) {
            ++mainImage.timeImageNull;
            if (mainImage.timeImageNull >= 200) {
                GlobalService.gI().load_image((short)(id + 7000));
                mainImage.timeImageNull = 0;
            }
        }
        return mainImage;
    }

    public static MainImage getImageIconArCheClan(short id) {
        MainImage mainImage = (MainImage)Item.HashImageIconArcheClan.get("" + id);
        if (mainImage == null) {
            mainImage = new MainImage();
            Item.HashImageIconArcheClan.put("" + id, mainImage);
            mImage mImage2 = mImage.createImage("/resLocal/iconclan/huyhieu/" + id + ".png");
            mainImage.img = mImage2 == null ? ObjectData.getFromRms((short)(id + 9500)) : mImage2;
        }
        if (mainImage.img == null) {
            ++mainImage.timeImageNull;
            if (mainImage.timeImageNull >= 200) {
                GlobalService.gI().load_image((short)(id + 9500));
                mainImage.timeImageNull = 0;
            }
        }
        return mainImage;
    }

    public static MainImage getImageIconPet(short id) {
        MainImage mainImage = (MainImage)Item.HashImagePetIcon.get("" + id);
        if (mainImage == null) {
            mainImage = new MainImage();
            Item.HashImagePetIcon.put("" + id, mainImage);
            mImage mImage2 = mImage.createImage("/resLocal/pet/icon/icon_pet_" + id + ".png");
            mainImage.img = mImage2 == null ? ObjectData.getFromRms((short)(id + 10000)) : mImage2;
        }
        return mainImage;
    }

    public static MainImage getImagePet(short id) {
        MainImage mainImage = (MainImage)Pet.HashImagePet.get("" + id);
        if (mainImage == null) {
            mainImage = new MainImage();
            Pet.HashImagePet.put("" + id, mainImage);
            mImage mImage2 = mImage.createImage("/resLocal/pet/" + id + ".png");
            mainImage.img = mImage2 == null ? ObjectData.getFromRms((short)(id + 10200)) : mImage2;
        }
        if (mainImage.img == null) {
            ++mainImage.timeImageNull;
            if (mainImage.timeImageNull >= 200) {
                GlobalService.gI().load_image((short)(id + 10200));
                mainImage.timeImageNull = 0;
            }
        }
        return mainImage;
    }

    public static mImage getFromRms(short id) {
        mImage mImage2 = null;
        String string = "image" + id;
        byte[] byArray = null;
        if (ObjectData.setIdOK(id)) {
            byArray = CRes.loadRMS("image" + id);
        }
        if (byArray == null) {
            GlobalService.gI().load_image(id);
            return mImage2;
        }
        try {
            mImage2 = mImage.createImage(byArray, 0, byArray.length, string);
        }
        catch (Exception exception) {
            GlobalService.gI().load_image(id);
            return null;
        }
        return mImage2;
    }

    public static boolean setIdOK(short id) {
        return id >= 4000 && id < 5000 || id >= 3000 && id < 3500 || id >= 6000 && id < 7000;
    }

    public static void setToRms(byte[] mimg, short id) {
        try {
            CRes.saveRMS("image" + id, mimg);
        }
        catch (Exception exception) {
            exception.printStackTrace();
        }
    }

    public static void checkDelHash(mHashtable hash) {
        Enumeration enumeration = hash.keys();
        while (enumeration.hasMoreElements()) {
            String string = (String)enumeration.nextElement();
            MainImage mainImage = (MainImage)hash.get(string);
            if (GameCanvas.timeNow / 1000L - (long)mainImage.count <= 300L) continue;
            if (mainImage.img != null) {
                mainImage.img.clearData();
            }
            hash.remove(string);
        }
    }

    public static MainImage getImageMount(short id) {
        MainImage mainImage = (MainImage)Item.HashImageMount.get("" + id);
        if (mainImage == null) {
            mainImage = new MainImage();
            Item.HashImageMount.put("" + id, mainImage);
            mImage mImage2 = mImage.createImage("/resLocal/mount/" + id + ".png");
            mainImage.img = mImage2 == null ? ObjectData.getFromRms((short)(id + 10700)) : mImage2;
        }
        if (mainImage.img == null) {
            ++mainImage.timeImageNull;
            if (mainImage.timeImageNull >= 200) {
                GlobalService.gI().load_image((short)(id + 10700));
                mainImage.timeImageNull = 0;
            }
        }
        return mainImage;
    }
}

