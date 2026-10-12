/*
 * Decompiled with CFR 0.152.
 */
package GameObjects;

import CLib.mGraphics;
import GameObjects.MainObject;
import Model.FrameImage;

public class Mount
extends MainObject {
    public static byte[][] Dir = new byte[][]{{5, 5, 5, 5, 5, 5, 6, 6, 6, 6, 6, 6}, {0, 0, 0, 0, 0, 0, 1, 1, 1, 1, 1, 1}, {0, 0, 0, 0, 0, 0, 1, 1, 1, 1, 1, 1}, {0, 0, 0, 0, 0, 0, 1, 1, 1, 1, 1, 1}};

    public Mount(byte type, byte dir, int x, int y) {
        this.typeMount = type;
        this.x = x;
        this.y = y;
        this.Direction = dir;
        this.Action = 0;
        this.typeObject = (byte)10;
    }

    public void updateMount() {
        this.frameMount = (byte)(this.frameMount + 1);
        if (this.frameMount > Dir[this.Direction].length - 1) {
            this.frameMount = 0;
        }
    }

    public void paint(mGraphics g) {
        if (this.typeMount == -1) {
            return;
        }
        FrameImage frameImage = FrameImage.getFrameImageMount(this.typeMount, 3, 5, 0);
        if (frameImage != null) {
            g.drawImage(MainObject.shadow, this.x, this.y - 8, 3, false);
            frameImage.drawFrameNew(Dir[this.Direction][this.frameMount], this.x + this.xMount, this.y - this.ysai - this.dy + this.yMount, this.Direction > 2 ? 2 : 0, 33, g);
        }
    }
}

