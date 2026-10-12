/*
 * Decompiled with CFR 0.152.
 */
package GameObjects;

public class item_sell {
    public int price;
    public short id;
    public short soluuong;
    public byte cat;

    public item_sell(short id, int price) {
        this.id = id;
        this.price = price;
    }

    public item_sell() {
    }

    public item_sell(short id, int price, int soluong) {
        this.id = id;
        this.price = price;
        this.soluuong = (short)soluong;
    }

    public item_sell(short id, int price, int soluong, int cat) {
        this.id = id;
        this.price = price;
        this.soluuong = (short)soluong;
        this.cat = (byte)cat;
    }
}

