/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  javax.microedition.lcdui.Graphics
 */
package CLib;

import CLib.mImage;
import Main.GameCanvas;
import Model.AvMain;
import javax.microedition.lcdui.Graphics;

public class mGraphics {
    public Graphics g;
    public static int zoomLevel = 1;
    public static final int HCENTER = 1;
    public static final int VCENTER = 2;
    public static final int LEFT = 4;
    public static final int RIGHT = 8;
    public static final int TOP = 16;
    public static final int BOTTOM = 32;
    public static final int BASELINE = 64;
    public static final int SOLID = 0;
    public static final int DOTTED = 1;
    public static final boolean isTrue = true;
    public static final boolean isFalse = false;
    int wImg;
    int hImg;

    public mGraphics(Graphics graphics) {
        this.g = graphics;
    }

    public mGraphics() {
    }

    public void drawImage(mImage image, int x, int y, int anchor, boolean useClip) {
        this.g.drawImage(image.image, x *= zoomLevel, y *= zoomLevel, anchor);
    }

    public void drawLine(int x1, int y1, int x2, int y2, boolean useClip) {
        x1 *= zoomLevel;
        y1 *= zoomLevel;
        x2 *= zoomLevel;
        y2 *= zoomLevel;
        for (int i = 0; i < zoomLevel; ++i) {
            this.g.drawLine(x1 + i, y1 + i, x2 + i, y2 + i);
            if (i <= 0) continue;
            this.g.drawLine(x1 + i, y1, x2 + i, y2);
            this.g.drawLine(x1, y1 + i, x2, y2 + i);
        }
    }

    public void drawRect(int x, int y, int w, int h, boolean useClip) {
        x *= zoomLevel;
        y *= zoomLevel;
        w *= zoomLevel;
        h *= zoomLevel;
        for (int i = 0; i < zoomLevel; ++i) {
            this.g.drawRect(x + i, y + i, w - i * 2, h - i * 2);
        }
    }

    public void drawRegion(mImage arg0, int x0, int y0, int w0, int h0, int arg5, int x, int y, int arg8, boolean useClip) {
        this.g.drawRegion(arg0.image, x0 *= zoomLevel, y0 *= zoomLevel, w0 *= zoomLevel, h0 *= zoomLevel, arg5, x *= zoomLevel, y *= zoomLevel, arg8);
    }

    public void drawRoundRect(int x, int y, int w, int h, int arcWidth, int arcHeight, boolean useClip) {
        this.g.drawRoundRect(x *= zoomLevel, y *= zoomLevel, w *= zoomLevel, h *= zoomLevel, arcWidth *= zoomLevel, arcHeight *= zoomLevel);
    }

    public void drawString(String s, int x, int y, int style, boolean useClip) {
        this.g.drawString(s, x *= zoomLevel, y *= zoomLevel, style);
    }

    public void fillRect(int x, int y, int w, int h, boolean useClip) {
        this.g.fillRect(x *= zoomLevel, y *= zoomLevel, w *= zoomLevel, h *= zoomLevel);
    }

    public void fillRoundRect(int x, int y, int width, int height, int arcWidth, int arcHeight, boolean useClip) {
        this.g.fillRoundRect(x *= zoomLevel, y *= zoomLevel, width *= zoomLevel, height *= zoomLevel, arcWidth *= zoomLevel, arcHeight *= zoomLevel);
    }

    public void drawArc(int n, int n2, int n3, int n4, int n5, int n6, boolean bl) {
        this.g.drawArc(n *= zoomLevel, n2 *= zoomLevel, n3 *= zoomLevel, n4 *= zoomLevel, n5, n6);
    }

    public void fillTriangle(int x1, int x2, int x3, int x4, int x5, int x6, boolean isClip) {
        this.g.fillTriangle(x1 *= zoomLevel, x2 *= zoomLevel, x3 *= zoomLevel, x4 *= zoomLevel, x5 *= zoomLevel, x6 *= zoomLevel);
    }

    public int getTranslateX() {
        return this.g.getTranslateX() / zoomLevel;
    }

    public int getTranslateY() {
        return this.g.getTranslateY() / zoomLevel;
    }

    public void setClip(int x, int y, int w, int h) {
        this.g.setClip(x *= zoomLevel, y *= zoomLevel, w *= zoomLevel, h *= zoomLevel);
    }

    public void endClip() {
    }

    public void setColor(int color) {
        this.g.setColor(color);
    }

    public void setColor(int rgb, float alpha) {
        this.g.setColor(rgb);
    }

    public void translate(int tx, int ty) {
        this.g.translate(tx *= zoomLevel, ty *= zoomLevel);
    }

    public void fillRecAlpla(int x, int y, int w, int h, int color) {
        this.drawRecAlpa(0, 0, GameCanvas.loadmap.mapW * 24, y, color);
        this.drawRecAlpa(0, y, x, GameCanvas.loadmap.mapH * 24 - y, color);
        this.drawRecAlpa(x, y + h, GameCanvas.loadmap.mapW * 24 - x, GameCanvas.loadmap.mapH * 24 - (y + h), color);
        this.drawRecAlpa(x + w, y, GameCanvas.loadmap.mapW * 24 - (x + w), h, color);
    }

    public void drawRecAlpa(int x, int y, int w, int h, int color) {
        this.setColor(color);
        this.fillRect(x, y, w, h, false);
    }

    public void fillRect(int x, int y, int w, int h, int color, int alpha, boolean useClip) {
        x *= zoomLevel;
        y *= zoomLevel;
        w *= zoomLevel;
        h *= zoomLevel;
        if (this.wImg != mImage.getImageWidth(AvMain.imgRect.image)) {
            this.wImg = mImage.getImageWidth(AvMain.imgRect.image);
        }
        if (this.hImg != mImage.getImageHeight(AvMain.imgRect.image)) {
            this.hImg = mImage.getImageHeight(AvMain.imgRect.image);
        }
        this.setClip(x, y, w, h);
        for (int i = x % this.wImg; i < w + this.wImg; i += this.wImg) {
            for (int j = y % this.hImg; j < h + this.hImg; j += this.hImg) {
                this.drawImage(AvMain.imgRect, x + w - i, y + h - j, 20, true);
            }
        }
        this.setClip(-this.g.getTranslateX(), -this.g.getTranslateY(), GameCanvas.w, GameCanvas.h);
    }

    public void restoreCanvas() {
    }

    public void saveCanvas() {
    }

    public void ClipRec(int n, int n2, int n3, int n4) {
    }

    public static void resetTransAndroid(mGraphics g) {
    }

    public void translateAndroid(int n, int n2) {
    }
}

