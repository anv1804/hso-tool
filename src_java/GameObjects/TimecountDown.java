/*
 * Decompiled with CFR 0.152.
 */
package GameObjects;

import CLib.mFont;
import CLib.mGraphics;
import CLib.mSystem;
import Main.GameCanvas;
import Model.AvMain;
import Thread_More.LoadMap;

public class TimecountDown {
    short mysecond;
    String tile;
    private long currT;
    private long lastT;
    public boolean wantdestroy;
    int x;
    int y;

    public TimecountDown(int second, String tile, int x, int y) {
        this.mysecond = (short)second;
        this.tile = tile;
        this.x = x;
        this.y = y;
        this.currT = mSystem.currentTimeMillis();
        this.lastT = mSystem.currentTimeMillis();
    }

    public void paint(mGraphics g) {
        g.drawImage(AvMain.imgBackInfo, this.x, this.y + GameCanvas.hText / 2, 3, false);
        mFont.tahoma_7b_white.drawString(g, this.tile + " : " + LoadMap.converSecon2hours(this.mysecond), this.x, this.y, 2, false);
    }

    public void update() {
        this.currT = mSystem.currentTimeMillis();
        if (this.currT - this.lastT >= 1000L) {
            this.lastT = mSystem.currentTimeMillis();
            this.mysecond = (short)(this.mysecond - 1);
        }
        if (this.mysecond <= 0) {
            this.wantdestroy = true;
        }
    }
}

