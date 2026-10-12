/*
 * Decompiled with CFR 0.152.
 */
package Model;

import CLib.mGraphics;
import CLib.mHashtable;
import CLib.mImage;
import GameEffect.EffectSkill;
import GameObjects.ObjectData;
import Main.ImageEffect;
import Model.FilePack;
import Model.MainImage;

public class FrameImage {
    public int frameWidth;
    public int frameHeight;
    public int nFrame;
    public mImage imgFrame;
    public int Id = -1;
    public int numWidth;
    public int numHeight;
    public static mHashtable HashFrameImageMount = new mHashtable();
    boolean isNgua = false;

    public FrameImage(mImage img, int width, int height) {
        if (img != null) {
            this.imgFrame = img;
            this.frameWidth = width;
            this.frameHeight = height;
            this.nFrame = mImage.getImageHeight(img.image) / height;
        }
    }

    public void clearData() {
        if (this.imgFrame != null) {
            this.imgFrame.clearData();
        }
    }

    public int getNFrame() {
        return this.nFrame;
    }

    public FrameImage(int ID) {
        this.Id = ID;
        this.frameWidth = EffectSkill.arrInfoEff[ID][0];
        this.frameHeight = EffectSkill.arrInfoEff[ID][1];
        this.nFrame = EffectSkill.arrInfoEff[ID][2];
    }

    public static FrameImage init(String path, int w, int h) {
        return new FrameImage(FilePack.getImg(path), w, h);
    }

    public FrameImage(mImage ID, int numW, int numH, int numNull) {
        mImage mImage3 = ID;
        this.numWidth = numW;
        this.numHeight = numH;
        this.frameWidth = mImage.getImageWidth(mImage3.image) / numW;
        this.frameHeight = mImage.getImageHeight(mImage3.image) / numH;
        this.nFrame = numW * numH - numNull;
    }

    public static FrameImage getFrameImageMount(short id, int numW, int numH, int numNull) {
        FrameImage frameImage = (FrameImage)HashFrameImageMount.get("" + id);
        if (frameImage == null) {
            frameImage = new FrameImage(id, numW, numH, numNull);
            HashFrameImageMount.put("" + id, frameImage);
        } else if (frameImage.imgFrame == null) {
            MainImage mainImage = ObjectData.getImageMount(id);
            if (mainImage != null && mainImage.img != null) {
                frameImage.imgFrame = mainImage.img;
                frameImage.numWidth = numW;
                frameImage.numHeight = numH;
                frameImage.frameWidth = mImage.getImageWidth(frameImage.imgFrame.image) / numW;
                frameImage.frameHeight = mImage.getImageHeight(frameImage.imgFrame.image) / numH;
                if (frameImage.imgFrame != null || frameImage.imgFrame.image != null) {
                    frameImage.nFrame = numW * numH - numNull;
                }
            }
        } else {
            frameImage.numWidth = numW;
            frameImage.numHeight = numH;
            frameImage.frameWidth = mImage.getImageWidth(frameImage.imgFrame.image) / numW;
            frameImage.frameHeight = mImage.getImageHeight(frameImage.imgFrame.image) / numH;
            if (frameImage.imgFrame != null || frameImage.imgFrame.image != null) {
                frameImage.nFrame = numW * numH - numNull;
            }
        }
        return frameImage;
    }

    public FrameImage(int ID, int numW, int numH, int numNull, int n) {
        this.Id = ID;
        this.isNgua = true;
        this.numWidth = numW;
        this.numHeight = numH;
        mImage mImage2 = mImage.createImage("/vantieu/bo.png");
        MainImage mainImage = new MainImage(mImage2);
        if (mainImage != null && mainImage.img != null) {
            this.imgFrame = mainImage.img;
            this.frameWidth = mImage.getImageWidth(this.imgFrame.image) / numW;
            this.frameHeight = mImage.getImageHeight(this.imgFrame.image) / numH;
            if (this.imgFrame != null || this.imgFrame.image != null) {
                this.nFrame = numW * numH - numNull;
            }
        }
    }

    public FrameImage(int ID, int numW, int numH, int numNull) {
        this.Id = ID;
        this.isNgua = true;
        this.numWidth = numW;
        this.numHeight = numH;
        MainImage mainImage = ObjectData.getImageMount((short)ID);
        if (mainImage != null && mainImage.img != null) {
            this.imgFrame = mainImage.img;
            this.frameWidth = mImage.getImageWidth(this.imgFrame.image) / numW;
            this.frameHeight = mImage.getImageHeight(this.imgFrame.image) / numH;
            if (this.imgFrame != null || this.imgFrame.image != null) {
                this.nFrame = numW * numH - numNull;
            }
        }
    }

    public void drawFrameNew(int idx, int x, int y, int trans, int orthor, mGraphics g) {
        if (idx >= 0 && idx < this.nFrame) {
            try {
                int n6 = 0;
                int n7 = idx * this.frameHeight;
                n6 = idx / this.numHeight * this.frameWidth;
                n7 = idx % this.numHeight * this.frameHeight;
                g.drawRegion(this.imgFrame, n6, n7, this.frameWidth, this.frameHeight, trans, x, y, orthor, true);
            }
            catch (Exception exception) {
                // empty catch block
            }
        }
    }

    public void drawFrame(int idx, int x, int y, int trans, int orthor, mGraphics g) {
        if (this.imgFrame == null) {
            mImage mImage2 = ImageEffect.setImage(this.Id);
            if (mImage2 != null) {
                this.imgFrame = mImage2;
            }
            if (mImage2.image != null) {
                this.nFrame = mImage.getImageHeight(this.imgFrame.image) / this.frameHeight;
            }
        } else if (idx >= 0 && idx < this.nFrame && this.imgFrame != null && this.imgFrame.image != null) {
            g.drawRegion(this.imgFrame, 0, idx * this.frameHeight, this.frameWidth, this.frameHeight, trans, x, y, orthor, true);
        }
    }

    public void drawFrameEffectSkill(int idx, int x, int y, int trans, int orthor, mGraphics g) {
        mImage mImage2 = ImageEffect.setImage(this.Id);
        if (mImage2 != null && mImage2.image != null) {
            int n6;
            if (idx > this.nFrame) {
                idx = this.nFrame;
            }
            if ((n6 = idx * this.frameHeight) > this.frameHeight * (this.nFrame - 1) || n6 < 0) {
                n6 = this.frameHeight * (this.nFrame - 1);
            }
            g.drawRegion(mImage2, 0, n6, this.frameWidth, this.frameHeight, trans, x, y, orthor, true);
        }
    }

    public void drawFrameEffectSkill1(int idx, int x, int y, int trans, mGraphics g) {
        mImage mImage2 = ImageEffect.setImage(this.Id);
        if (mImage2 != null && mImage2.image != null) {
            int n5;
            if (idx > this.nFrame) {
                idx = this.nFrame;
            }
            if ((n5 = idx * this.frameHeight) > this.frameHeight * (this.nFrame - 1) || n5 < 0) {
                n5 = this.frameHeight * (this.nFrame - 1);
            }
            g.drawRegion(mImage2, 0, n5, this.frameWidth, this.frameHeight, trans, x, y, 0, true);
        }
    }

    public void drawFrame(int idx, int x, int y, int trans, mGraphics g) {
        if (this.imgFrame == null) {
            mImage mImage2 = ImageEffect.setImage(this.Id);
            if (mImage2 != null) {
                this.imgFrame = mImage2;
            }
            if (this.imgFrame.image != null) {
                this.nFrame = mImage.getImageHeight(this.imgFrame.image) / this.frameHeight;
            }
        } else if (idx >= 0 && idx < this.nFrame && this.imgFrame.image != null) {
            g.drawRegion(this.imgFrame, 0, idx * this.frameHeight, this.frameWidth, this.frameHeight, trans, x, y, 0, true);
        }
    }

    public void drawFrameXY(int idx, int idy, int x, int y, mGraphics g) {
        if (this.imgFrame == null) {
            mImage mImage2 = ImageEffect.setImage(this.Id);
            if (mImage2 != null) {
                this.imgFrame = mImage2;
            }
            if (this.imgFrame.image != null) {
                this.nFrame = mImage.getImageHeight(this.imgFrame.image) / this.frameHeight;
            }
        } else if (idx >= 0 && idx < this.nFrame && this.imgFrame.image != null) {
            g.drawRegion(this.imgFrame, idx * this.frameWidth, idy * this.frameHeight, this.frameWidth, this.frameHeight, 0, x, y, 0, true);
        }
    }
}

