/*
 * Decompiled with CFR 0.152.
 */
package Model;

import CLib.mHashtable;
import CLib.mImage;
import CLib.mSystem;
import CLib.mVector;
import GameObjects.CharPartInfo;
import GameObjects.MainInfoItem;
import GameObjects.MainItem;
import GameObjects.MainObject;
import GameObjects.WPSplashInfo;
import GameObjects.WeaponInfo;
import GameScreen.PaintInfoGameScreen;
import Main.GameCanvas;
import Map.MainItemMap;
import Model.FilePack;
import Model.MainImageDataPartChar;
import Skill.Skill;
import Thread_More.Load_Data_And_Img;
import com.silverknight.TemMidlet;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Random;
import netcommand.global.GlobalService;

public class CRes {
    private static short[] sin;
    public static mHashtable hashWeapon;
    public static String RMS_REGION;
    private static short[] cos;
    private static int[] tan;
    static int value;
    public static Random r;
    public static final int LEG = 0;
    public static final int BODY = 1;
    public static final int HEAD = 2;
    public static final int HAT = 3;
    public static final int EYE = 4;
    public static final int HAIR = 5;
    public static final int WING = 6;
    public static CharPartInfo[][] charPartInfo;
    public static WPSplashInfo[][][] wpSplashInfos;

    public static final int sin(int a) {
        if (a >= 0 && a < 90) {
            return sin[a];
        }
        if (a >= 90 && a < 180) {
            return sin[180 - a];
        }
        if (a >= 180 && a < 270) {
            return -sin[a - 180];
        }
        return -sin[360 - a];
    }

    public static final int cos(int a) {
        if (a >= 0 && a < 90) {
            return cos[a];
        }
        if (a >= 90 && a < 180) {
            return -cos[180 - a];
        }
        if (a >= 180 && a < 270) {
            return -cos[a - 180];
        }
        return cos[360 - a];
    }

    public static final int tan(int a) {
        if (a >= 0 && a < 90) {
            return tan[a];
        }
        if (a >= 90 && a < 180) {
            return -tan[180 - a];
        }
        if (a >= 180 && a < 270) {
            return tan[a - 180];
        }
        return -tan[360 - a];
    }

    public static final int atan(int a) {
        for (int i = 0; i <= 90; ++i) {
            if (tan[i] < a) continue;
            return i;
        }
        return 0;
    }

    public static final int angle(int dx, int dy) {
        int n3;
        if (dx != 0) {
            int n4 = Math.abs((dy << 10) / dx);
            n3 = CRes.atan(n4);
            if (dy >= 0 && dx < 0) {
                n3 = 180 - n3;
            }
            if (dy < 0 && dx < 0) {
                n3 = 180 + n3;
            }
            if (dy < 0 && dx >= 0) {
                n3 = 360 - n3;
            }
        } else {
            n3 = dy > 0 ? 90 : 270;
        }
        return n3;
    }

    public static final int fixangle(int angle) {
        if (angle >= 360) {
            angle %= 360;
        }
        if (angle < 0) {
            angle = 360 + angle % 360;
        }
        return angle;
    }

    public static final int subangle(int a1, int a2) {
        int n3 = a2 - a1;
        if (n3 < -180) {
            return n3 + 360;
        }
        if (n3 > 180) {
            return n3 - 360;
        }
        return n3;
    }

    public static int abs(int a) {
        if (a < 0) {
            return -a;
        }
        return a;
    }

    public static int random(int a) {
        return r.nextInt(a);
    }

    public static int random_Am_0(int a) {
        int n2 = 0;
        while (n2 == 0) {
            n2 = r.nextInt() % a;
        }
        return n2;
    }

    public static int random_Am(int a, int b) {
        int n3 = a + r.nextInt(b - a);
        if (CRes.random(2) == 0) {
            n3 = -n3;
        }
        return n3;
    }

    public static int random(int a, int b) {
        return a + r.nextInt(b - a);
    }

    public static int sqrt(int a) {
        int n2;
        if (a <= 0) {
            return 0;
        }
        int n3 = (a + 1) / 2;
        while (Math.abs((n2 = n3) - (n3 = n3 / 2 + a / (2 * n3))) > 1) {
        }
        return n3;
    }

    public static float sqrt(float a) {
        float f2;
        if (a <= 0.0f) {
            return 0.0f;
        }
        float f3 = (a + 1.0f) / 2.0f;
        while (Math.abs((f2 = f3) - (f3 = f3 / 2.0f + a / (2.0f * f3))) > 1.0f) {
        }
        return f3;
    }

    public static int setDis(int x1, int y1, int x2, int y2) {
        return CRes.abs(x1 - x2) + CRes.abs(y1 - y2);
    }

    public static void quickSort(mVector actors) {
        CRes.recQuickSort(actors, 0, actors.size() - 1);
    }

    private static void recQuickSort(mVector actors, int left, int right) {
        try {
            if (right - left <= 0) {
                return;
            }
            MainObject mainObject = (MainObject)actors.elementAt(right);
            int n3 = mainObject.ySort;
            int n4 = CRes.partitionIt(actors, left, right, n3);
            CRes.recQuickSort(actors, left, n4 - 1);
            CRes.recQuickSort(actors, n4 + 1, right);
        }
        catch (Exception exception) {
            mSystem.println("loi Cres 1");
            exception.printStackTrace();
        }
    }

    private static int partitionIt(mVector actors, int left, int right, int pivot) {
        int n4 = left - 1;
        int n5 = right;
        try {
            while (true) {
                if (((MainObject)actors.elementAt((int)(++n4))).ySort < pivot) {
                    continue;
                }
                while (n5 > 0 && ((MainObject)actors.elementAt((int)(--n5))).ySort > pivot) {
                }
                if (n4 >= n5) break;
                CRes.swap(actors, n4, n5);
            }
            CRes.swap(actors, n4, right);
        }
        catch (Exception exception) {
            mSystem.println("loi Cres 2");
            exception.printStackTrace();
        }
        return n4;
    }

    private static void swap(mVector actors, int dex1, int dex2) {
        Object object = actors.elementAt(dex2);
        if (((MainObject)actors.elementAt((int)dex2)).ySort != ((MainObject)actors.elementAt((int)dex1)).ySort) {
            actors.setElementAt(actors.elementAt(dex1), dex2);
            actors.setElementAt(object, dex1);
        }
    }

    public static void quickSort1(mVector actors) {
        CRes.recQuickSort1(actors, 0, actors.size() - 1);
    }

    private static void recQuickSort1(mVector actors, int left, int right) {
        try {
            if (right - left <= 0) {
                return;
            }
            MainItemMap mainItemMap = (MainItemMap)actors.elementAt(right);
            int n3 = mainItemMap.y;
            int n4 = CRes.partitionIt1(actors, left, right, n3);
            CRes.recQuickSort1(actors, left, n4 - 1);
            CRes.recQuickSort1(actors, n4 + 1, right);
        }
        catch (Exception exception) {
            mSystem.println("loi Cres 1");
            exception.printStackTrace();
        }
    }

    private static int partitionIt1(mVector actors, int left, int right, int pivot) {
        int n4 = left - 1;
        int n5 = right;
        try {
            while (true) {
                if (((MainItemMap)actors.elementAt((int)(++n4))).y < pivot) {
                    continue;
                }
                while (n5 > 0 && ((MainItemMap)actors.elementAt((int)(--n5))).y > pivot) {
                }
                if (n4 >= n5) break;
                CRes.swap1(actors, n4, n5);
            }
            CRes.swap1(actors, n4, right);
        }
        catch (Exception exception) {
            mSystem.println("loi Cres 2");
            exception.printStackTrace();
        }
        return n4;
    }

    private static void swap1(mVector actors, int dex1, int dex2) {
        Object object = actors.elementAt(dex2);
        if (((MainItemMap)actors.elementAt((int)dex2)).y != ((MainItemMap)actors.elementAt((int)dex1)).y) {
            actors.setElementAt(actors.elementAt(dex1), dex2);
            actors.setElementAt(object, dex1);
        }
    }

    public static void quickSort(MainItemMap[] actors) {
        CRes.recQuickSort(actors, 0, actors.length - 1);
    }

    private static void recQuickSort(MainItemMap[] actors, int left, int right) {
        try {
            if (right - left <= 0) {
                return;
            }
            int n3 = actors[right].y;
            int n4 = CRes.partitionIt(actors, left, right, n3);
            CRes.recQuickSort(actors, left, n4 - 1);
            CRes.recQuickSort(actors, n4 + 1, right);
        }
        catch (Exception exception) {
            mSystem.println("loi Cres 3");
            exception.printStackTrace();
        }
    }

    private static int partitionIt(MainItemMap[] actors, int left, int right, int pivot) {
        int n4 = left - 1;
        int n5 = right;
        try {
            while (true) {
                if (actors[++n4].y < pivot) {
                    continue;
                }
                while (n5 > 0 && actors[--n5].y > pivot) {
                }
                if (n4 >= n5) break;
                CRes.swap(actors, n4, n5);
            }
            CRes.swap(actors, n4, right);
        }
        catch (Exception exception) {
            mSystem.println("LOI PAINT partitionIt TRONG UTIL");
            exception.printStackTrace();
        }
        return n4;
    }

    private static void swap(MainItemMap[] actors, int dex1, int dex2) {
        MainItemMap mainItemMap = actors[dex2];
        if (actors[dex2].y != actors[dex1].y) {
            actors[dex2] = actors[dex1];
            actors[dex1] = mainItemMap;
        }
    }

    public static MainInfoItem[] selectionSort(MainInfoItem[] arr) {
        int n = arr.length;
        for (int i = 0; i < n - 1; ++i) {
            int n2 = i;
            for (int j = i + 1; j < n; ++j) {
                if (arr[j].id >= arr[n2].id) continue;
                n2 = j;
            }
            if (n2 == i) continue;
            MainInfoItem mainInfoItem = arr[i];
            arr[i] = arr[n2];
            arr[n2] = mainInfoItem;
        }
        return arr;
    }

    public static mVector selectionSortIDSkill(mVector arr) {
        int n = arr.size();
        for (int i = 0; i < n - 1; ++i) {
            int n2 = i;
            for (int j = i + 1; j < n; ++j) {
                if (((Skill)arr.elementAt((int)j)).Id >= ((Skill)arr.elementAt((int)n2)).Id) continue;
                n2 = j;
            }
            if (n2 == i) continue;
            CRes.swapSkill(arr, i, n2);
        }
        return arr;
    }

    public static mVector selectionSortSkill(mVector arr) {
        int n = arr.size();
        for (int i = 0; i < n - 1; ++i) {
            int n2 = i;
            for (int j = i + 1; j < n; ++j) {
                if (((Skill)arr.elementAt((int)j)).lvMin >= ((Skill)arr.elementAt((int)n2)).lvMin) continue;
                n2 = j;
            }
            if (n2 == i) continue;
            CRes.swapSkill(arr, i, n2);
        }
        return arr;
    }

    private static void swapSkill(mVector actors, int dex1, int dex2) {
        Object object = actors.elementAt(dex2);
        if (((Skill)actors.elementAt((int)dex2)).lvMin != ((Skill)actors.elementAt((int)dex1)).lvMin) {
            actors.setElementAt(actors.elementAt(dex1), dex2);
            actors.setElementAt(object, dex1);
        }
    }

    public static mVector selectionSortInven(mVector arr) {
        int n = arr.size();
        for (int i = 0; i < n - 1; ++i) {
            int n2 = i;
            for (int j = i + 1; j < n; ++j) {
                if (((MainItem)arr.elementAt((int)j)).IndexSort >= ((MainItem)arr.elementAt((int)n2)).IndexSort) continue;
                n2 = j;
            }
            if (n2 == i) continue;
            CRes.swapItem(arr, i, n2);
        }
        return arr;
    }

    private static void swapItem(mVector actors, int dex1, int dex2) {
        Object object = actors.elementAt(dex2);
        actors.setElementAt(actors.elementAt(dex1), dex2);
        actors.setElementAt(object, dex1);
    }

    public static int readSignByte(InputStream iss) {
        byte[] byArray = new byte[1];
        try {
            iss.read(byArray, 0, 1);
        }
        catch (IOException iOException) {
            iOException.printStackTrace();
            mSystem.outloi("loi Cres 5");
        }
        return byArray[0];
    }

    public static void setRemoveCharPartInfo() {
        if (charPartInfo == null) {
            return;
        }
        for (int i = 0; i < charPartInfo.length; ++i) {
            for (int j = 0; j < charPartInfo[i].length; ++j) {
                if (charPartInfo[i][j] == null || (GameCanvas.timeNow - CRes.charPartInfo[i][j].timeRemove) / 1000L <= 60L) continue;
                CRes.charPartInfo[i][j] = null;
            }
        }
    }

    public static synchronized CharPartInfo getCharPartInfo(int type, int id) {
        if (charPartInfo[type][id] == null) {
            CRes.charPartInfo[type][id] = new CharPartInfo((byte)type, (short)id);
        }
        return charPartInfo[type][id];
    }

    public static void getImgWeaPone(int claz, int i, int j) {
        block18: {
            try {
                InputStream inputStream;
                Object object;
                Object object2;
                MainObject.imgWeapone[claz][i][j] = new WeaponInfo();
                if (mSystem.isImgLocal) {
                    object2 = new String[]{"kiem/", "songkiem/", "phapsu/", "sung/"};
                    object = null;
                    try {
                        object = mImage.createImage("/weapon/" + object2[claz] + i + ".img");
                    }
                    catch (Exception exception) {
                        // empty catch block
                    }
                    if (object != null && ((mImage)object).image != null) {
                        inputStream = null;
                        try {
                            inputStream = mImage.openFile("/weapon/" + object2[claz] + i + "_data");
                        }
                        catch (Exception exception) {
                            // empty catch block
                        }
                        if (inputStream != null) {
                            MainObject.imgWeapone[claz][i][j].img = object;
                            for (int i = 0; i < 4; ++i) {
                                for (int j = 0; j < 3; ++j) {
                                    MainObject.imgWeapone[claz][i][j].mPos[i][j][0] = (byte)inputStream.read();
                                    MainObject.imgWeapone[claz][i][j].mPos[i][j][1] = (byte)inputStream.read();
                                }
                                MainObject.imgWeapone[claz][i][j].mRegion[i][0] = (byte)inputStream.read();
                                MainObject.imgWeapone[claz][i][j].mRegion[i][1] = (byte)inputStream.read();
                            }
                            MainObject.imgWeapone[claz][i][j].himg = mImage.getImageHeight(MainObject.imgWeapone[claz][i][j].img.image);
                            return;
                        }
                    }
                }
                if ((object2 = (String[])hashWeapon.get(claz + "_" + i)) == null) {
                    object2 = new MainImageDataPartChar();
                    hashWeapon.put(claz + "_" + i, object2);
                    object2.timeImageNull = GameCanvas.timeNow;
                    CRes.getFromRms((byte)claz, (short)i);
                    MainObject.imgWeapone[claz][i][j] = null;
                    break block18;
                }
                if (object2.img != null) {
                    MainObject.imgWeapone[claz][i][j].img = object2.img;
                    inputStream = new ByteArrayInputStream(object2.isData);
                    object = new DataInputStream(inputStream);
                    FilePack.reset();
                    try {
                        for (int i = 0; i < 4; ++i) {
                            for (int j = 0; j < 3; ++j) {
                                MainObject.imgWeapone[claz][i][j].mPos[i][j][0] = (byte)((InputStream)object).read();
                                MainObject.imgWeapone[claz][i][j].mPos[i][j][1] = (byte)((InputStream)object).read();
                            }
                            MainObject.imgWeapone[claz][i][j].mRegion[i][0] = (byte)((InputStream)object).read();
                            MainObject.imgWeapone[claz][i][j].mRegion[i][1] = (byte)((InputStream)object).read();
                        }
                        MainObject.imgWeapone[claz][i][j].himg = mImage.getImageHeight(MainObject.imgWeapone[claz][i][j].img.image);
                    }
                    catch (Exception exception) {
                        // empty catch block
                    }
                    hashWeapon.remove(object2);
                    break block18;
                }
                if ((GameCanvas.timeNow - object2.timeImageNull) / 1000L >= 15L) {
                    CRes.getFromRms((byte)claz, (short)i);
                }
            }
            catch (Exception exception) {
                exception.printStackTrace();
                mSystem.println("loi Cres 7");
            }
        }
        FilePack.reset();
    }

    public static void getFromRms(byte type, short id) {
        MainImageDataPartChar mainImageDataPartChar = (MainImageDataPartChar)hashWeapon.get(type + "_" + id);
        if (mainImageDataPartChar != null) {
            mainImageDataPartChar.timeImageNull = GameCanvas.timeNow;
        }
        GlobalService.gI().load_image_data_part_char((byte)(type + 50), id);
    }

    public static WPSplashInfo GetWPSplashInfo(int claz, int i, int j) {
        try {
            String[] stringArray = new String[]{"kiem/", "songkiem/", "phapsu/", "sung/"};
            CRes.wpSplashInfos[claz][i][j] = new WPSplashInfo();
            CRes.wpSplashInfos[claz][i][j].image = mImage.createImage("/wps/" + stringArray[claz] + i + ".img");
            DataInputStream dataInputStream = mImage.openFile("/wps/" + stringArray[claz] + i + "_data");
            if (dataInputStream != null) {
                for (int i = 0; i < 4; ++i) {
                    for (int j = 0; j < 8; ++j) {
                        CRes.wpSplashInfos[claz][i][j].P0_X[i][j] = ((InputStream)dataInputStream).read();
                        CRes.wpSplashInfos[claz][i][j].P0_Y[i][j] = ((InputStream)dataInputStream).read();
                        CRes.wpSplashInfos[claz][i][j].PF_X[i][j] = CRes.readSignByte(dataInputStream);
                        CRes.wpSplashInfos[claz][i][j].PF_Y[i][j] = CRes.readSignByte(dataInputStream);
                        CRes.wpSplashInfos[claz][i][j].PF_W[i][j] = ((InputStream)dataInputStream).read();
                        CRes.wpSplashInfos[claz][i][j].PF_H[i][j] = ((InputStream)dataInputStream).read();
                    }
                }
            }
            FilePack.reset();
        }
        catch (IOException iOException) {
            CRes.wpSplashInfos[claz][i][j] = null;
            mSystem.println("loi Cres 8");
            iOException.printStackTrace();
        }
        return wpSplashInfos[claz][i][j];
    }

    public static WeaponInfo loadImgWeaPone(int i, int j, int index) {
        if (MainObject.imgWeapone[i][j][index] == null) {
            Load_Data_And_Img.load.loadImgWeaPone(i, j, index);
        }
        return MainObject.imgWeapone[i][j][index];
    }

    public static WPSplashInfo loadWpsPlash(int claz, int i, int j) {
        if (wpSplashInfos[i][j] == null) {
            Load_Data_And_Img.load.loadWpsPlash(i, j);
        }
        return wpSplashInfos[claz][i][j];
    }

    public static byte[] encoding(byte[] array) {
        if (array != null) {
            for (int i = 0; i < array.length; ++i) {
                array[i] = ~array[i];
            }
        }
        return array;
    }

    public static void saveRMS(String string, byte[] byArray) throws Exception {
        TemMidlet.saveRMS(string, byArray);
    }

    public static byte[] loadRMS(String name) {
        return TemMidlet.loadRMS(name);
    }

    public static String loadRMSString(String string) {
        byte[] byArray = CRes.loadRMS(string);
        if (byArray == null) {
            return null;
        }
        try {
            String string2 = new String(byArray, "UTF-8");
            return string2;
        }
        catch (Exception exception) {
            return new String(byArray);
        }
    }

    public static void saveRMSString_(String string, String string2) {
        try {
            CRes.saveRMS(string, string2.getBytes("UTF-8"));
        }
        catch (Exception exception) {
            exception.printStackTrace();
        }
    }

    public static void saveRMSName(byte by, byte[] byArray) throws Exception {
        GlobalService.gI().Save_RMS_Server((byte)0, by, byArray);
    }

    public static void saveRMSString(String string, String string2) throws Exception {
        try {
            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            DataOutputStream dataOutputStream = new DataOutputStream(byteArrayOutputStream);
            dataOutputStream.writeUTF(string2);
            CRes.saveRMS(string, byteArrayOutputStream.toByteArray());
            dataOutputStream.close();
        }
        catch (Exception exception) {
            // empty catch block
        }
    }

    public static boolean ktvc(int x1, int xw1, int x2, int xw2, int y1, int yh1, int y2, int yh2) {
        return x1 <= xw2 && xw1 >= x2 && y1 <= yh2 && yh1 >= y2;
    }

    public static boolean CheckDelRMS(String str) {
        return str.compareTo("isLowDevice") != 0 && str.compareTo("isQty") != 0 && str.compareTo("user_pass") != 0 && str.compareTo("isIndexPart") != 0 && str.compareTo("isIndexServer") != 0;
    }

    public static int rnd(int n, int n2) {
        int n3 = r.nextInt(2);
        if (n3 == 0) {
            return n;
        }
        return n2;
    }

    public static int rnd(int n) {
        return r.nextInt(n);
    }

    public static void load_TaiTho() {
        try {
            byte[] byArray = CRes.loadRMS("taitho");
            if (byArray != null) {
                ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(byArray);
                DataInputStream dataInputStream = new DataInputStream(byteArrayInputStream);
                PaintInfoGameScreen.taitho = dataInputStream.readByte();
                PaintInfoGameScreen.xPointMove = PaintInfoGameScreen.taitho == 0 ? 55 : 85;
            }
        }
        catch (Exception exception) {
            // empty catch block
        }
    }

    public static void save_TaiTho() {
        try {
            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            DataOutputStream dataOutputStream = new DataOutputStream(byteArrayOutputStream);
            dataOutputStream.writeByte(PaintInfoGameScreen.taitho);
            CRes.saveRMS("taitho", byteArrayOutputStream.toByteArray());
        }
        catch (Exception exception) {
            // empty catch block
        }
    }

    public static void saveID_REGION() {
        try {
            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            DataOutputStream dataOutputStream = new DataOutputStream(byteArrayOutputStream);
            dataOutputStream.writeByte(mSystem.ID_REGION);
            CRes.saveRMS(RMS_REGION, byteArrayOutputStream.toByteArray());
        }
        catch (Exception exception) {
            // empty catch block
        }
    }

    static {
        int n;
        sin = new short[]{0, 18, 36, 54, 71, 89, 107, 125, 143, 160, 178, 195, 213, 230, 248, 265, 282, 299, 316, 333, 350, 367, 384, 400, 416, 433, 449, 465, 481, 496, 512, 527, 543, 558, 573, 587, 602, 616, 630, 644, 658, 672, 685, 698, 711, 724, 737, 749, 761, 773, 784, 796, 807, 818, 828, 839, 849, 859, 868, 878, 887, 896, 904, 912, 920, 928, 935, 943, 949, 956, 962, 968, 974, 979, 984, 989, 994, 998, 1002, 1005, 1008, 1011, 1014, 1016, 1018, 1020, 1022, 1023, 1023, 1024, 1024};
        hashWeapon = new mHashtable();
        RMS_REGION = "region_Rms";
        cos = new short[91];
        tan = new int[91];
        for (n = 0; n <= 90; ++n) {
            CRes.cos[n] = sin[90 - n];
            CRes.tan[n] = cos[n] == 0 ? Integer.MAX_VALUE : (sin[n] << 10) / cos[n];
        }
        value = 1;
        r = new Random();
        charPartInfo = new CharPartInfo[7][];
        for (n = 0; n < charPartInfo.length; ++n) {
            CRes.charPartInfo[n] = new CharPartInfo[256];
        }
        wpSplashInfos = new WPSplashInfo[4][5][3];
    }
}

