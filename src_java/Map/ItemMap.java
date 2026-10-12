/*
 * Decompiled with CFR 0.152.
 */
package Map;

import CLib.mGraphics;
import GameObjects.ObjectData;
import Main.GameCanvas;
import Map.MainItemMap;
import Model.FrameImage;
import Model.MainImage;

public class ItemMap
extends MainItemMap {
    public static boolean isPaintDieHouseArena = false;
    public static FrameImage[] img_HouseArena_Die;
    public static int[] dx_imgDie;
    public static int[] dy_imgDie;

    public ItemMap(short s, short s2, int n, int n2, int[][] nArray) {
        super(s, s2, n, n2, nArray);
        this.TypeItem = 0;
    }

    public void setInfoItem(int x, int y) {
        this.x = x;
        this.y = y;
    }

    public void paint(mGraphics g) {
        MainImage mainImage = ObjectData.getImagePartItemMap(this.IDImage);
        if (mainImage.img != null) {
            g.drawImage(mainImage.img, this.x + this.dx, this.y + this.dy, 0, false);
        }
    }

    public static void paintDieHouseArena(mGraphics g) {
        if (isPaintDieHouseArena) {
            if (GameCanvas.loadmap.idMap == 58) {
                img_HouseArena_Die[0].drawFrame(0, dx_imgDie[0], dy_imgDie[0], 0, g);
            } else if (GameCanvas.loadmap.idMap == 56) {
                img_HouseArena_Die[1].drawFrame(0, dx_imgDie[1], dy_imgDie[1], 0, g);
            } else if (GameCanvas.loadmap.idMap == 54) {
                img_HouseArena_Die[2].drawFrame(0, dx_imgDie[2], dy_imgDie[2], 0, g);
            } else if (GameCanvas.loadmap.idMap == 60) {
                img_HouseArena_Die[3].drawFrame(0, dx_imgDie[3], dy_imgDie[3], 0, g);
            }
        }
    }

    static {
        dx_imgDie = new int[]{420, 376, 290, 310};
        dy_imgDie = new int[]{188, 447, 426, 105};
    }
}

