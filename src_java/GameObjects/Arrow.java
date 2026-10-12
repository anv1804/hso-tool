/*
 * Decompiled with CFR 0.152.
 */
package GameObjects;

import CLib.mGraphics;
import GameObjects.IArrow;
import GameObjects.MainObject;
import Model.CRes;
import Model.FrameImage;

public class Arrow
extends IArrow {
    public static final int[] ARROWINDEX = new int[]{0, 15, 37, 52, 75, 105, 127, 142, 165, 195, 217, 232, 255, 285, 307, 322, 345, 370};
    public int power;
    public int effect;
    public int typeEffEnd = -1;
    private MainObject target;
    private int[] xw;
    private int[] yw;
    int frame;
    int transform;
    int pos;
    private FrameImage img;
    public static final byte[][] ARROWSIZE = new byte[][]{new byte[0], new byte[0]};
    public static final int[] TRANSFORM = new int[]{0, 0, 0, 7, 6, 6, 6, 2, 2, 3, 3, 4, 5, 5, 5, 1};
    public static final byte[] FRAME = new byte[]{0, 1, 2, 1, 0, 1, 2, 1, 0, 1, 2, 1, 0, 1, 2, 1, 0, 1, 2, 1, 0, 1, 2, 1, 0};
    private short endeff;

    public Arrow(int imgIndex) {
    }

    public Arrow() {
    }

    public void setAngle(int angle) {
    }

    public void onArrowTouchTarget() {
        this.wantDestroy = true;
    }

    public void paint(mGraphics g) {
        if (this.img != null) {
            this.img.drawFrameEffectSkill(this.frame, this.xw[this.pos], this.yw[this.pos], this.transform, 3, g);
        }
    }

    public void set(int type, int x, int y, int power, short effect, MainObject owner, MainObject target) {
    }

    public void set(int type, int x, int y, int power, short effect, MainObject owner, MainObject target, int idend) {
        int n6;
        this.effect = idend;
        this.effect = effect;
        this.target = target;
        this.power = power;
        int n7 = 0;
        int n8 = 0;
        n7 = target.x - x;
        n8 = target.y + target.getDy() - y;
        if (type == 7) {
            n7 = target.x + 10 - x;
            n8 = target.y + target.getDy() - y;
        } else if (type == 8) {
            n7 = target.x - 10 - x;
            n8 = target.y - 10 + target.getDy() - y;
        }
        int n9 = (Math.abs(n7) + Math.abs(n8)) / 20;
        if (n9 < 2) {
            n9 = 2;
        }
        this.xw = new int[n9];
        this.yw = new int[n9];
        for (n6 = 1; n6 < n9; ++n6) {
            this.xw[n6] = x + n6 * n7 / n9;
            this.yw[n6] = y + n6 * n8 / n9;
        }
        n6 = Arrow.findDirIndexFromAngle(CRes.angle(n7, -n8));
        this.frame = FRAME[n6];
        this.transform = TRANSFORM[n6];
    }

    public static int findDirIndexFromAngle(int angle) {
        for (int i = 0; i < ARROWINDEX.length - 1; ++i) {
            if (angle < ARROWINDEX[i] || angle > ARROWINDEX[i + 1]) continue;
            if (i >= 16) {
                return 0;
            }
            return i;
        }
        return 0;
    }

    public void update() {
        ++this.pos;
        if (this.pos >= this.xw.length) {
            this.pos = this.xw.length;
        }
        if (this.pos == this.xw.length) {
            this.onArrowTouchTarget();
            return;
        }
    }

    public void SetEffFollow(int id) {
    }
}

