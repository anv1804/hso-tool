/*
 * Decompiled with CFR 0.152.
 */
package GameObjects;

import CLib.mFont;
import CLib.mGraphics;
import CLib.mHashtable;
import CLib.mImage;
import CLib.mSystem;
import CLib.mVector;
import GameEffect.Effect_UpLv_Item;
import GameObjects.Atb_info;
import GameObjects.MainInfoItem;
import GameObjects.MainItem;
import GameObjects.ObjectData;
import GameScreen.GameScreen;
import GameScreen.PaintInfoGameScreen;
import InterfaceComponents.MainTabNew;
import Main.GameCanvas;
import Model.AvMain;
import Model.CRes;
import Model.FrameImage;
import Model.InfocontenNew;
import Model.MainImage;
import Model.MainTemplateItem;
import Model.T;
import netcommand.global.GlobalService;

public class Item {
    public static byte TYPE_NGOC_KHAM = (byte)49;
    public static byte TYPE_BUA_HUYEN_BI = (byte)50;
    public static int ID_TEM_VE_MUA_BAN = 51;
    public static String[] nameInfoItem;
    public static byte[] colorInfoItem;
    public static byte[] isPercentInfoItem;
    public String itemName;
    public String content;
    public String itemNameExcludeLv;
    public MainInfoItem[] mInfo;
    public String[] mcontent;
    public String[] mPlusContent;
    public int[] mColor;
    public int[] mPlusColor;
    public int timeupdateMore;
    public int timeUse;
    public long priceItem;
    public static Effect_UpLv_Item eff_UpLv;
    public int sizeW = 60;
    public int imageId;
    public int ItemCatagory;
    public short IdTem;
    public static FrameImage fraeffitemdrop;
    int wImage = 16;
    public byte canSell = 0;
    public byte canTrade = 0;
    public byte tier = 0;
    public byte typeMaterial;
    public byte isLock = 0;
    public byte IndexSort = 0;
    public long timeDem = 0L;
    public byte Selling = 0;
    public byte can_sell_for_other_player = 0;
    public int timeDefaultItemFashion = -1;
    public long currentTimeItemFashion = -1L;
    public byte CanShell_CanNotTrade = 0;
    public boolean isSell = false;
    public String[] infoHop = new String[]{"", ""};
    public mVector moreContenGem = new mVector("EffectSkill moreContenGem");
    public int numPotion;
    public byte typePotion;
    public static final byte HP = 0;
    public static final byte MP = 1;
    public static final byte COIN = 2;
    public static final byte GOLD = 6;
    public int classcharItem;
    public int colorNameItem = 0;
    public int Id;
    public int idTab;
    public int sell;
    public int value;
    public byte priceType;
    public int idPart;
    public int type_Only_Item;
    public int LvItem;
    public String diaHoiEvent = "";
    public static mHashtable VecEquipPlayer;
    public static mVector VecInvetoryPlayer;
    public static mVector VecChestPlayer;
    public static mVector VecPetContainer;
    public static mVector VecClanInvetory;
    public static mVector VecLotteryReward;
    public static mVector VecItemSell;
    public static mVector VecItem_Sell_in_store;
    public static mHashtable HashImageItem;
    public static mHashtable HashImagePotion;
    public static mHashtable HashImageQuestItem;
    public static mHashtable HashImageMaterial;
    public static mHashtable HashImageIconClan;
    public static mHashtable HashImageIconArcheClan;
    public static mHashtable HashImagePetIcon;
    public static mHashtable HashImageMount;
    public static byte[] frameicon;
    public byte frame = 0;
    public Item itemClone;
    public static byte[] ATB_Can_Not_Paint;

    public boolean isOutTime() {
        return mSystem.currentTimeMillis() / 1000L - this.currentTimeItemFashion > (long)this.timeDefaultItemFashion;
    }

    public boolean isFashionItem() {
        return this.timeDefaultItemFashion != -1;
    }

    public long getTimeItemFashion() {
        return mSystem.currentTimeMillis() - this.currentTimeItemFashion;
    }

    public static boolean isWeapone(int type) {
        return type >= 8 && type <= 11;
    }

    public void addhole(short[] typehole) {
    }

    public static boolean isMaterial_Light_Drak(int typeMaterial) {
        return typeMaterial > 22 && typeMaterial < 33;
    }

    public void paintItem(mGraphics g, int x, int y, int w, int lech, int numlech) {
        MainImage mainImage = null;
        if (this.itemName == null) {
            if (this.setNameNull()) {
                return;
            }
        } else {
            int n6;
            if (this.colorNameItem > 0) {
                n6 = this.colorNameItem;
                if (this.colorNameItem >= 20 && this.colorNameItem < 30) {
                    n6 = 6;
                } else if (this.colorNameItem >= 30 && this.colorNameItem < 40) {
                    n6 = 7;
                } else if (this.colorNameItem >= 40 && this.colorNameItem < 50) {
                    n6 = 8;
                }
                g.drawRegion(AvMain.imgColorItem, 0, (n6 - 1) * 32, w - 1, w - 1, 2, x - (w - 1) / 2, y - (w - 1) / 2, 0, true);
            }
            if (this.ItemCatagory == 6) {
                if (this.priceItem <= 0L) {
                    g.drawRegion(AvMain.imgColorItem, 0, 0, w - 1, w - 1, 2, x - (w - 1) / 2, y - (w - 1) / 2, 0, true);
                }
                CRes.getCharPartInfo(5, this.imageId).paintShow(g, x, y, 0, 0);
                return;
            }
            if (this.ItemCatagory == 5) {
                mainImage = ObjectData.getImageQuestItem((short)this.imageId);
            } else if (this.ItemCatagory == 4) {
                mainImage = ObjectData.getImagePotion((short)this.imageId);
            } else if (this.ItemCatagory == 3) {
                mainImage = ObjectData.getImageItem((short)this.imageId);
            } else if (this.ItemCatagory == 7) {
                mainImage = ObjectData.getImageMaterial((short)this.imageId);
            } else if (this.ItemCatagory == 8) {
                mainImage = ObjectData.getImageIconClan((short)this.Id);
            } else if (this.ItemCatagory == 9) {
                mainImage = ObjectData.getImageIconPet((short)this.imageId);
            }
            if (this.ItemCatagory != 6) {
                if (mainImage != null && mainImage.img != null) {
                    if (mImage.getImageHeight(mainImage.img.image) / 18 == 3) {
                        if (GameCanvas.gameTick % 6 == 0) {
                            n6 = frameicon.length;
                            if (n6 == 0) {
                                n6 = 1;
                            }
                            this.frame = (byte)((this.frame + 1) % n6);
                        }
                        g.drawRegion(mainImage.img, 0, frameicon[this.frame] * 18, 18, 18, 0, x, y, 3, true);
                    } else {
                        g.drawImage(mainImage.img, x, y, 3, true);
                    }
                } else {
                    g.drawRegion(AvMain.imgLoadImg, 0, GameCanvas.gameTick % 12 * 16, 16, 16, 0, x, y, 3, true);
                }
            }
            if (this.ItemCatagory == 3 && this.tier > 0) {
                n6 = this.tier;
                if (this.type_Only_Item == 7) {
                    n6 = this.tier / 2;
                }
                eff_UpLv.paintUpgradeEffect(x, y, n6, w - 4, g, lech);
            }
        }
        if (this.numPotion > 1) {
            mFont.number_Yellow_Small.drawString(g, this.numPotion + "", x + w / 2 - 1 - numlech, y + w / 2 - 9 - numlech, 1, true);
        }
        if (this.isLock > 0) {
            g.drawImage(AvMain.imgLock, x + 4, y + 3, 0, true);
        }
        if (this.ItemCatagory == 3) {
            if (this.timeUse > 0) {
                this.updateTime();
            }
            if (this.isFashionItem()) {
                this.updateFashion();
            }
        }
    }

    public void paintItem_notnum(mGraphics g, int x, int y, int w, int lech, int numlech) {
        MainImage mainImage = null;
        if (this.itemName == null) {
            if (this.setNameNull()) {
                return;
            }
        } else {
            int n6;
            if (this.colorNameItem > 0) {
                n6 = this.colorNameItem;
                if (this.colorNameItem >= 20 && this.colorNameItem < 30) {
                    n6 = 6;
                } else if (this.colorNameItem >= 30 && this.colorNameItem < 40) {
                    n6 = 7;
                } else if (this.colorNameItem >= 40 && this.colorNameItem < 50) {
                    n6 = 8;
                }
                g.drawRegion(AvMain.imgColorItem, 0, (n6 - 1) * 32, w - 1, w - 1, 2, x - (w - 1) / 2, y - (w - 1) / 2, 0, true);
            }
            if (this.ItemCatagory == 6) {
                if (this.priceItem <= 0L) {
                    g.drawRegion(AvMain.imgColorItem, 0, 0, w - 1, w - 1, 2, x - (w - 1) / 2, y - (w - 1) / 2, 0, true);
                }
                CRes.getCharPartInfo(5, this.imageId).paintShow(g, x, y, 0, 0);
                return;
            }
            if (this.ItemCatagory == 5) {
                mainImage = ObjectData.getImageQuestItem((short)this.imageId);
            } else if (this.ItemCatagory == 4) {
                mainImage = ObjectData.getImagePotion((short)this.imageId);
            } else if (this.ItemCatagory == 3) {
                mainImage = ObjectData.getImageItem((short)this.imageId);
            } else if (this.ItemCatagory == 7) {
                mainImage = ObjectData.getImageMaterial((short)this.imageId);
            } else if (this.ItemCatagory == 8) {
                mainImage = ObjectData.getImageIconClan((short)this.Id);
            } else if (this.ItemCatagory == 9) {
                mainImage = ObjectData.getImageIconPet((short)this.imageId);
            }
            if (this.ItemCatagory != 6) {
                if (mainImage != null && mainImage.img != null) {
                    if (mImage.getImageHeight(mainImage.img.image) / 18 == 3) {
                        if (GameCanvas.gameTick % 6 == 0) {
                            n6 = frameicon.length;
                            if (n6 == 0) {
                                n6 = 1;
                            }
                            this.frame = (byte)((this.frame + 1) % n6);
                        }
                        g.drawRegion(mainImage.img, 0, frameicon[this.frame] * 18, 18, 18, 0, x, y, 3, true);
                    } else {
                        g.drawImage(mainImage.img, x, y, 3, true);
                    }
                } else {
                    g.drawRegion(AvMain.imgLoadImg, 0, GameCanvas.gameTick % 12 * 16, 16, 16, 0, x, y, 3, true);
                }
            }
            if (this.ItemCatagory == 3 && this.tier > 0) {
                n6 = this.tier;
                if (this.type_Only_Item == 7) {
                    n6 = this.tier / 2;
                }
                eff_UpLv.paintUpgradeEffect(x, y, n6, w - 4, g, lech);
            }
        }
        if (this.isLock > 0) {
            g.drawImage(AvMain.imgLock, x + 4, y + 3, 0, true);
        }
        if (this.ItemCatagory == 3) {
            if (this.timeUse > 0) {
                this.updateTime();
            }
            if (this.isFashionItem()) {
                this.updateFashion();
            }
        }
    }

    public void paintItemNew(mGraphics g, int x, int y, int w, int lech, int numlech) {
        MainImage mainImage = null;
        if (this.itemName == null) {
            if (this.setNameNull()) {
                return;
            }
        } else {
            int n6;
            if (this.colorNameItem > 0) {
                n6 = this.colorNameItem;
                if (this.colorNameItem >= 20 && this.colorNameItem < 30) {
                    n6 = 6;
                } else if (this.colorNameItem >= 30 && this.colorNameItem < 40) {
                    n6 = 7;
                } else if (this.colorNameItem >= 40 && this.colorNameItem < 50) {
                    n6 = 8;
                }
                g.drawRegion(AvMain.imgColorItem, 0, (n6 - 1) * 32, w - 1, w - 1, 2, x - (w - 1) / 2, y - (w - 1) / 2, 0, true);
            }
            if (this.ItemCatagory == 6) {
                if (this.priceItem <= 0L) {
                    g.drawRegion(AvMain.imgColorItem, 0, 0, w - 1, w - 1, 2, x - (w - 1) / 2, y - (w - 1) / 2, 0, true);
                }
                CRes.getCharPartInfo(5, this.imageId).paintShow(g, x, y, 0, 0);
                return;
            }
            if (this.ItemCatagory == 5) {
                mainImage = ObjectData.getImageQuestItem((short)this.imageId);
            } else if (this.ItemCatagory == 4) {
                mainImage = ObjectData.getImagePotion((short)this.imageId);
            } else if (this.ItemCatagory == 3) {
                mainImage = ObjectData.getImageItem((short)this.imageId);
            } else if (this.ItemCatagory == 7) {
                mainImage = ObjectData.getImageMaterial((short)this.imageId);
            } else if (this.ItemCatagory == 8) {
                mainImage = ObjectData.getImageIconClan((short)this.Id);
            } else if (this.ItemCatagory == 9) {
                mainImage = ObjectData.getImageIconPet((short)this.imageId);
            }
            if (this.ItemCatagory != 6) {
                if (mainImage != null && mainImage.img != null) {
                    if (mImage.getImageHeight(mainImage.img.image) / 18 == 3) {
                        if (GameCanvas.gameTick % 10 == 0) {
                            n6 = frameicon.length;
                            if (n6 == 0) {
                                n6 = 1;
                            }
                            this.frame = (byte)((this.frame + 1) % n6);
                        }
                        g.drawRegion(mainImage.img, 0, frameicon[this.frame] * 18, 18, 18, 0, x, y, 3, true);
                    } else {
                        g.drawImage(mainImage.img, x, y, 3, true);
                    }
                } else {
                    g.drawRegion(AvMain.imgLoadImg, 0, GameCanvas.gameTick % 12 * 16, 16, 16, 0, x, y, 3, true);
                }
            }
            if (this.ItemCatagory == 3 && this.tier > 0) {
                n6 = this.tier;
                if (this.type_Only_Item == 7) {
                    n6 = this.tier / 2;
                }
                eff_UpLv.paintUpgradeEffect(x, y, n6, w - 4, g, lech);
            }
        }
        if (this.isLock > 0) {
            g.drawImage(AvMain.imgLock, x + 4, y + 3, 0, true);
        }
        if (this.ItemCatagory == 3) {
            if (this.timeUse > 0) {
                this.updateTime();
            }
            if (this.isFashionItem()) {
                this.updateFashion();
            }
        }
    }

    public void updateTime() {
        if ((GameCanvas.timeNow - this.timeDem) / 1000L >= 60L && !this.isFashionItem()) {
            this.timeDem += 60000L;
            --this.timeUse;
            if (this.mPlusContent != null) {
                this.mPlusContent[0] = this.timeUse > 0 ? (this.type_Only_Item == 14 ? T.thoigianaptrung + PaintInfoGameScreen.getStringTime(this.timeUse) : T.thoigianconlai + PaintInfoGameScreen.getStringTime(this.timeUse)) : "";
            }
        }
    }

    public void updateFashion() {
        if ((GameCanvas.timeNow - this.timeDem) / 1000L >= 60L) {
            this.timeDem += 60000L;
            int n = (int)((long)this.timeDefaultItemFashion - this.getTimeItemFashion() / 60000L);
            if (this.mPlusContent != null) {
                this.mPlusContent[0] = n > 0 ? (this.type_Only_Item == 14 ? T.thoigianaptrung + PaintInfoGameScreen.getStringTime(n) : T.thoigianconlai + PaintInfoGameScreen.getStringTime(n)) : "";
            }
        }
    }

    public boolean setNameNull() {
        if (this.itemName == null) {
            MainTemplateItem mainTemplateItem = MainTemplateItem.getItemTem(this.IdTem);
            if (mainTemplateItem != null && mainTemplateItem.name != null) {
                this.setValueItem(mainTemplateItem);
                return false;
            }
            MainTemplateItem.getItemTem(this.IdTem);
            GameCanvas.start_Ok_Dialog(T.danglaydulieu);
            return true;
        }
        return false;
    }

    public void setValueItem(MainTemplateItem item) {
        this.itemName = item.name;
        this.imageId = item.IconId;
        this.type_Only_Item = item.typeItem;
        if (this.mInfo == null) {
            MainInfoItem[] mainInfoItemArray = new MainInfoItem[item.mValueItem.length];
            for (int i = 0; i < item.mValueItem.length; ++i) {
                mainInfoItemArray[i] = new MainInfoItem(i, item.mValueItem[i]);
            }
            this.mInfo = mainInfoItemArray;
            this.setContentTem();
        }
    }

    public void setContentTem() {
        int n;
        int n2 = 0;
        for (n = 0; n < this.mInfo.length; ++n) {
            MainInfoItem mainInfoItem = this.mInfo[n];
            if (mainInfoItem.value <= 0) continue;
            ++n2;
        }
        this.mcontent = new String[n2];
        this.mColor = new int[n2];
        n2 = 0;
        n = mFont.tahoma_7b_white.getWidth(this.itemName);
        if (n > this.sizeW - 10) {
            this.sizeW = n + 10;
        }
        for (int i = 0; i < this.mInfo.length; ++i) {
            MainInfoItem mainInfoItem = this.mInfo[i];
            if (mainInfoItem.value <= 0) continue;
            this.mcontent[n2] = nameInfoItem[i] + ": " + mainInfoItem.value;
            if (isPercentInfoItem[i] == 1) {
                int n3 = n2;
                this.mcontent[n3] = this.mcontent[n3] + "%";
            }
            this.mColor[n2] = colorInfoItem[i];
            n = mFont.tahoma_7_black.getWidth(this.mcontent[n2]);
            if (n > this.sizeW - 10) {
                this.sizeW = n + 10;
            }
            ++n2;
        }
        if (this.sizeW > 200 && GameCanvas.isTouch) {
            this.sizeW = 130;
        }
        this.setSliptConten();
    }

    public void setContentItem() {
        int n;
        Object object;
        int n2;
        if (this.itemName == null) {
            this.itemName = null;
            return;
        }
        int n3 = this.mInfo.length;
        mVector mVector2 = new mVector("EffectSkill tem");
        for (n2 = 0; n2 < this.mInfo.length; ++n2) {
            MainInfoItem mainInfoItem = this.mInfo[n2];
            if (isPercentInfoItem[mainInfoItem.id] == 4) {
                object = new InfocontenNew(mainInfoItem.value, n2);
                this.moreContenGem.addElement(object);
                continue;
            }
            if (!Item.isATB_Can_paint(isPercentInfoItem[mainInfoItem.id])) continue;
            mVector2.addElement(mainInfoItem);
        }
        n3 = mVector2.size();
        this.mcontent = new String[n3];
        this.mColor = new int[n3];
        n2 = mFont.tahoma_7b_white.getWidth(this.itemName);
        if (n2 > this.sizeW - 10) {
            this.sizeW = n2 + 10;
        }
        for (n = 0; n < n3; ++n) {
            object = (MainInfoItem)mVector2.elementAt(n);
            if (((MainInfoItem)object).value == 0) {
                this.mcontent[n] = nameInfoItem[((MainInfoItem)object).id];
            } else if (this.type_Only_Item == 14) {
                this.mcontent[n] = ((MainInfoItem)object).maxDam > 0 ? nameInfoItem[((MainInfoItem)object).id] + ": " + Item.getPercent(isPercentInfoItem[((MainInfoItem)object).id], ((MainInfoItem)object).value) + "-" + ((MainInfoItem)object).maxDam : nameInfoItem[((MainInfoItem)object).id] + ": " + Item.getPercent(isPercentInfoItem[((MainInfoItem)object).id], ((MainInfoItem)object).value);
            } else {
                this.mcontent[n] = nameInfoItem[((MainInfoItem)object).id] + ": " + (((MainInfoItem)object).id == 70 ? MainItem.getDotNumber(((MainInfoItem)object).value) : Item.getPercent(isPercentInfoItem[((MainInfoItem)object).id], ((MainInfoItem)object).value));
                if (((MainInfoItem)object).id == 70) {
                    this.Selling = 1;
                }
            }
            this.mColor[n] = colorInfoItem[((MainInfoItem)object).id];
            n2 = mFont.tahoma_7_black.getWidth(this.mcontent[n]);
            if (n2 <= this.sizeW - 10) continue;
            this.sizeW = n2 + 10;
        }
        n = 0;
        if (this.timeUse > 0) {
            ++n;
        }
        boolean bl = this.isFashionItem();
        int n4 = (int)((long)this.timeDefaultItemFashion - this.getTimeItemFashion() / 60000L);
        if (n4 <= 0) {
            bl = false;
        }
        if (bl) {
            ++n;
        }
        if (this.priceItem > 0L) {
            ++n;
        }
        if (this.ItemCatagory == 3 && this.classcharItem < 4 && this.classcharItem > -1) {
            ++n;
        }
        if (this.type_Only_Item != 14 && this.LvItem > 0) {
            ++n;
        }
        if (n > 0) {
            this.mPlusContent = new String[n];
            this.mPlusColor = new int[n];
            n = 0;
            if (this.timeUse > 0) {
                this.mPlusColor[n] = 6;
                this.mPlusContent[n] = this.type_Only_Item == 14 ? T.thoigianaptrung + PaintInfoGameScreen.getStringTime(this.timeUse) : T.thoigianconlai + PaintInfoGameScreen.getStringTime(this.timeUse);
                n2 = mFont.tahoma_7_black.getWidth(this.mPlusContent[n]);
                if (n2 > this.sizeW - 10) {
                    this.sizeW = n2 + 10;
                }
                ++n;
            }
            if (bl) {
                this.mPlusColor[n] = 6;
                this.mPlusContent[n] = T.thoigianconlai + PaintInfoGameScreen.getStringTime(n4);
                ++n;
            }
            if (this.ItemCatagory == 3 && this.classcharItem < 4 && this.classcharItem > -1) {
                this.mPlusContent[n] = "[" + T.mClass[this.classcharItem] + "]";
                this.mPlusColor[n] = this.classcharItem == GameScreen.player.clazz ? 0 : 6;
                ++n;
            }
            if (this.priceItem > 0L) {
                this.mPlusContent[n] = T.price + ": " + this.priceItem + this.getPriceType();
                this.mPlusColor[n] = 2;
                ++n;
            }
            if (this.type_Only_Item != 14 && this.LvItem > 0) {
                this.mPlusContent[n] = T.LVyeucau + this.LvItem;
                this.mPlusColor[n] = 0;
                n2 = mFont.tahoma_7_black.getWidth(this.mPlusContent[n]);
                if (n2 > this.sizeW - 10) {
                    this.sizeW = n2 + 10;
                }
            }
        } else {
            this.mPlusContent = null;
            this.mPlusColor = null;
        }
        if (this.sizeW > 200 && GameCanvas.isTouch) {
            this.sizeW = 130;
        }
        this.setSliptConten();
    }

    public void setSliptConten() {
        Object object;
        int n;
        if (this.mcontent == null || this.mcontent != null && this.mcontent.length == 1 || this.mcontent.length == 0) {
            return;
        }
        if (this.ItemCatagory != 3) {
            return;
        }
        int n2 = this.sizeW;
        mVector mVector2 = new mVector();
        String string = this.mcontent[0];
        int n3 = this.mColor[0];
        for (n = 1; n < this.mcontent.length; ++n) {
            object = mFont.tahoma_7_black.splitFontArray(this.mcontent[n], n2);
            for (int i = 0; i < ((String[])object).length; ++i) {
                Atb_info atb_info = new Atb_info(object[i], (byte)this.mColor[n]);
                mVector2.addElement(atb_info);
            }
        }
        this.mcontent = new String[mVector2.size() + 1];
        this.mColor = new int[mVector2.size() + 1];
        this.mcontent[0] = string;
        this.mColor[0] = n3;
        for (n = 0; n < mVector2.size(); ++n) {
            object = (Atb_info)mVector2.elementAt(n);
            this.mcontent[n + 1] = object.info;
            this.mColor[n + 1] = object.id_color;
        }
    }

    public static String getPercent(int t, int value) {
        if (t == 1) {
            if (value % 100 == 0) {
                return value / 100 + "%";
            }
            if (value % 10 == 0) {
                return value / 100 + "." + value % 100 / 10 + "%";
            }
            return value / 100 + "." + value % 100 / 10 + "" + value % 10 + "%";
        }
        if (t == 2) {
            return value / 1000 + "," + value % 1000 / 100 + "s";
        }
        if (t == 3) {
            return value + "$";
        }
        if (t == 4) {
            return " ";
        }
        if (t == 6) {
            return value + " " + T.coin;
        }
        if (t == 7) {
            return "";
        }
        return "" + value;
    }

    public String getPriceType() {
        String string = " " + T.coin;
        if (this.priceType == 1) {
            string = " " + T.gem;
        }
        return string;
    }

    public static Item getItemVec(int type, short ID, mVector vec) {
        for (int i = 0; i < vec.size(); ++i) {
            Item item = (Item)vec.elementAt(i);
            if (item.Id != ID || item.ItemCatagory != type) continue;
            return item;
        }
        return null;
    }

    public static Item getItemInventory(int type, short ID) {
        for (int i = 0; i < VecInvetoryPlayer.size(); ++i) {
            Item item = (Item)VecInvetoryPlayer.elementAt(i);
            if (item.Id != ID || item.ItemCatagory != type) continue;
            return item;
        }
        return null;
    }

    public static Item getItemChest(int type, short ID) {
        for (int i = 0; i < VecChestPlayer.size(); ++i) {
            Item item = (Item)VecChestPlayer.elementAt(i);
            if (item.Id != ID || item.ItemCatagory != type) continue;
            return item;
        }
        return null;
    }

    public static MainItem getMaterial(int id) {
        MainItem mainItem = null;
        mainItem = (MainItem)MainTemplateItem.hashMaterialTem.get(id + "");
        if (mainItem != null) {
            return mainItem;
        }
        return null;
    }

    public static void put_Material(int id) {
        MainItem mainItem = new MainItem();
        MainTemplateItem.hashMaterialTem.put("" + id, mainItem);
        GlobalService.gI().RequestMaterialTemplate((short)id);
    }

    public boolean isMaterialHopNguyenLieu() {
        return false;
    }

    public void setinfo(int ID, String itemName, int imageId, int typeMain, long price, byte priceType, String content, int value, byte typeitem, short num, byte Sell, byte Trade) {
        this.itemName = itemName;
        this.imageId = imageId;
        this.Id = ID;
        this.ItemCatagory = typeMain;
        this.priceItem = price;
        this.priceType = priceType;
        this.value = value;
        this.canSell = Sell;
        this.canTrade = Trade;
        this.content = content;
        this.typeMaterial = typeitem;
        if (content != null) {
            this.mMoreContent(content);
        }
        this.IndexSort = (byte)2;
    }

    public void setinfo(String itemName, int imageId, int typeMain, long price, byte priceType, String content, int value, byte typeitem, short num, byte Sell, byte Trade) {
        this.itemName = itemName;
        this.imageId = imageId;
        this.ItemCatagory = typeMain;
        this.priceItem = price;
        this.priceType = priceType;
        this.value = value;
        this.canSell = Sell;
        this.canTrade = Trade;
        this.content = content;
        this.typeMaterial = typeitem;
        if (content != null) {
            this.mMoreContent(content);
        }
        this.IndexSort = (byte)2;
    }

    public void mMoreContent(String str) {
        int n;
        this.sizeW = 86;
        if (MainTabNew.longwidth > 0) {
            this.sizeW = MainTabNew.longwidth - 5;
        }
        this.mcontent = mFont.tahoma_7_black.splitFontArray(str, this.sizeW - 5);
        if (MainTabNew.longwidth == 0 && ((this.mcontent.length + 1) * GameCanvas.hText > GameCanvas.h / 4 * 3 || mFont.tahoma_7b_black.getWidth(this.itemName) > 70)) {
            this.sizeW = 120;
            this.mcontent = mFont.tahoma_7_black.splitFontArray(str, this.sizeW - 5);
        }
        this.mColor = new int[this.mcontent.length];
        for (n = 0; n < this.mColor.length; ++n) {
            this.mColor[n] = 0;
        }
        n = 0;
        if (this.priceItem > 0L) {
            ++n;
        }
        if (n > 0) {
            this.mPlusContent = new String[n];
            this.mPlusColor = new int[n];
            if (this.priceItem > 0L) {
                this.mPlusContent[n - 1] = T.price + ": " + MainItem.getDotNumber(this.priceItem) + this.getPriceType();
                this.mPlusColor[n - 1] = 2;
            }
        } else {
            this.mPlusContent = null;
            this.mPlusColor = null;
        }
        if (this.sizeW > 200 && GameCanvas.isTouch) {
            this.sizeW = 120;
        }
        this.setSliptConten();
    }

    public static boolean isATB_Can_paint(int id) {
        for (int i = 0; i < ATB_Can_Not_Paint.length; ++i) {
            if (id != ATB_Can_Not_Paint[i]) continue;
            return false;
        }
        return true;
    }

    public void setColorName(int colorNameItem) {
        this.colorNameItem = colorNameItem;
    }

    public void addATBitem(int n, int n2, int n3) {
        this.mInfo = new MainInfoItem[n];
        for (int i = 0; i < n; ++i) {
            this.mInfo[i] = new MainInfoItem(n3, n2);
        }
    }

    static {
        eff_UpLv = new Effect_UpLv_Item();
        VecEquipPlayer = new mHashtable();
        VecInvetoryPlayer = new mVector("Item VecInvetoryPlayer");
        VecChestPlayer = new mVector("Item VecChestPlayer");
        VecPetContainer = new mVector("Item VecPetContainer");
        VecClanInvetory = new mVector("Item VecClanInvetory");
        VecLotteryReward = new mVector("Item VecLotteryReward");
        VecItemSell = new mVector("Item Sell");
        VecItem_Sell_in_store = new mVector("Item Sell in store");
        HashImageItem = new mHashtable();
        HashImagePotion = new mHashtable();
        HashImageQuestItem = new mHashtable();
        HashImageMaterial = new mHashtable();
        HashImageIconClan = new mHashtable();
        HashImageIconArcheClan = new mHashtable();
        HashImagePetIcon = new mHashtable();
        HashImageMount = new mHashtable();
        frameicon = new byte[]{0, 1, 2, 1};
    }
}

