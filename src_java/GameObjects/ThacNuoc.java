/*
 * Decompiled with CFR 0.152.
 */
package GameObjects;

import CLib.mGraphics;
import CLib.mImage;
import Main.GameCanvas;

public class ThacNuoc {
    public byte idImgThac;
    int x;
    int y;
    public static mImage[] allImgThac = null;
    int indexTile = 0;

    public ThacNuoc() {
    }

    public ThacNuoc(int idtile, int x, int y) {
        this.x = x;
        this.y = y;
        this.idImgThac = (byte)idtile;
        if (allImgThac == null) {
            allImgThac = new mImage[9];
            for (int i = 0; i < allImgThac.length; ++i) {
                ThacNuoc.allImgThac[i] = mImage.createImage("/tilethac" + i + ".png");
            }
        }
    }

    public void paint(mGraphics g) {
        if (allImgThac != null && this.idImgThac < allImgThac.length && allImgThac[this.idImgThac] != null) {
            g.drawRegion(allImgThac[this.idImgThac], 0, this.indexTile * 24, 24, 24, this.idImgThac > 4 ? 0 : 0, this.x, this.y, 0, false);
        }
    }

    public void update() {
        if (GameCanvas.gameTick % 4 == 0) {
            this.indexTile = (this.indexTile + 1) % 4;
        }
    }

    public boolean isThacNuoc() {
        return true;
    }
}

