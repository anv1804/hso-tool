/*
 * Decompiled with CFR 0.152.
 */
package GameObjects;

import CLib.mVector;

public class FrameEff {
    public mVector listPart = new mVector();
    public mVector listPartTop = new mVector();
    public mVector listPartBottom = new mVector();
    public byte xShadow;
    public int yShadow;

    public FrameEff(mVector list) {
        this.listPart = list;
    }

    public FrameEff(mVector listtop, mVector listbottom) {
        this.listPartTop = listtop;
        this.listPartBottom = listbottom;
    }
}

