/*
 * Decompiled with CFR 0.152.
 */
package GameObjects;

public class PartFrame {
    public short dx;
    public short dy;
    public byte flip;
    public byte onTop = 0;
    public byte idSmallImg;

    public PartFrame(short dx, short dy, byte idSmall) {
        this.idSmallImg = idSmall;
        this.dx = dx;
        this.dy = dy;
    }
}

