/*
 * Decompiled with CFR 0.152.
 */
package InterfaceComponents;

public class DataRebuildItem {
    public short id;
    public short valueWing;
    public byte lv;
    public byte cat;
    public int priceCoin;
    public int priceGold;
    public byte[] mValue;

    public DataRebuildItem() {
    }

    public DataRebuildItem(byte lv, int coin, int gold, byte[] mvalue) {
        this.lv = lv;
        this.priceCoin = coin;
        this.priceGold = gold;
        this.mValue = mvalue;
    }

    public DataRebuildItem(short id, short cat) {
        this.id = id;
        this.valueWing = cat;
    }

    public DataRebuildItem(short id, byte cat) {
        this.id = id;
        this.cat = cat;
    }
}

