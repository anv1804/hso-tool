/*
 * Decompiled with CFR 0.152.
 */
package InterfaceComponents;

import CLib.TField;
import CLib.mFont;
import CLib.mGraphics;
import CLib.mSystem;
import CLib.mVector;
import GameScreen.GameScreen;
import GameScreen.MainScreen;
import GameScreen.PaintInfoGameScreen;
import InterfaceComponents.ListNew;
import InterfaceComponents.MainDialog;
import InterfaceComponents.iCommand;
import Main.GameCanvas;
import Model.T;
import netcommand.global.GlobalService;

public class InputDialog
extends MainDialog {
    public TField tfInput;
    iCommand cmdClose;
    TField[] mtfInput;
    boolean isMore = false;
    boolean iscroll;
    boolean isNew = false;
    long price;
    String name;
    String info;
    String xuluong;
    short type = 0;
    short idNPC = (short)-1;
    static int hTouch = 0;
    static int istouchMore = 0;
    static int hitem;
    mFont fontInput = mFont.tahoma_7_white;
    ListNew list;
    String strtam;
    int idSelect = 0;

    public InputDialog() {
        this.cmdClose = new iCommand(T.close, -1);
        istouchMore = 15;
        if (GameCanvas.isTouch) {
            hTouch = iCommand.hButtonCmd + 5;
        }
    }

    public void closeDialog() {
        if (GameCanvas.currentDialog != null) {
            GameCanvas.currentDialog = null;
        } else {
            GameCanvas.subDialog = null;
        }
    }

    public void commandTab(int index, int subIndex) {
        switch (index) {
            case -1: {
                this.closeDialog();
                break;
            }
            case 0: {
                String[] stringArray = new String[this.mtfInput.length];
                for (int i = 0; i < stringArray.length; ++i) {
                    stringArray[i] = "";
                    if (this.mtfInput[i].getText().length() <= 0) continue;
                    stringArray[i] = this.mtfInput[i].getText();
                }
                GlobalService.gI().nap_tien(this.type, stringArray);
                GameCanvas.start_Wait_Dialog(T.pleaseWait, new iCommand(T.close, -1));
                break;
            }
            case 1: {
                mVector mVector2 = new mVector();
                boolean bl = this.idNPC == GameScreen.ID_DLG_BUY_KC;
                for (int i = 0; i < this.mtfInput.length; ++i) {
                    String string = "";
                    if (this.mtfInput[i].getText().length() > 0) {
                        string = this.mtfInput[i].getText();
                    }
                    mVector2.addElement(string);
                }
                if (bl) {
                    mVector2.addElement(mSystem.getLong());
                    mVector2.addElement(mSystem.getLat());
                }
                GlobalService.gI().sendMoreServerInfo(this.idNPC, this.type, mVector2);
                this.closeDialog();
            }
        }
        super.commandTab(index, subIndex);
    }

    public void setinfo(String info, iCommand cmd, boolean isNum, String nameInput) {
        this.isMore = false;
        this.isNew = false;
        this.left = null;
        this.right = null;
        this.center = null;
        if (cmd == null) {
            GameCanvas.currentDialog = null;
        }
        this.wDia = GameCanvas.w - 30;
        if (this.wDia > 200) {
            this.wDia = 200;
        }
        this.strinfo = this.fontInput.splitFontArray(info, this.wDia - 20);
        this.name = nameInput;
        this.hDia = 15 * this.strinfo.length + 10 + TField.getHeight() + GameCanvas.hCommand;
        this.hDia += hTouch + istouchMore;
        this.xDia = GameCanvas.hw - this.wDia / 2;
        this.yDia = GameCanvas.h - GameCanvas.hCommand * 2 - this.hDia + 15;
        this.numw = (this.wDia - 6) / 32;
        this.numh = (this.hDia - 6) / 32;
        this.tfInput = new TField(this.xDia + 10, this.yDia + this.hDia - hTouch - (TField.getHeight() + 8), this.wDia - 20);
        this.tfInput.setMaxTextLenght(100);
        if (isNum) {
            this.tfInput.setIputType(1);
        }
        this.tfInput.setText("");
        if (GameCanvas.isTouch) {
            cmd.setPos(this.xDia + this.wDia / 2, this.yDia + this.hDia - iCommand.hButtonCmd / 2 - 5, null, cmd.caption);
            this.cmdClose.setPos(this.xDia + this.wDia - 13, this.yDia + 10, PaintInfoGameScreen.fraCloseMenu, "");
            this.right = this.cmdClose;
            this.left = cmd;
            this.tfInput.title = info;
        } else {
            this.center = cmd;
            this.left = this.cmdClose;
            this.tfInput.setFocus(true);
            this.right = this.tfInput.cmdClear;
        }
    }

    public void setinfo(String[] info, iCommand cmd, short type, short idNPC, String name, String[] texmacdinh, byte typepaint) {
        int n;
        this.isMore = true;
        this.isNew = false;
        this.left = null;
        this.right = null;
        this.center = null;
        this.mtfInput = null;
        hitem = 50;
        this.type = type;
        this.idNPC = idNPC;
        if (cmd == null) {
            GameCanvas.subDialog = null;
        }
        this.wDia = GameCanvas.w - 30;
        if (this.wDia > 140) {
            this.wDia = 140;
        }
        this.mtfInput = new TField[info.length];
        this.strinfo = info;
        this.name = name;
        if (this.mtfInput.length > 3) {
            this.iscroll = true;
        }
        if (this.iscroll) {
            this.wDia = GameCanvas.w / 4 * 3;
            this.hDia = GameCanvas.h / 5 * 4;
            this.wDia = GameCanvas.w - 30;
            if (this.wDia > 140) {
                this.wDia = 140;
            }
            if (this.hDia < 210) {
                this.hDia = 210;
            } else if (this.hDia > 280) {
                this.hDia = 280;
            }
            if (this.hDia < 210) {
                this.hDia = 210;
            } else if (this.hDia > 280) {
                this.hDia = 280;
            }
            this.xDia = GameCanvas.hw - this.wDia / 2;
            this.yDia = GameCanvas.hh - this.hDia / 2;
            if (this.yDia < 20) {
                this.yDia = 20;
            }
            if (GameCanvas.isSmallScreen) {
                this.yDia = 0;
            }
        } else {
            this.hDia = (TField.getHeight() + 18) * this.strinfo.length + 6 + GameCanvas.hCommand;
            this.hDia += hTouch + istouchMore;
            this.xDia = GameCanvas.hw - this.wDia / 2;
            this.yDia = GameCanvas.h - GameCanvas.hCommand * 2 - this.hDia + 15;
        }
        this.numw = (this.wDia - 6) / 32;
        this.numh = (this.hDia - 6) / 32;
        for (n = 0; n < this.mtfInput.length; ++n) {
            this.mtfInput[n] = new TField(this.xDia + 10, this.yDia + 8 + (TField.getHeight() + 18) * n + istouchMore + GameCanvas.hCommand, this.wDia - 20);
            this.mtfInput[n].setText("");
        }
        if (GameCanvas.isTouch) {
            cmd.setPos(this.xDia + this.wDia / 2, this.yDia + this.hDia - iCommand.hButtonCmd / 2 - 5, null, cmd.caption);
            this.cmdClose.setPos(this.xDia + this.wDia - 13, this.yDia + 10, PaintInfoGameScreen.fraCloseMenu, "");
            if (GameCanvas.currentScreen != GameCanvas.createChar) {
                this.right = this.cmdClose;
            }
            this.left = cmd;
        } else {
            this.center = cmd;
            this.left = this.cmdClose;
        }
        if (!GameCanvas.isTouch) {
            this.mtfInput[0].setFocus(true);
            this.right = this.mtfInput[0].cmdClear;
        }
        if (this.iscroll) {
            MainScreen.cameraSub.setAll(0, this.mtfInput.length * hitem - this.hDia + GameCanvas.hCommand + 30, 0, 0);
            this.list = new ListNew(this.xDia, this.yDia, this.wDia, this.hDia, hitem, 0, MainScreen.cameraSub.yLimit);
        }
        if (typepaint == 0) {
            for (n = 0; n < this.mtfInput.length; ++n) {
                this.mtfInput[n].setStringNull(info[n]);
            }
        }
        for (n = 0; n < this.mtfInput.length; ++n) {
            this.mtfInput[n].setText(texmacdinh[n]);
        }
    }

    public void setinfo(String[] info, iCommand cmd, short type, short idNPC, String name) {
        this.isMore = true;
        this.isNew = false;
        this.left = null;
        this.right = null;
        this.center = null;
        this.mtfInput = null;
        hitem = 50;
        this.type = type;
        this.idNPC = idNPC;
        if (cmd == null) {
            GameCanvas.subDialog = null;
        }
        this.wDia = GameCanvas.w - 30;
        if (this.wDia > 140) {
            this.wDia = 140;
        }
        this.mtfInput = new TField[info.length];
        this.strinfo = info;
        this.name = name;
        if (this.mtfInput.length > 3) {
            this.iscroll = true;
        }
        if (this.iscroll) {
            this.wDia = GameCanvas.w / 4 * 3;
            this.hDia = GameCanvas.h / 5 * 4;
            this.wDia = GameCanvas.w - 30;
            if (this.wDia > 140) {
                this.wDia = 140;
            }
            if (this.hDia < 210) {
                this.hDia = 210;
            } else if (this.hDia > 280) {
                this.hDia = 280;
            }
            if (this.hDia < 210) {
                this.hDia = 210;
            } else if (this.hDia > 280) {
                this.hDia = 280;
            }
            this.xDia = GameCanvas.hw - this.wDia / 2;
            this.yDia = GameCanvas.hh - this.hDia / 2;
            if (this.yDia < 20) {
                this.yDia = 20;
            }
            if (GameCanvas.isSmallScreen) {
                this.yDia = 0;
            }
        } else {
            this.hDia = (TField.getHeight() + 18) * this.strinfo.length + 6 + GameCanvas.hCommand;
            this.hDia += hTouch + istouchMore;
            this.xDia = GameCanvas.hw - this.wDia / 2;
            this.yDia = GameCanvas.h - GameCanvas.hCommand * 2 - this.hDia + 15;
        }
        this.numw = (this.wDia - 6) / 32;
        this.numh = (this.hDia - 6) / 32;
        for (int i = 0; i < this.mtfInput.length; ++i) {
            this.mtfInput[i] = new TField(this.xDia + 10, this.yDia + 8 + (TField.getHeight() + 18) * i + istouchMore + GameCanvas.hCommand, this.wDia - 20);
            this.mtfInput[i].setText("");
        }
        if (GameCanvas.isTouch) {
            cmd.setPos(this.xDia + this.wDia / 2, this.yDia + this.hDia - iCommand.hButtonCmd / 2 - 5, null, cmd.caption);
            this.cmdClose.setPos(this.xDia + this.wDia - 13, this.yDia + 10, PaintInfoGameScreen.fraCloseMenu, "");
            this.right = this.cmdClose;
            this.left = cmd;
        } else {
            this.center = cmd;
            this.left = this.cmdClose;
        }
        if (!GameCanvas.isTouch) {
            this.mtfInput[0].setFocus(true);
            this.right = this.mtfInput[0].cmdClear;
        }
        if (this.iscroll) {
            MainScreen.cameraSub.setAll(0, this.mtfInput.length * hitem - this.hDia + GameCanvas.hCommand + 30, 0, 0);
            this.list = new ListNew(this.xDia, this.yDia, this.wDia, this.hDia, hitem, 0, MainScreen.cameraSub.yLimit);
        }
    }

    public void setinfoSmallNew(String info, iCommand cmd, boolean isNum, int defValue, long price, String xuluong) {
        this.isMore = false;
        this.isNew = true;
        this.left = null;
        this.right = null;
        this.center = null;
        if (cmd == null) {
            GameCanvas.currentDialog = null;
        }
        this.price = price;
        this.info = info;
        this.name = "";
        this.xuluong = xuluong;
        this.wDia = GameCanvas.w / 3 * 2;
        if (this.wDia > 160) {
            this.wDia = 160;
        }
        String string3 = "";
        this.strinfo = this.fontInput.splitFontArray(info + " " + string3, this.wDia - 20);
        this.hDia = 15 * this.strinfo.length + 10 + TField.getHeight() + GameCanvas.hCommand;
        this.hDia += hTouch + istouchMore;
        this.xDia = GameCanvas.hw - this.wDia / 2;
        this.yDia = GameCanvas.h - GameCanvas.hCommand * 2 - this.hDia + 15;
        this.numw = (this.wDia - 6) / 32;
        this.numh = (this.hDia - 6) / 32;
        int n2 = this.wDia - 100;
        if (n2 < 40) {
            n2 = 40;
        }
        this.tfInput = new TField(this.xDia + this.wDia / 2 - n2 / 2, this.yDia + this.hDia - hTouch - (TField.getHeight() + 8), n2);
        if (isNum) {
            this.tfInput.setIputType(1);
        }
        if (defValue >= 0) {
            this.tfInput.setText("" + defValue);
        }
        this.strtam = this.tfInput.getText();
        if (GameCanvas.isTouch) {
            cmd.setPos(this.xDia + this.wDia / 2, this.yDia + this.hDia - iCommand.hButtonCmd / 2 - 5, null, cmd.caption);
            this.cmdClose.setPos(this.xDia + this.wDia - 12, this.yDia + 10, PaintInfoGameScreen.fraCloseMenu, "");
            this.right = this.cmdClose;
            this.left = cmd;
        } else {
            this.center = cmd;
            this.left = this.cmdClose;
            this.tfInput.setFocus(true);
            this.right = this.tfInput.cmdClear;
        }
    }

    public void setinfoSmallNew(String info, iCommand cmd, boolean isNum, int defValue, long price, String name, String xuluong) {
        this.isMore = false;
        this.isNew = true;
        this.left = null;
        this.right = null;
        this.center = null;
        if (cmd == null) {
            GameCanvas.currentDialog = null;
        }
        this.price = price;
        this.info = info;
        this.name = name;
        this.xuluong = xuluong;
        this.wDia = GameCanvas.w / 3 * 2;
        if (this.wDia > 160) {
            this.wDia = 160;
        }
        String string4 = "\n" + T.price + " " + price + " " + xuluong;
        this.strinfo = this.fontInput.splitFontArray(info + " " + string4, this.wDia - 20);
        this.hDia = 15 * this.strinfo.length + 10 + TField.getHeight() + GameCanvas.hCommand;
        this.hDia += hTouch + istouchMore;
        this.xDia = GameCanvas.hw - this.wDia / 2;
        this.yDia = GameCanvas.h - GameCanvas.hCommand * 2 - this.hDia + 15;
        this.numw = (this.wDia - 6) / 32;
        this.numh = (this.hDia - 6) / 32;
        int n2 = this.wDia - 100;
        if (n2 < 40) {
            n2 = 40;
        }
        this.tfInput = new TField(this.xDia + this.wDia / 2 - n2 / 2, this.yDia + this.hDia - hTouch - (TField.getHeight() + 8), n2);
        if (isNum) {
            this.tfInput.setIputType(1);
        }
        if (defValue >= 0) {
            this.tfInput.setText("" + defValue);
        }
        this.strtam = this.tfInput.getText();
        if (GameCanvas.isTouch) {
            cmd.setPos(this.xDia + this.wDia / 2, this.yDia + this.hDia - iCommand.hButtonCmd / 2 - 5, null, cmd.caption);
            this.cmdClose.setPos(this.xDia + this.wDia - 12, this.yDia + 10, PaintInfoGameScreen.fraCloseMenu, "");
            this.right = this.cmdClose;
            this.left = cmd;
        } else {
            this.center = cmd;
            this.left = this.cmdClose;
            this.tfInput.setFocus(true);
            this.right = this.tfInput.cmdClear;
        }
    }

    public void paint(mGraphics g) {
        GameCanvas.resetTrans(g);
        this.paintFormList(g, this.xDia, this.yDia, this.wDia, this.hDia, this.name);
        if (this.iscroll) {
            g.setClip(this.xDia + 10, this.yDia + GameCanvas.hCommand, this.wDia, this.hDia - hitem - 10);
            g.saveCanvas();
            g.ClipRec(this.xDia + 10, this.yDia + GameCanvas.hCommand, this.wDia, this.hDia - hitem - 10);
            g.translate(0, -MainScreen.cameraSub.yCam);
        }
        int n = this.yDia + istouchMore + GameCanvas.hCommand;
        if (this.isMore) {
            for (int i = 0; i < this.mtfInput.length; ++i) {
                this.fontInput.drawString(g, this.strinfo[i], GameCanvas.w / 2, n - 5 + i * (TField.getHeight() + 18), 2, true);
                if (this.iscroll) {
                    this.mtfInput[i].paintByList(g);
                    continue;
                }
                this.mtfInput[i].paint(g);
            }
        } else {
            for (int i = 0; i < this.strinfo.length; ++i) {
                this.fontInput.drawString(g, this.strinfo[i], GameCanvas.w / 2, n + i * 15 - 5, 2, true);
            }
            this.tfInput.paint(g);
        }
        if (this.iscroll) {
            mGraphics.resetTransAndroid(g);
            g.restoreCanvas();
        }
        g.endClip();
        GameCanvas.resetTrans(g);
        super.paintCmd(g);
    }

    public void keypress(int keyCode) {
        if (this.isMore) {
            for (int i = 0; i < this.mtfInput.length; ++i) {
                if (!this.mtfInput[i].isFocused()) continue;
                this.mtfInput[i].keyPressed(keyCode);
                return;
            }
        } else {
            this.tfInput.keyPressed(keyCode);
        }
        super.keypress(keyCode);
    }

    public void update() {
        this.updatekey();
        this.updatePointer();
        if (this.isMore) {
            for (int i = 0; i < this.mtfInput.length; ++i) {
                this.mtfInput[i].update();
            }
        } else if (this.tfInput != null) {
            this.tfInput.update();
            if (!GameCanvas.isTouch && this.right != this.tfInput.cmdClear) {
                this.right = this.tfInput.cmdClear;
            }
            if (this.isNew && this.tfInput.getText().compareTo(this.strtam) != 0) {
                this.strtam = this.tfInput.getText();
                int n = 0;
                try {
                    n = Integer.parseInt(this.strtam);
                }
                catch (Exception exception) {
                    n = 0;
                }
                String string = "\n" + T.price + " " + this.price * (long)n + " " + this.xuluong;
                this.strinfo = this.fontInput.splitFontArray(this.info + " " + string, this.wDia - 20);
                this.hDia = 15 * this.strinfo.length + 10 + TField.getHeight() + hTouch + istouchMore + GameCanvas.hCommand;
                int n2 = this.wDia - 100;
                if (n2 < 40) {
                    n2 = 40;
                }
                this.xDia = GameCanvas.hw - this.wDia / 2;
                this.yDia = GameCanvas.h - GameCanvas.hCommand * 2 - this.hDia + 15;
                this.numw = (this.wDia - 6) / 32;
                this.numh = (this.hDia - 6) / 32;
                this.tfInput.x = this.xDia + this.wDia / 2 - n2 / 2;
                this.tfInput.y = this.yDia + this.hDia - (TField.getHeight() + 8) - hTouch;
            }
        }
        super.update();
    }

    public void updatekey() {
        if (this.isMore) {
            int n = this.idSelect;
            if (GameCanvas.keyMyHold[8]) {
                for (int i = 0; i < this.mtfInput.length; ++i) {
                    if (!this.mtfInput[i].isFocused()) continue;
                    this.mtfInput[i].setFocus(false);
                    if (i < this.mtfInput.length - 1) {
                        this.mtfInput[i + 1].setFocus(true);
                        this.idSelect = i + 1;
                        if (GameCanvas.isTouch) break;
                        this.right = this.mtfInput[i + 1].cmdClear;
                        break;
                    }
                    this.mtfInput[0].setFocus(true);
                    this.idSelect = 0;
                    if (GameCanvas.isTouch) break;
                    this.right = this.mtfInput[0].cmdClear;
                    break;
                }
                GameCanvas.clearKeyHold(8);
            } else if (GameCanvas.keyMyHold[2]) {
                for (int i = 0; i < this.mtfInput.length; ++i) {
                    if (!this.mtfInput[i].isFocused()) continue;
                    this.mtfInput[i].setFocus(false);
                    if (i > 0) {
                        this.mtfInput[i - 1].setFocus(true);
                        this.idSelect = i - 1;
                        break;
                    }
                    this.mtfInput[this.mtfInput.length - 1].setFocus(true);
                    this.idSelect = this.mtfInput.length - 1;
                    break;
                }
                GameCanvas.clearKeyHold(2);
            }
            this.idSelect = this.resetSelect(this.idSelect, this.mtfInput.length - 1, false);
            if (n != this.idSelect) {
                MainScreen.cameraSub.moveCamera(0, this.idSelect * 40 - this.hDia / 2 + 40 + GameCanvas.hCommand);
            }
        }
        super.updatekey();
    }

    public void updatePointer() {
        super.updatePointer();
        if (this.isMore) {
            if (this.iscroll) {
                if (GameCanvas.isPointSelect(this.xDia, this.yDia + GameCanvas.hCommand, this.wDia, this.hDia - GameCanvas.hCommand)) {
                    int n;
                    for (n = 0; n < this.mtfInput.length; ++n) {
                        if (!this.mtfInput[n].isFocus) continue;
                        return;
                    }
                    n = (MainScreen.cameraSub.yCam + GameCanvas.py - this.yDia - GameCanvas.hCommand) / hitem;
                    if (n >= 0 && n < this.mtfInput.length) {
                        if (GameCanvas.isTouch) {
                            this.mtfInput[n].setFocus(true);
                            this.mtfInput[n].updatepointerByList();
                        } else {
                            this.mtfInput[n].updatePoiter();
                        }
                    }
                    GameCanvas.isPointerSelect = false;
                }
            } else {
                for (int i = 0; i < this.mtfInput.length; ++i) {
                    this.mtfInput[i].updatePoiter();
                }
            }
        } else if (this.tfInput != null) {
            this.tfInput.updatePoiter();
        }
        if (this.iscroll) {
            if (GameCanvas.isTouch && this.list != null) {
                this.list.moveCamera();
            } else {
                MainScreen.cameraSub.UpdateCamera();
            }
        }
        if (GameCanvas.isTouch && this.list != null) {
            this.list.update_Pos_UP_DOWN();
            MainScreen.cameraSub.yCam = this.list.cmx;
        }
    }

    public String[] getarrayText() {
        String[] stringArray = new String[this.mtfInput.length];
        for (int i = 0; i < this.mtfInput.length; ++i) {
            stringArray[i] = this.mtfInput[i].getText();
        }
        return stringArray;
    }
}

