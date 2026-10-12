/*
 * Decompiled with CFR 0.152.
 */
package Model;

import CLib.mFont;
import CLib.mGraphics;
import CLib.mImage;
import InterfaceComponents.MainTabNew;
import InterfaceComponents.iCommand;
import Main.GameCanvas;
import Model.FrameImage;

public class AvMain {
    public iCommand left;
    public iCommand right;
    public iCommand center;
    public static mImage[] tab = new mImage[4];
    public static int wimg;
    public static mImage[] imghitScr;
    public static mImage imgtextfield;
    public static mImage imgInfo;
    public static mImage imghpmp;
    public static mImage imgcolorhpmp;
    public static mImage imgPopup;
    public static mImage imgBackInfo;
    public static mImage imgWorldMap;
    public static mImage imgSelect;
    public static mImage imgFocusMap;
    public static mImage imgDelaySkill;
    public static mImage imgLoadImg;
    public static mImage imgEyeDie;
    public static mImage imgHotKey;
    public static mImage imgHotKey2;
    public static mImage imgMess;
    public static mImage imgColorItem;
    public static mImage imgicongt;
    public static mImage imgGlass;
    public static mImage imgLock;
    public static mImage imgRect;
    public static mImage imgSelect_1;
    public static mImage imgcolorhpmp_back;
    public static mImage imgcolorhpSmall_back;
    public static mImage imgcolorhpSmall;
    public static mImage img18Plus;
    public static FrameImage fraPlayerDie;
    public static FrameImage fraObjMiniMap;
    public static FrameImage fraPk;
    public static FrameImage fraStar;
    public static FrameImage fraQuest;
    public static FrameImage fraMonSample;
    public static FrameImage fraDiamond;
    public static FrameImage fraPlayerNo;
    public static FrameImage imgStun;
    public static FrameImage imgSleep;
    public static FrameImage fraFogetPass;
    public static mImage[] textf;
    public static final int SELECTED_COLOR = -6517641;
    public static int[] color;
    static int[] colorDia;

    public void paint(mGraphics g) {
        GameCanvas.resetTrans(g);
        if (GameCanvas.currentDialog == null && !GameCanvas.menu2.isShowMenu) {
            this.paintCmd(g);
        }
    }

    public void update() {
    }

    public void keypress(int keyCode) {
    }

    public void paintCmd(mGraphics g) {
        if (GameCanvas.isSmallScreen) {
            this.paintCmdSmall(g);
        } else {
            if (this.left != null) {
                this.left.paint(g, GameCanvas.wCommand, GameCanvas.h - iCommand.hButtonCmd / 2 - 1);
            }
            if (this.right != null) {
                this.right.paint(g, GameCanvas.w - GameCanvas.wCommand, GameCanvas.h - iCommand.hButtonCmd / 2 - 1);
            }
            if (this.center != null) {
                this.center.paint(g, GameCanvas.hw, GameCanvas.h - iCommand.hButtonCmd / 2 - 1);
            }
        }
    }

    public void paintCmd_OnlyText(mGraphics g) {
        if (GameCanvas.menu2.isShowMenu || GameCanvas.currentDialog != null) {
            return;
        }
        GameCanvas.resetTrans(g);
        if (GameCanvas.isSmallScreen) {
            this.paintCmd_OnlyText_Small(g);
        } else {
            if (this.left != null) {
                AvMain.Font3dWhite(g, this.left.caption, 30, GameCanvas.h - GameCanvas.hCommand / 2 - 4, 2);
            }
            if (this.right != null) {
                AvMain.Font3dWhite(g, this.right.caption, GameCanvas.w - 30, GameCanvas.h - GameCanvas.hCommand / 2 - 4, 2);
            }
            if (this.center != null) {
                AvMain.Font3dWhite(g, this.center.caption, GameCanvas.hw, GameCanvas.h - GameCanvas.hCommand / 2 - 4, 2);
            }
        }
    }

    public void paintCmdSmall(mGraphics g) {
        GameCanvas.resetTrans(g);
        if (this.left != null) {
            this.left.paint(g, GameCanvas.wCommand, GameCanvas.h - iCommand.hButtonCmd / 2 - 1);
        }
        if (this.right != null) {
            this.right.paint(g, GameCanvas.w - GameCanvas.wCommand, GameCanvas.h - iCommand.hButtonCmd / 2 - 1);
        }
        if (this.center != null) {
            this.center.paint(g, GameCanvas.hw, GameCanvas.h - iCommand.hButtonCmd / 2 - 1);
        }
    }

    public void paintCmd_OnlyText_Small(mGraphics g) {
        if (this.left != null) {
            mFont.tahoma_7b_white.drawString(g, this.left.caption, 27, GameCanvas.h - GameCanvas.hCommand / 2 + 1, 2, false);
        }
        if (this.right != null) {
            mFont.tahoma_7b_white.drawString(g, this.right.caption, GameCanvas.w - 27, GameCanvas.h - GameCanvas.hCommand / 2 + 1, 2, false);
        }
        if (this.center != null) {
            mFont.tahoma_7b_white.drawString(g, this.center.caption, GameCanvas.hw, GameCanvas.h - GameCanvas.hCommand / 2 + 1, 2, false);
        }
    }

    public void commandTab(int index, int subIndex) {
    }

    public void commandMenu(int index, int subIndex) {
    }

    public void commandPointer(int index, int subIndex) {
    }

    public void commandPointer(int index, Object subIndex) {
    }

    public void paintRect(mGraphics g, int x, int y, int w, int h, boolean isSelect) {
        g.setColor(0);
        g.fillRect(x, y, w, h, false);
        g.setColor(-15703219);
        if (isSelect) {
            g.setColor(-1);
        }
        g.fillRect(x + 2, y + 2, w - 4, h - 4, false);
    }

    public void paintRect(mGraphics g, int x, int y, int w, int h) {
        g.setColor(0);
        g.fillRect(x, y, w, h, false);
        g.setColor(-15703219);
        g.fillRect(x + 2, y + 2, w - 4, h - 4, false);
    }

    public void paintRect(mGraphics g, int x, int y, int w, int h, int color, int color2) {
        g.setColor(color);
        g.fillRect(x, y, w, h, false);
        g.setColor(color2);
        g.fillRect(x + 2, y + 2, w - 4, h - 4, false);
    }

    public static void paintDialog(mGraphics g, int xDia, int yDia, int wDia, int hDia, int Indexcolor) {
        if (wDia < 35) {
            wDia = 35;
        }
        int n6 = (wDia - 6) / 32;
        int n7 = (hDia - 6) / 32;
        if (hDia % 2 != 0) {
            ++hDia;
        }
        if (GameCanvas.lowGraphic) {
            if (Indexcolor > 2) {
                Indexcolor = Indexcolor == 8 || Indexcolor == 12 ? 4 : 3;
            }
            g.setColor(colorDia[0]);
            g.fillRect(xDia, yDia, wDia, hDia, false);
            g.setColor(colorDia[4]);
            g.drawRect(xDia + 1, yDia + 1, wDia - 3, hDia - 3, false);
            MainTabNew.paintRectLowGraphic(g, xDia + 3, yDia + 3, wDia - 6, hDia - 6, Indexcolor);
        } else {
            if (hDia < 32) {
                for (int i = 0; i <= n6; ++i) {
                    for (int j = 0; j <= n7; ++j) {
                        if (i == n6) {
                            if (j == n7) {
                                g.drawRegion(MainTabNew.imgTab[Indexcolor], 0, 0, 32, hDia, 0, xDia - 3 + wDia - 32, yDia, 0, false);
                                continue;
                            }
                            g.drawRegion(MainTabNew.imgTab[Indexcolor], 0, 0, 32, hDia, 0, xDia - 3 + wDia - 32, yDia + 3 + 32 * j, 0, false);
                            continue;
                        }
                        if (j == n7) {
                            g.drawRegion(MainTabNew.imgTab[Indexcolor], 0, 0, 32, hDia, 0, xDia + 3 + i * 32, yDia, 0, false);
                            continue;
                        }
                        g.drawRegion(MainTabNew.imgTab[Indexcolor], 0, 0, 32, hDia, 0, xDia + 3 + i * 32, yDia + 3 + 32 * j, 0, false);
                    }
                }
            } else {
                for (int i = 0; i <= n6; ++i) {
                    for (int j = 0; j <= n7; ++j) {
                        if (i == n6) {
                            if (j == n7) {
                                g.drawImage(MainTabNew.imgTab[Indexcolor], xDia - 3 + wDia - 32, yDia - 3 + hDia - 32, 0, false);
                                continue;
                            }
                            g.drawImage(MainTabNew.imgTab[Indexcolor], xDia - 3 + wDia - 32, yDia + 3 + 32 * j, 0, false);
                            continue;
                        }
                        if (j == n7) {
                            g.drawImage(MainTabNew.imgTab[Indexcolor], xDia + 3 + i * 32, yDia - 3 + hDia - 32, 0, false);
                            continue;
                        }
                        g.drawImage(MainTabNew.imgTab[Indexcolor], xDia + 3 + i * 32, yDia + 3 + 32 * j, 0, false);
                    }
                }
            }
            g.drawRegion(imgPopup, 0, 0, 5, 5, 0, xDia, yDia, 0, false);
            g.drawRegion(imgPopup, 0, 5, 5, 5, 0, xDia + wDia - 5, yDia, 0, false);
            g.drawRegion(imgPopup, 0, 15, 5, 5, 0, xDia, yDia + hDia - 5, 0, false);
            g.drawRegion(imgPopup, 0, 10, 5, 5, 0, xDia + wDia - 5, yDia + hDia - 5, 0, false);
            g.setColor(colorDia[0]);
            g.fillRect(xDia + 3, yDia, wDia - 6, 1, false);
            g.fillRect(xDia, yDia + 3, 1, hDia - 6, false);
            g.setColor(colorDia[1]);
            g.fillRect(xDia + 3, yDia + 1, wDia - 6, 1, false);
            g.fillRect(xDia + 1, yDia + 3, 1, hDia - 6, false);
            g.setColor(colorDia[2]);
            g.fillRect(xDia + 3, yDia + 2, wDia - 6, 1, false);
            g.fillRect(xDia + 2, yDia + 3, 1, hDia - 6, false);
            g.setColor(colorDia[2]);
            g.fillRect(xDia + 3, yDia + hDia - 1, wDia - 6, 1, false);
            g.fillRect(xDia + wDia - 1, yDia + 3, 1, hDia - 6, false);
            g.setColor(colorDia[4]);
            g.fillRect(xDia + 3, yDia + hDia - 2, wDia - 6, 1, false);
            g.fillRect(xDia + wDia - 2, yDia + 3, 1, hDia - 6, false);
            g.setColor(colorDia[0]);
            g.fillRect(xDia + 3, yDia + hDia - 3, wDia - 6, 1, false);
            g.fillRect(xDia + wDia - 3, yDia + 3, 1, hDia - 6, false);
        }
    }

    public static void paintDialogNew(mGraphics g, int xDia, int yDia, int wDia, int hDia, int Indexcolor) {
        if (GameCanvas.lowGraphic) {
            AvMain.paintDialog(g, xDia, yDia, wDia, hDia, Indexcolor);
        } else {
            int n6 = (wDia - 6) / 32;
            int n7 = (hDia - 6) / 32;
            if (hDia % 2 != 0) {
                ++hDia;
            }
            for (int i = 0; i <= n6; ++i) {
                for (int j = 0; j <= n7; ++j) {
                    if (j > 1 && j < n7 - 1 && i != 0 && i != n6) continue;
                    if (i == n6) {
                        if (j == n7) {
                            g.drawImage(MainTabNew.imgTab[Indexcolor], xDia - 3 + wDia - 32, yDia - 3 + hDia - 32, 0, false);
                            continue;
                        }
                        g.drawImage(MainTabNew.imgTab[Indexcolor], xDia - 3 + wDia - 32, yDia + 3 + 32 * j, 0, false);
                        continue;
                    }
                    if (j == n7) {
                        g.drawImage(MainTabNew.imgTab[Indexcolor], xDia + 3 + i * 32, yDia - 3 + hDia - 32, 0, false);
                        continue;
                    }
                    g.drawImage(MainTabNew.imgTab[Indexcolor], xDia + 3 + i * 32, yDia + 3 + 32 * j, 0, false);
                }
            }
            g.drawRegion(imgPopup, 0, 0, 5, 5, 0, xDia, yDia, 0, false);
            g.drawRegion(imgPopup, 0, 5, 5, 5, 0, xDia + wDia - 5, yDia, 0, false);
            g.drawRegion(imgPopup, 0, 15, 5, 5, 0, xDia, yDia + hDia - 5, 0, false);
            g.drawRegion(imgPopup, 0, 10, 5, 5, 0, xDia + wDia - 5, yDia + hDia - 5, 0, false);
            g.setColor(colorDia[0]);
            g.fillRect(xDia + 3, yDia, wDia - 6, 1, false);
            g.fillRect(xDia, yDia + 3, 1, hDia - 6, false);
            g.setColor(colorDia[1]);
            g.fillRect(xDia + 3, yDia + 1, wDia - 6, 1, false);
            g.fillRect(xDia + 1, yDia + 3, 1, hDia - 6, false);
            g.setColor(colorDia[2]);
            g.fillRect(xDia + 3, yDia + 2, wDia - 6, 1, false);
            g.fillRect(xDia + 2, yDia + 3, 1, hDia - 6, false);
            g.setColor(colorDia[2]);
            g.fillRect(xDia + 3, yDia + hDia - 1, wDia - 6, 1, false);
            g.fillRect(xDia + wDia - 1, yDia + 3, 1, hDia - 6, false);
            g.setColor(colorDia[4]);
            g.fillRect(xDia + 3, yDia + hDia - 2, wDia - 6, 1, false);
            g.fillRect(xDia + wDia - 2, yDia + 3, 1, hDia - 6, false);
            g.setColor(colorDia[0]);
            g.fillRect(xDia + 3, yDia + hDia - 3, wDia - 6, 1, false);
            g.fillRect(xDia + wDia - 3, yDia + 3, 1, hDia - 6, false);
        }
    }

    public static void paintTabNew(mGraphics g, int xTab, int yTab, int wTab, int hTab, boolean ismore, byte colorBack) {
        if (GameCanvas.lowGraphic) {
            AvMain.paintDialog(g, xTab, yTab, wTab, hTab, colorBack);
        } else {
            if (hTab < 32) {
                hTab = 32;
            }
            g.setColor(color[0]);
            g.fillRect(xTab + wimg - 2, yTab + 3, wTab - 2 * wimg + 4, hTab - 5, false);
            g.fillRect(xTab + 4, yTab + wimg - 2, wTab - 8, hTab - 2 * wimg + 4, false);
            g.setColor(color[1]);
            g.fillRect(xTab + wimg - 2, yTab + 4, wTab - 2 * wimg + 4, hTab - 7, false);
            g.fillRect(xTab + 5, yTab + wimg - 2, wTab - 10, hTab - 2 * wimg + 4, false);
            g.setColor(color[0]);
            g.fillRect(xTab + wimg - 2, yTab + 5, wTab - 2 * wimg + 4, hTab - 9, false);
            g.fillRect(xTab + 6, yTab + wimg - 2, wTab - 12, hTab - 2 * wimg + 4, false);
            g.setColor(color[2]);
            g.fillRect(xTab + 7, yTab + 6, wTab - 14, hTab - 12, false);
            for (int i = 0; i <= (wTab - 15) / 32; ++i) {
                for (int j = 0; j <= (hTab - 11) / 32; ++j) {
                    if (i == (wTab - 15) / 32) {
                        if (j == (hTab - 11) / 32) {
                            g.drawImage(MainTabNew.imgTab[colorBack], xTab + wTab - 39, yTab + hTab - 37, 0, false);
                            continue;
                        }
                        g.drawImage(MainTabNew.imgTab[colorBack], xTab + wTab - 39, yTab + 7 + j * 32, 0, false);
                        continue;
                    }
                    if (j == (hTab - 11) / 32) {
                        g.drawImage(MainTabNew.imgTab[colorBack], xTab + 8 + i * 32, yTab + hTab - 37, 0, false);
                        continue;
                    }
                    g.drawImage(MainTabNew.imgTab[colorBack], xTab + 8 + i * 32, yTab + 7 + j * 32, 0, false);
                }
            }
            g.drawImage(tab[0], xTab, yTab, 0, false);
            g.drawRegion(tab[0], 0, 0, wimg, wimg, 2, xTab + wTab - wimg, yTab, 0, false);
            g.drawImage(tab[1], xTab + 2, yTab + hTab - wimg, 0, false);
            g.drawRegion(tab[1], 0, 0, 30, 30, 2, xTab + wTab - 32, yTab + hTab - wimg, 0, false);
            if (ismore) {
                g.drawImage(tab[2], xTab + wTab / 2, yTab + 2, 3, false);
            }
        }
    }

    public static void paintRectNice(mGraphics g, int xTab, int yTab, int wTab, int hTab, byte colorBack) {
        if (hTab % 2 == 1) {
            ++hTab;
        }
        if (GameCanvas.lowGraphic) {
            if (colorBack > 2) {
                colorBack = colorBack == 8 || colorBack == 12 ? (byte)4 : (byte)3;
            }
            MainTabNew.paintRectLowGraphic(g, xTab, yTab, wTab, hTab, colorBack);
        } else {
            for (int i = 0; i <= wTab / 32; ++i) {
                for (int j = 0; j <= hTab / 32; ++j) {
                    if (i == wTab / 32) {
                        if (j == hTab / 32) {
                            g.drawImage(MainTabNew.imgTab[colorBack], xTab + wTab - 32, yTab + hTab - 32, 0, false);
                            continue;
                        }
                        g.drawImage(MainTabNew.imgTab[colorBack], xTab + wTab - 32, yTab + j * 32, 0, false);
                        continue;
                    }
                    if (j == hTab / 32) {
                        g.drawImage(MainTabNew.imgTab[colorBack], xTab + i * 32, yTab + hTab - 32, 0, false);
                        continue;
                    }
                    g.drawImage(MainTabNew.imgTab[colorBack], xTab + i * 32, yTab + j * 32, 0, false);
                }
            }
        }
    }

    public static void fill(int x, int y, int w, int h, int color, mGraphics g) {
        g.setColor(color);
        g.fillRect(x, y, w, h, false);
    }

    public static void paintRectText(mGraphics g, int xText, int yText, int wText, int hText, boolean isFocus) {
        g.setColor(-4155296);
        if (isFocus) {
            g.setColor(-132648);
        }
        g.drawRegion(imgtextfield, 0, isFocus ? 30 : 0, 4, 15, 0, xText, yText, 0, true);
        g.drawRegion(imgtextfield, 0, (isFocus ? 30 : 0) + 15, 4, 15, 0, xText + wText - 4, yText + hText - 15, 0, true);
        g.drawRegion(imgtextfield, 0, (isFocus ? 30 : 0) + 11, 4, 4, 0, xText, yText + hText - 4, 0, true);
        g.drawRegion(imgtextfield, 0, (isFocus ? 30 : 0) + 15, 4, 4, 0, xText + wText - 4, yText, 0, true);
        g.fillRect(xText + 4, yText, wText - 8, hText, true);
        g.fillRect(xText, yText + 4, wText, hText - 8, true);
    }

    public void paintSelect(mGraphics g, int x, int y, int w, int h) {
        g.setColor(-6517641);
        g.fillRect(x, y, w, h, false);
    }

    public void paintFormList(mGraphics g, int x, int y, int w, int h, String name) {
        AvMain.paintDialogNew(g, x - 6, y - 6, w + 12, h + 12, 0);
        AvMain.paintRectNice(g, x, y + GameCanvas.hCommand, w, h - GameCanvas.hCommand, (byte)2);
        MainTabNew.paintNameItem(g, x + w / 2, y + GameCanvas.hCommand / 4, w, name, 7);
    }

    public int resetSelect(int select, int max, boolean isreset) {
        if (select < 0) {
            select = isreset ? max : 0;
        } else if (select > max) {
            select = isreset ? 0 : max;
        }
        return select;
    }

    public void updatekey() {
        if (GameCanvas.keyMyHold[5]) {
            if (this.center != null) {
                GameCanvas.clearKeyPressed(5);
                GameCanvas.clearKeyHold(5);
                this.center.perform();
            }
        } else if (GameCanvas.keyMyHold[12]) {
            if (this.left != null) {
                GameCanvas.clearKeyPressed(12);
                GameCanvas.clearKeyHold(12);
                this.left.perform();
            }
        } else if (GameCanvas.keyMyHold[13] && this.right != null) {
            GameCanvas.clearKeyPressed(13);
            GameCanvas.clearKeyHold(13);
            this.right.perform();
        }
    }

    public void updatePointer() {
        if (GameCanvas.isTouch) {
            if (this.left != null) {
                if (this.left.isPosCmd()) {
                    this.left.updatePointer();
                } else if (GameCanvas.isPointSelect(0, GameCanvas.h - GameCanvas.hCommand - 5, GameCanvas.wCommand * 2, GameCanvas.hCommand + 10)) {
                    this.left.perform();
                }
            }
            if (this.right != null) {
                if (this.right.isPosCmd()) {
                    this.right.updatePointer();
                } else if (GameCanvas.isPointSelect(GameCanvas.w - GameCanvas.wCommand * 2, GameCanvas.h - GameCanvas.hCommand - 5, GameCanvas.wCommand * 2, GameCanvas.hCommand + 10)) {
                    this.right.perform();
                }
            }
            if (this.center != null) {
                if (this.center.isPosCmd()) {
                    this.center.updatePointer();
                } else if (GameCanvas.isPointSelect(GameCanvas.hw - GameCanvas.wCommand, GameCanvas.h - GameCanvas.hCommand - 5, GameCanvas.wCommand * 2, GameCanvas.hCommand + 10)) {
                    this.center.perform();
                }
            }
        }
    }

    public static void Font3dWhite(mGraphics g, String str, int x, int y, int ar) {
        mFont.tahoma_7b_black.drawString(g, str, x + 1, y + 1, ar, true);
        mFont.tahoma_7b_white.drawString(g, str, x, y, ar, true);
    }

    public static void Font3dColor(mGraphics g, String str, int x, int y, int ar, byte color) {
        mFont.tahoma_7b_black.drawString(g, str, x + 1, y + 1, ar, true);
        MainTabNew.setTextColorName(color).drawString(g, str, x, y, ar, true);
    }

    public static void Font3dColorAndColor(mGraphics g, String str, int x, int y, int ar, byte color, byte color2) {
        MainTabNew.setTextColorName(color).drawString(g, str, x + 1, y + 1, ar, true);
        MainTabNew.setTextColorName(color2).drawString(g, str, x, y, ar, true);
    }

    public static void FontBorderColor(mGraphics g, String str, int x, int y, int ar, int color) {
        if (color == 2) {
            mFont.tahoma_7b_black.drawString(g, str, x + 1, y + 1, ar, true);
        } else {
            mFont.tahoma_7b_black.drawString(g, str, x - 1, y - 1, ar, true);
            mFont.tahoma_7b_black.drawString(g, str, x - 1, y + 1, ar, true);
            mFont.tahoma_7b_black.drawString(g, str, x + 1, y - 1, ar, true);
            mFont.tahoma_7b_black.drawString(g, str, x + 1, y + 1, ar, true);
            mFont.tahoma_7b_black.drawString(g, str, x - 1, y, ar, true);
            mFont.tahoma_7b_black.drawString(g, str, x + 1, y, ar, true);
            mFont.tahoma_7b_black.drawString(g, str, x, y - 1, ar, true);
            mFont.tahoma_7b_black.drawString(g, str, x, y + 1, ar, true);
        }
        MainTabNew.setTextColorName(color).drawString(g, str, x, y, ar, false);
    }

    static {
        imghitScr = new mImage[3];
        textf = new mImage[2];
        color = new int[]{-12052464, -477112, -6778760, -2043720, -6649744};
        colorDia = new int[]{-8902126, -460632, -11467264, -477112, -477112};
    }
}

