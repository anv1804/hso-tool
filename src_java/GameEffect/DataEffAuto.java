/*
 * Decompiled with CFR 0.152.
 */
package GameEffect;

public class DataEffAuto {
    public byte[] data;
    public short id;
    public long timeremove;

    public DataEffAuto(int id) {
        this.id = (short)id;
    }

    public void setdata(byte[] mdata) {
        this.data = mdata;
    }
}

