/*
 * Decompiled with CFR 0.152.
 */
package InterfaceComponents;

import CLib.mFont;
import CLib.mGraphics;
import CLib.mSound;
import GameScreen.PaintInfoGameScreen;
import InterfaceComponents.ChatTextField;
import InterfaceComponents.IAction;
import Main.GameCanvas;
import Model.AvMain;
import Model.FrameImage;

public class iCommand {
    public String caption;
    public String clickCaption = "";
    public String[] subCaption;
    public IAction action;
    public AvMain Pointer;
    public byte indexMenu;
    public byte subIndex = (byte)-1;
    public boolean isSelect = false;
    int nframe;
    int beginframe;
    FrameImage fraImgCaption;
    FrameImage fraImageCmd;
    int wimgCaption;
    int himgCaption;
    int wstr;
    int wimgCmd;
    int himgCmd;
    public int xCmd = -1;
    public int yCmd = -1;
    public int frameCmd;
    public int xdich;
    public int ydich;
    public static int wButtonCmd = 70;
    public static int hButtonCmd = 24;
    public int IdGiaotiep = 0;
    public Object obj;
    public boolean isButtonDai = false;

    public iCommand(String caption, IAction type) {
        this.caption = caption;
        this.action = type;
    }

    public iCommand(String caption, int type) {
        this.caption = caption;
        this.indexMenu = (byte)type;
    }

    public iCommand(String caption, int type, AvMain subIndex) {
        this.caption = caption;
        this.indexMenu = (byte)type;
        this.Pointer = subIndex;
    }

    public iCommand(String caption, int type, int subType, AvMain pointer) {
        this.caption = caption;
        this.indexMenu = (byte)type;
        this.subIndex = (byte)subType;
        this.Pointer = pointer;
    }

    public iCommand(String caption, int type, int subIndex) {
        this.caption = caption;
        this.indexMenu = (byte)type;
        this.subIndex = (byte)subIndex;
    }

    public iCommand(String caption, int type, Object subType, AvMain pointer) {
        this.caption = caption;
        this.indexMenu = (byte)type;
        this.Pointer = pointer;
        this.obj = subType;
    }

    public void setFraCaption(FrameImage fra) {
        this.fraImgCaption = fra;
        this.wimgCaption = this.fraImgCaption.frameWidth;
        this.himgCaption = this.fraImgCaption.frameHeight;
        this.nframe = this.fraImgCaption.nFrame;
        this.beginframe = 0;
        this.wstr = GameCanvas.isSmallScreen ? mFont.tahoma_7_white.getWidth(this.caption) : mFont.tahoma_7b_white.getWidth(this.caption);
    }

    public void setFraCaption(FrameImage fra, int nframe, int beginframe) {
        this.fraImgCaption = fra;
        this.wimgCaption = this.fraImgCaption.frameWidth;
        this.himgCaption = this.fraImgCaption.frameHeight;
        this.nframe = nframe;
        this.beginframe = beginframe;
        this.wstr = GameCanvas.isSmallScreen ? mFont.tahoma_7_white.getWidth(this.caption) : mFont.tahoma_7b_white.getWidth(this.caption);
    }

    public void setPosImg(int x, int y, FrameImage fra, int nframe, int beginframe) {
        this.xCmd = x;
        this.yCmd = y;
        this.fraImgCaption = fra;
        this.wimgCaption = this.fraImgCaption.frameWidth;
        this.himgCaption = this.fraImgCaption.frameHeight;
        this.nframe = nframe;
        this.beginframe = beginframe;
        if (this.fraImageCmd != null) {
            this.wimgCmd = this.fraImageCmd.frameWidth;
            this.himgCmd = this.fraImageCmd.frameHeight;
            if (this.wimgCmd < 28) {
                this.wimgCmd = 28;
            }
            if (this.himgCmd < 28) {
                this.himgCmd = 28;
            }
        } else {
            this.wimgCmd = 70;
            this.himgCmd = hButtonCmd;
        }
    }

    public void setPos(int x, int y, FrameImage fra, String caption) {
        this.caption = caption;
        this.xCmd = x;
        this.yCmd = y;
        this.fraImageCmd = fra;
        if (this.fraImageCmd != null) {
            this.wimgCmd = this.fraImageCmd.frameWidth;
            this.himgCmd = this.fraImageCmd.frameHeight;
            if (this.wimgCmd < 28) {
                this.wimgCmd = 28;
            }
            if (this.himgCmd < 28) {
                this.himgCmd = 28;
            }
        } else {
            this.wimgCmd = 70;
            this.himgCmd = hButtonCmd;
        }
    }

    public void setPos_BoxText(int x, int y, FrameImage fra, String caption, int wBox, int hBox) {
        this.isButtonDai = true;
        this.caption = caption;
        this.xCmd = x;
        this.yCmd = y;
        this.fraImageCmd = fra;
        if (this.fraImageCmd != null) {
            this.wimgCmd = this.fraImageCmd.frameWidth;
            this.himgCmd = this.fraImageCmd.frameHeight;
            if (this.wimgCmd < 28) {
                this.wimgCmd = 28;
            }
            if (this.himgCmd < 28) {
                this.himgCmd = 28;
            }
        } else {
            this.wimgCmd = wBox;
            this.himgCmd = hBox;
        }
    }

    public void setPos_ShowName(int x, int y, FrameImage fra, String clickcaption, int xdich, int ydich) {
        this.caption = "";
        this.clickCaption = clickcaption;
        this.xCmd = x;
        this.yCmd = y;
        this.xdich = xdich;
        this.ydich = ydich;
        this.fraImageCmd = fra;
        if (this.fraImageCmd != null) {
            this.wimgCmd = this.fraImageCmd.frameWidth;
            this.himgCmd = this.fraImageCmd.frameHeight;
            if (this.wimgCmd < 28) {
                this.wimgCmd = 28;
            }
            if (this.himgCmd < 28) {
                this.himgCmd = 28;
            }
        } else {
            this.wimgCmd = 70;
            this.himgCmd = hButtonCmd;
        }
    }

    public void setPosXY(int x, int y) {
        this.xCmd = x;
        this.yCmd = y;
    }

    public void perform() {
        if (GameCanvas.menu2.isShowMenu && (GameCanvas.menu2.runText == null || GameCanvas.menu2.runText.checkDlgStep())) {
            GameCanvas.menu2.doCloseMenu();
        }
        if (this.action != null) {
            this.action.perform();
            GameCanvas.isPointerSelect = false;
            GameCanvas.clearKeyHold();
            GameCanvas.clearKeyPressed();
            mSound.playSound(41, mSound.volumeSound);
        } else {
            mSound.playSound(41, mSound.volumeSound);
            if (this.Pointer != null) {
                if (this.obj != null) {
                    this.Pointer.commandPointer((int)this.indexMenu, this.obj);
                } else {
                    this.Pointer.commandPointer((int)this.indexMenu, this.subIndex);
                }
            } else {
                if (GameCanvas.currentDialog != null) {
                    GameCanvas.currentDialog.commandTab(this.indexMenu, this.subIndex);
                    GameCanvas.isPointerSelect = false;
                    GameCanvas.clearKeyHold();
                    GameCanvas.clearKeyPressed();
                    return;
                }
                if (ChatTextField.isShow) {
                    ChatTextField.gI().commandTab(this.indexMenu, this.subIndex);
                    return;
                }
                if (GameCanvas.menu2.isShowMenu) {
                    GameCanvas.currentScreen.commandMenu(this.indexMenu, this.subIndex);
                    return;
                }
                if (GameCanvas.subDialog != null) {
                    GameCanvas.subDialog.commandTab(this.indexMenu, this.subIndex);
                    return;
                }
                GameCanvas.currentScreen.commandTab(this.indexMenu, this.subIndex);
                return;
            }
            GameCanvas.isPointerSelect = false;
            GameCanvas.clearKeyHold();
            GameCanvas.clearKeyPressed();
        }
    }

    public void update() {
    }

    public void paint(mGraphics g, int x, int y) {
        if (this.isPosCmd()) {
            if (this.fraImageCmd != null) {
                this.fraImageCmd.drawFrame(this.frameCmd, this.xCmd, this.yCmd, 0, 3, g);
            } else {
                this.paintbutton(g, this.xCmd, this.yCmd);
            }
            this.paintCaptionImage(g, this.xCmd, this.yCmd - 6, 2);
        } else {
            if (this.fraImageCmd != null) {
                this.fraImageCmd.drawFrame(this.frameCmd, x, y, 0, 3, g);
            } else {
                this.paintbutton(g, x, y);
            }
            this.paintCaptionImage(g, x, y - 6, 2);
        }
    }

    public void paintCaptionImage(mGraphics g, int x, int y, int pos) {
        if (this.caption == null) {
            return;
        }
        int n4 = 0;
        if (this.fraImgCaption != null) {
            if (pos == 2) {
                this.fraImgCaption.drawFrame(this.beginframe + GameCanvas.gameTick / 2 % this.nframe, x - this.wimgCaption / 2 - this.wstr / 2, y + this.himgCaption / 2 - 3, 0, 3, g);
                n4 = this.wimgCaption / 2;
            } else if (pos == 0) {
                this.fraImgCaption.drawFrame(this.beginframe + GameCanvas.gameTick / 2 % this.nframe, x + this.wimgCaption / 2, y + this.himgCaption / 2 - 4, 0, 3, g);
                n4 = this.wimgCaption + 6;
            }
        }
        if (!this.isButtonDai) {
            if (GameCanvas.isSmallScreen) {
                mFont.tahoma_7_white.drawString(g, this.caption, x + n4, y, pos, false);
            } else {
                AvMain.Font3dColor(g, this.caption, x + n4, y, pos, (byte)0);
            }
        } else if (GameCanvas.isSmallScreen) {
            mFont.tahoma_7_white.drawString(g, this.caption, x + n4 + this.wimgCmd / 2, y + this.himgCmd / 2 + 1, pos, false);
        } else {
            AvMain.Font3dColor(g, this.caption, x + n4 + this.wimgCmd / 2, y + this.himgCmd / 2 + 1, pos, (byte)0);
        }
    }

    public void paintClickCaption(mGraphics g, int x, int y, int pos) {
        if (this.frameCmd == 1 && this.clickCaption.length() > 0) {
            AvMain.Font3dColor(g, this.clickCaption, x + this.xdich, y + this.ydich, pos, (byte)0);
        }
    }

    public void paintCaptionImageMenu(mGraphics g, int x, int y, int pos) {
        int n4 = 0;
        if (this.fraImgCaption != null) {
            if (pos == 2) {
                this.fraImgCaption.drawFrame(this.beginframe + GameCanvas.gameTick / 2 % this.nframe, x - this.wimgCaption / 2 - this.wstr / 2, y + this.himgCaption / 2, 0, 3, g);
                n4 = this.wimgCaption / 2;
            } else if (pos == 0) {
                this.fraImgCaption.drawFrame(this.beginframe + GameCanvas.gameTick / 2 % this.nframe, x + this.wimgCaption / 2, y + this.himgCaption / 2, 0, 3, g);
                n4 = this.wimgCaption + 6;
            }
        }
        if (GameCanvas.isSmallScreen) {
            mFont.tahoma_7_white.drawString(g, this.caption, x + n4, y, pos, false);
        } else {
            AvMain.Font3dColorAndColor(g, this.caption, x + n4, y, pos, (byte)7, (byte)0);
        }
    }

    public void paintImage(mGraphics g, int x, int y, int pos, int typePaint) {
        if (this.fraImgCaption != null) {
            if (typePaint == 0) {
                this.fraImgCaption.drawFrame(this.beginframe + GameCanvas.gameTick / 2 % this.nframe, x, y + this.himgCaption / 2, 0, pos, g);
            } else if (typePaint == 1) {
                int n5;
                int n6 = n5 = this.beginframe + this.frameCmd == 1 ? 1 : 0;
                if (n5 > this.nframe) {
                    n5 = this.beginframe - 1;
                }
                this.fraImgCaption.drawFrame(n5, x, y + this.himgCaption / 2, 0, pos, g);
            } else {
                this.fraImgCaption.drawFrame(this.beginframe, x, y + this.himgCaption / 2, 0, pos, g);
            }
        }
    }

    public void updatePointer() {
        if (!this.isButtonDai) {
            if (this.isPosCmd()) {
                this.frameCmd = GameCanvas.isPointerDown || GameCanvas.isPointerMove ? (GameCanvas.isPoint(this.xCmd - this.wimgCmd / 2 - 5, this.yCmd - this.himgCmd / 2 - 5, this.wimgCmd + 10, this.himgCmd + 10) ? 1 : 0) : 0;
                if (GameCanvas.isPointSelect(this.xCmd - this.wimgCmd / 2 - 5, this.yCmd - this.himgCmd / 2 - 5, this.wimgCmd + 10, this.himgCmd + 10)) {
                    this.perform();
                    GameCanvas.isPointerSelect = false;
                    this.frameCmd = 0;
                }
            }
        } else if (this.isPosCmd()) {
            if (GameCanvas.isPointerDown || GameCanvas.isPointerMove) {
                if (GameCanvas.isPoint(this.xCmd - 5, this.yCmd - 5, this.wimgCmd + 10, this.himgCmd + 10)) {
                    this.isSelect = true;
                    this.frameCmd = 1;
                } else {
                    this.frameCmd = 0;
                    this.isSelect = false;
                }
            } else {
                this.frameCmd = 0;
                this.isSelect = false;
            }
            if (GameCanvas.isPointSelect(this.xCmd - 5, this.yCmd - 5, this.wimgCmd + 10, this.himgCmd + 10)) {
                this.perform();
                GameCanvas.isPointerSelect = false;
                this.frameCmd = 0;
                this.isSelect = false;
            }
        }
    }

    public void updatePointer(int cmx, int cmy) {
        if (this.isPosCmd()) {
            this.frameCmd = GameCanvas.isPointerDown || GameCanvas.isPointerMove ? (GameCanvas.isPoint(this.xCmd - this.wimgCmd / 2 - 3 - cmx, this.yCmd - this.himgCmd / 2 - 3 - cmy, this.wimgCmd + 6, this.himgCmd + 6) ? 1 : 0) : 0;
            if (GameCanvas.isPointSelect(this.xCmd - this.wimgCmd / 2 - 3 - cmx, this.yCmd - this.himgCmd / 2 - 3 - cmy, this.wimgCmd + 6, this.himgCmd + 6)) {
                this.perform();
                GameCanvas.isPointerSelect = false;
                this.frameCmd = 0;
            }
        }
    }

    public void updatePointerShow(int cmx, int cmy) {
        if (this.isPosCmd()) {
            this.frameCmd = GameCanvas.isPointerDown || GameCanvas.isPointerMove ? (GameCanvas.isPoint(this.xCmd - this.wimgCmd / 2 - 3 - cmx, this.yCmd - this.himgCmd / 2 - 3 - cmy, this.wimgCmd + 6, this.himgCmd + 6) ? 1 : 0) : 0;
        }
    }

    public void paintbutton(mGraphics g, int x, int y) {
        if (GameCanvas.isTouch) {
            if (!this.isButtonDai) {
                PaintInfoGameScreen.fraButton.drawFrame(this.frameCmd, x, y, 0, 3, g);
            } else {
                AvMain.paintDialog(g, x, y, this.wimgCmd, this.himgCmd, this.isSelect ? 1 : 2);
            }
        } else if (!this.isButtonDai) {
            AvMain.paintDialog(g, x - wButtonCmd / 2, y - hButtonCmd / 2, wButtonCmd, hButtonCmd, this.isSelect ? 1 : 2);
        } else {
            AvMain.paintDialog(g, x, y, this.wimgCmd, this.himgCmd, this.isSelect ? 1 : 2);
        }
    }

    public boolean isPosCmd() {
        return this.xCmd > 0 && this.yCmd > 0;
    }
}

