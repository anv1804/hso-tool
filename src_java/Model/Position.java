/*
 * Decompiled with CFR 0.152.
 */
package Model;

import Model.CRes;

public class Position {
    public int x;
    public int y;
    public int anchor;
    public byte depth = 0;
    public short index = (short)-1;

    public Position() {
        this.x = 0;
        this.y = 0;
    }

    public Position(int x, int y) {
        this.x = x;
        this.y = y;
    }

    public Position(int x, int y, int anchor) {
        this.x = x;
        this.y = y;
        this.anchor = anchor;
    }

    public boolean setDetectX(int xx, int num) {
        return CRes.abs(xx - this.x) <= num;
    }

    public boolean setDetectY(int yy, int num) {
        return CRes.abs(yy - this.y) <= num;
    }
}

