/*
 * Decompiled with CFR 0.152.
 */
package GameEffect;

import CLib.mGraphics;
import GameObjects.MainObject;
import GameScreen.MainScreen;
import Main.GameCanvas;
import Model.FrameImage;

public class MainEffect {
    boolean isPaint = true;
    public int typeEffect = 0;
    public int valueEffect;
    public int timeRemove;
    int fRemove;
    public int x;
    public int y;
    public int toX;
    public int toY;
    public int f;
    public int frame;
    public int ysai;
    public int Direction;
    public int vMax;
    public int vx;
    public int vy;
    public int hOne;
    public int subf;
    public int levelPaint = 0;
    long timeBegin;
    public MainObject objBeKillMain;
    public byte[] nFrame;
    public byte[] nSubFrame;
    public FrameImage fraImgEff;
    public FrameImage fraImgSubEff;
    public FrameImage fraImgSub2Eff;
    public FrameImage fraImgSub3Eff;
    public boolean isRemove;
    public boolean isStop;

    public void paint(mGraphics g) {
    }

    public void clearData() {
        if (this.fraImgEff != null) {
            this.fraImgEff.clearData();
            this.fraImgEff = null;
        }
        if (this.fraImgSubEff != null) {
            this.fraImgSubEff.clearData();
            this.fraImgSubEff = null;
        }
        if (this.fraImgSub2Eff != null) {
            this.fraImgSub2Eff.clearData();
            this.fraImgSub2Eff = null;
        }
        if (this.fraImgSub3Eff != null) {
            this.fraImgSub3Eff.clearData();
            this.fraImgSub3Eff = null;
        }
    }

    public void update() {
        this.x += this.vx;
        this.y += this.vy;
    }

    public void setPosition(int x, int y, int xto, int yto) {
        this.x = x;
        this.y = y;
        this.toX = xto;
        this.toY = yto;
    }

    public static boolean isInScreen(int x, int y, int w, int h) {
        return x >= MainScreen.cameraMain.xCam - w && x <= MainScreen.cameraMain.xCam + GameCanvas.w + w && y >= MainScreen.cameraMain.yCam - h && y <= MainScreen.cameraMain.yCam + GameCanvas.h + h;
    }

    public void setObjFrom(short id, byte tem) {
    }

    public void setTimeRemove(short time) {
    }
}

