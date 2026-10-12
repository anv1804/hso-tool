/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  javax.microedition.lcdui.Image
 */
package CLib;

import CLib.TemGraphics;
import CLib.mGraphics;
import CLib.mSystem;
import java.io.DataInputStream;
import java.io.IOException;
import javax.microedition.lcdui.Image;

public class mImage {
    public Image image;

    public static String getLink(String str) {
        return str;
    }

    public void clearData() {
    }

    public static mImage createEncodedImage(String string) {
        mImage mImage2 = new mImage();
        String string2 = "/x" + mGraphics.zoomLevel + string;
        try {
            DataInputStream dataInputStream = null;
            dataInputStream = new DataInputStream("".getClass().getResourceAsStream(string2));
            int n = dataInputStream.available();
            byte[] byArray = new byte[n];
            dataInputStream.read(byArray, 0, n);
            for (int i = 0; i < byArray.length; ++i) {
                byArray[i] = (byte)(byArray[i] ^ 3);
            }
            mImage2.image = Image.createImage((byte[])byArray, (int)0, (int)n);
        }
        catch (IOException iOException) {
            mSystem.outz("IOException :" + iOException.getMessage());
        }
        catch (IllegalArgumentException illegalArgumentException) {
            mSystem.outz(string2 + " IllegalArgumentException :" + illegalArgumentException.getMessage());
        }
        catch (NullPointerException nullPointerException) {
            mSystem.outz(string2 + " NullPointerException :" + ((Throwable)nullPointerException).getMessage());
        }
        if (mImage2.image == null) {
            return null;
        }
        return mImage2;
    }

    public static String replaceIMGtoPNG(String string, String string2, String string3) {
        String string4 = "";
        String string5 = "";
        int n = string.lastIndexOf(46);
        if (n > 0) {
            string4 = string.substring(n + 1, string.length());
            string5 = string.substring(0, n + 1);
        }
        if (string4.compareTo(string2) == 0) {
            StringBuffer stringBuffer = new StringBuffer(string5);
            stringBuffer.append(string3);
            return stringBuffer.toString();
        }
        return string;
    }

    public static mImage createImage(String url) {
        url = mImage.replaceIMGtoPNG(url, "img", "png");
        String string2 = "";
        int n = url.lastIndexOf(46);
        if (n > 0) {
            string2 = url.substring(n + 1, url.length());
        }
        if (string2.compareTo("img") == 0) {
            return mImage.createEncodedImage(url);
        }
        mImage mImage2 = new mImage();
        try {
            mImage2.image = Image.createImage((String)("/x" + mGraphics.zoomLevel + url));
        }
        catch (Exception exception) {
            // empty catch block
        }
        if (mImage2.image == null) {
            return null;
        }
        return mImage2;
    }

    public static mImage createImageAll(String string) {
        mImage mImage2 = new mImage();
        try {
            mImage2.image = Image.createImage((String)string);
        }
        catch (Exception exception) {
            // empty catch block
        }
        if (mImage2.image == null) {
            return null;
        }
        return mImage2;
    }

    public static DataInputStream openFile(String path) {
        DataInputStream dataInputStream = null;
        dataInputStream = new DataInputStream("".getClass().getResourceAsStream(mImage.getLink(path)));
        return dataInputStream;
    }

    public static mImage createImage(int w, int h) {
        mImage mImage2 = new mImage();
        mImage2.image = Image.createImage((int)(w * mGraphics.zoomLevel), (int)(h * mGraphics.zoomLevel));
        return mImage2;
    }

    public static mImage createImage(byte[] data, int w, int h, String path) {
        mImage mImage2 = new mImage();
        mImage2.image = Image.createImage((byte[])data, (int)0, (int)data.length);
        return mImage2;
    }

    public TemGraphics getGraphics() {
        TemGraphics temGraphics = new TemGraphics();
        temGraphics.g = this.image.getGraphics();
        return temGraphics;
    }

    public static int getImageWidth(Image image) {
        return image.getWidth() / mGraphics.zoomLevel;
    }

    public int getWith() {
        return this.image.getWidth();
    }

    public int getHeight() {
        return this.image.getHeight();
    }

    public static int getImageHeight(Image image) {
        return image.getHeight() / mGraphics.zoomLevel;
    }

    public void getRGB(int[] rgbData, int offset, int scanlength, int x, int y, int width, int height) {
        this.image.getRGB(rgbData, offset, scanlength, x, y, width, height);
    }

    public static mImage createRGBImage(int[] rgb, int width, int height, boolean processAlpha) {
        mImage mImage2 = new mImage();
        mImage2.image = Image.createRGBImage((int[])rgb, (int)width, (int)height, (boolean)processAlpha);
        return mImage2;
    }
}

