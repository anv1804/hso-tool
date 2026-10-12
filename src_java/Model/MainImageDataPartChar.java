/*
 * Decompiled with CFR 0.152.
 */
package Model;

import CLib.mImage;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.OutputStream;

public class MainImageDataPartChar {
    public mImage img;
    public int count = -1;
    public byte type;
    public short id;
    public short version;
    public long timeImageNull = 0L;
    public byte[] isData = null;
    public byte[] isDataImg = null;

    public MainImageDataPartChar() {
    }

    public MainImageDataPartChar(byte[] issImg, byte[] iss, byte type, short id, short version) {
        this.isDataImg = issImg;
        this.count = 0;
        this.isData = iss;
        this.type = type;
        this.id = id;
        this.version = version;
    }

    public MainImageDataPartChar(mImage img, byte[] iss) {
        this.img = img;
        this.count = 0;
        this.isData = iss;
    }

    public ByteArrayOutputStream getSaveData() {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        DataOutputStream dataOutputStream = new DataOutputStream(byteArrayOutputStream);
        try {
            dataOutputStream.writeShort(this.version);
            dataOutputStream.writeInt(this.isDataImg.length);
            ((OutputStream)dataOutputStream).write(this.isDataImg);
            dataOutputStream.writeShort(this.isData.length);
            ((OutputStream)dataOutputStream).write(this.isData);
        }
        catch (Exception exception) {
            // empty catch block
        }
        return byteArrayOutputStream;
    }
}

