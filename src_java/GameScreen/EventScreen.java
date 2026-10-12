/*
 * Decompiled with CFR 0.152.
 */
package GameScreen;

import CLib.mFont;
import CLib.mGraphics;
import CLib.mVector;
import GameObjects.MainEvent;
import GameScreen.MainScreen;
import GameScreen.PaintInfoGameScreen;
import InterfaceComponents.ListNew;
import InterfaceComponents.MsgChat;
import InterfaceComponents.iCommand;
import Main.GameCanvas;
import Model.AvMain;
import Model.T;
import Model.mCamera;

public class EventScreen
extends MainScreen {
    public int hItem;
    public int wDia;
    public int maxSizeList;
    public int hbutton;
    public int hDia;
    public int min;
    public int max;
    public int xDia;
    public int yDia;
    public int numw;
    public int numh;
    public int idSelect;
    public int idCommand;
    iCommand cmdSeL;
    iCommand cmdDel;
    public static mVector vecListEvent = new mVector("EventScreen vecListEvent");
    public static mVector vecEventShow = new mVector("EventScreen vecEventShow");
    public static mVector cmdList = new mVector("EventScreen cmdList");
    mCamera cameraDia = new mCamera();
    ListNew list;
    int xBegin;
    int w2cmd;

    public EventScreen() {
        this.cmdSeL = new iCommand(T.view, 2, 0, (AvMain)this);
        this.cmdDel = new iCommand(T.delMess, 2, 1, (AvMain)this);
        this.hItem = GameCanvas.hCommand + 5;
        this.wDia = GameCanvas.w / 4 * 3;
        if (this.wDia > 180) {
            this.wDia = 180;
        }
    }

    public void setCaptionCmd() {
        this.cmdSeL.caption = T.view;
        this.cmdDel.caption = T.delMess;
    }

    public void Show(MainScreen screen) {
        super.Show(screen);
        PaintInfoGameScreen.numMess = 0;
    }

    public void init() {
        for (int i = 0; i < vecListEvent.size(); ++i) {
            MainEvent mainEvent = (MainEvent)vecListEvent.elementAt(i);
            if (!mainEvent.isRemove) continue;
            vecListEvent.removeElement(mainEvent);
            --i;
        }
        this.hbutton = iCommand.hButtonCmd * 3 / 2;
        if (GameCanvas.isTouch) {
            this.hbutton = 0;
        }
        this.maxSizeList = (GameCanvas.h / 4 * 3 - (10 + GameCanvas.hCommand)) / this.hItem + 1;
        this.hDia = this.hItem * this.maxSizeList + 10 + GameCanvas.hCommand;
        this.maxSizeList += 2;
        this.min = 0;
        this.max = this.maxSizeList;
        if (this.max > vecListEvent.size()) {
            this.max = vecListEvent.size();
        }
        this.xDia = GameCanvas.hw - this.wDia / 2;
        this.yDia = GameCanvas.hh - this.hDia / 2 - GameCanvas.hCommand / 2;
        this.numw = (this.wDia - 6) / 32;
        this.numh = (this.hDia - 6) / 32;
        this.updateList();
    }

    private void updateList() {
        cmdList.removeAllElements();
        iCommand iCommand2 = new iCommand(T.close, -1, this);
        if (GameCanvas.isTouch) {
            iCommand2.setPos(this.xDia + this.wDia - 12, this.yDia + 10, PaintInfoGameScreen.fraCloseMenu, "");
            cmdList.addElement(iCommand2);
        } else {
            if (vecListEvent.size() > 0) {
                iCommand iCommand3 = new iCommand(T.menu, 1, this);
                cmdList.addElement(iCommand3);
            }
            cmdList.addElement(iCommand2);
            this.setPosCmdNew(0);
            this.right = iCommand2;
        }
        this.cameraDia.setAll(0, this.hItem * vecListEvent.size() - (this.hDia - GameCanvas.hCommand - this.hbutton), 0, 0);
        this.list = new ListNew(this.xDia, this.yDia, this.wDia, this.hDia, 0, 0, this.cameraDia.yLimit);
        if (this.idSelect >= vecListEvent.size()) {
            this.idSelect = GameCanvas.isTouch ? -1 : 0;
        }
    }

    public void setPosCmdNew(int yplus) {
        this.idCommand = 0;
        if (cmdList.size() > 0) {
            int n2 = cmdList.size();
            if (n2 == 1) {
                this.xBegin = this.xDia + this.wDia / 2;
                this.w2cmd = 0;
            } else if (n2 == 2) {
                this.w2cmd = 20;
                this.xBegin = this.xDia + this.wDia / 2 - this.w2cmd / 2 - iCommand.wButtonCmd / 2;
            } else {
                this.w2cmd = 20;
                this.xBegin = this.xDia + this.wDia / 2 - this.w2cmd / 2 - iCommand.wButtonCmd / 2;
            }
            for (int i = 0; i < n2; ++i) {
                iCommand iCommand2 = (iCommand)cmdList.elementAt(i);
                iCommand2.isSelect = false;
                if (n2 == 3 && i == 2) {
                    iCommand2.setPos(this.xDia + this.wDia / 2, this.yDia + this.hDia - iCommand.hButtonCmd - (n2 - 1) / 2 * (iCommand.hButtonCmd + 5) + 7 + i / 2 * (iCommand.hButtonCmd + 5) + yplus, null, iCommand2.caption);
                } else {
                    iCommand2.setPos(this.xBegin + i % 2 * (iCommand.wButtonCmd + this.w2cmd), this.yDia + this.hDia - iCommand.hButtonCmd - (n2 - 1) / 2 * (iCommand.hButtonCmd + 5) + 7 + i / 2 * (iCommand.hButtonCmd + 5) + yplus, null, iCommand2.caption);
                }
                if (i != 0) continue;
                iCommand2.isSelect = true;
            }
        }
    }

    public void commandPointer(int index, int subIndex) {
        switch (index) {
            case -1: {
                this.lastScreen.Show(this.lastScreen.lastScreen);
                break;
            }
            case 1: {
                if (this.idSelect < 0 || this.idSelect >= vecListEvent.size()) {
                    return;
                }
                if (this.idSelect > 0) {
                    mVector mVector2 = new mVector("EventScreen vec");
                    mVector2.addElement(this.cmdSeL);
                    mVector2.addElement(this.cmdDel);
                    GameCanvas.menu2.startAt(mVector2, 2, T.mevent, false, null);
                    break;
                }
                this.cmdSeL.perform();
                break;
            }
            case 2: {
                if (this.idSelect < 0 || this.idSelect >= vecListEvent.size()) {
                    return;
                }
                boolean bl = false;
                if (subIndex == 1) {
                    bl = true;
                }
                MainEvent mainEvent = (MainEvent)vecListEvent.elementAt(this.idSelect);
                this.doEvent(bl, mainEvent);
            }
        }
    }

    public void paint(mGraphics g) {
        Object object;
        int n;
        if (this.lastScreen != null) {
            this.lastScreen.paint(g);
        }
        if (GameCanvas.currentScreen != this) {
            return;
        }
        GameCanvas.resetTrans(g);
        this.paintFormList(g, this.xDia, this.yDia, this.wDia, this.hDia, T.mevent);
        int n2 = this.yDia + GameCanvas.hCommand + 3;
        if (vecListEvent.size() > 0) {
            g.setClip(this.xDia + 4, n2, this.wDia - 8, this.hDia - GameCanvas.hCommand - this.hbutton - 6);
            g.saveCanvas();
            g.ClipRec(this.xDia + 4, n2, this.wDia - 8, this.hDia - GameCanvas.hCommand - this.hbutton - 6);
            g.translate(0, -this.cameraDia.yCam);
            if (this.idSelect >= 0) {
                g.setColor(-4873075);
                g.fillRect(this.xDia + 4, n2 - 2 + this.idSelect * this.hItem, this.wDia - 8, this.hItem - 1, true);
            }
            n2 += this.hItem * this.min;
            for (n = this.min; n < this.max; ++n) {
                if (n < 0 && n >= vecListEvent.size()) continue;
                object = (MainEvent)vecListEvent.elementAt(n);
                int n3 = 0;
                if (((MainEvent)object).isNew == 1 && GameCanvas.gameTick % 20 > 9) {
                    n3 = 1;
                }
                PaintInfoGameScreen.fraEvent.drawFrame(((MainEvent)object).IDCmd * 2 + 1 - n3, this.xDia + 20, n2 + this.hItem / 2 - 2, 0, 3, g);
                if (GameCanvas.isTouch && n > 0) {
                    PaintInfoGameScreen.fraClose.drawFrame(0, this.xDia + this.wDia - 20, n2 + this.hItem / 2 - 2, 0, 3, g);
                }
                mFont.tahoma_7b_white.drawString(g, ((MainEvent)object).nameEvent, this.xDia + 35, n2 + 1, 0, true);
                mFont.tahoma_7_white.drawString(g, ((MainEvent)object).contentEvent, this.xDia + 42, n2 + 13, 0, true);
                n2 += this.hItem;
                if (n >= vecListEvent.size() - 1) continue;
                g.setColor(AvMain.color[4]);
                g.fillRect(this.xDia + 6, n2 - 3, this.wDia - 12, 1, true);
            }
            mGraphics.resetTransAndroid(g);
            g.restoreCanvas();
            g.endClip();
        } else {
            mFont.tahoma_7_white.drawString(g, T.noEvent, this.xDia + this.wDia / 2, n2 + 3, 2, true);
        }
        GameCanvas.resetTrans(g);
        if (cmdList != null) {
            for (n = 0; n < cmdList.size(); ++n) {
                object = (iCommand)cmdList.elementAt(n);
                ((iCommand)object).paint(g, ((iCommand)object).xCmd, ((iCommand)object).yCmd);
            }
        }
        this.paintCmd_OnlyText(g);
    }

    public void update() {
        if (GameCanvas.currentScreen == this) {
            this.lastScreen.update();
        }
        int n = this.cameraDia.yCam;
        if (GameCanvas.isTouch) {
            this.list.moveCamera();
        } else {
            this.cameraDia.UpdateCamera();
        }
        super.update();
        int n2 = vecListEvent.size();
        for (int i = 0; i < vecListEvent.size(); ++i) {
            MainEvent mainEvent = (MainEvent)vecListEvent.elementAt(i);
            if (!mainEvent.isRemove) continue;
            vecListEvent.removeElement(mainEvent);
            --i;
        }
        if (this.idSelect < 0 || this.idSelect >= vecListEvent.size()) {
            this.idSelect = GameCanvas.isTouch ? -1 : 0;
        }
        if (this.cameraDia.yCam != n || n2 != vecListEvent.size()) {
            this.min = this.cameraDia.yCam / this.hItem;
            this.max = this.min + this.maxSizeList;
            if (this.max > vecListEvent.size()) {
                this.max = vecListEvent.size();
                this.min = this.max - this.maxSizeList;
            }
            if (this.min < 0) {
                this.min = 0;
            }
        }
    }

    public void updatekey() {
        if (vecListEvent.size() > 0) {
            if (GameCanvas.keyMyHold[2]) {
                --this.idSelect;
                GameCanvas.clearKeyHold(2);
            } else if (GameCanvas.keyMyHold[8]) {
                ++this.idSelect;
                GameCanvas.clearKeyHold(8);
            }
            this.idSelect = this.resetSelect(this.idSelect, vecListEvent.size() - 1, true);
            this.cameraDia.moveCamera(0, this.idSelect * this.hItem - (this.hDia / 2 - GameCanvas.hCommand - this.hbutton));
        } else if (cmdList.size() > 1) {
            cmdList.removeAllElements();
            cmdList.addElement(new iCommand(T.close, -1, this));
            this.setPosCmdNew(0);
            this.idSelect = 0;
        }
        if (cmdList != null) {
            int n = cmdList.size();
            if (!GameCanvas.isTouch && n > 0) {
                int n2 = this.idCommand--;
                if (GameCanvas.keyMyHold[4]) {
                    GameCanvas.clearKeyHold(4);
                } else if (GameCanvas.keyMyHold[6]) {
                    ++this.idCommand;
                    GameCanvas.clearKeyHold(6);
                }
                this.idCommand = this.resetSelect(this.idCommand, n - 1, false);
                if (n2 != this.idCommand) {
                    for (int i = 0; i < n; ++i) {
                        iCommand iCommand2 = (iCommand)cmdList.elementAt(i);
                        iCommand2.isSelect = i == this.idCommand;
                    }
                }
            }
        }
        if (GameCanvas.keyMyHold[5]) {
            GameCanvas.clearKeyHold(5);
            if (cmdList != null && this.idCommand < cmdList.size()) {
                ((iCommand)cmdList.elementAt(this.idCommand)).perform();
            }
        }
        super.updatekey();
    }

    public void updatePointer() {
        int n;
        if (GameCanvas.isTouch) {
            this.list.update_Pos_UP_DOWN();
            this.cameraDia.yCam = this.list.cmx;
        }
        if (vecListEvent.size() > 0 && GameCanvas.isPointSelect(this.xDia, this.yDia + GameCanvas.hCommand, this.wDia, this.hDia - GameCanvas.hCommand) && (n = (GameCanvas.py - (this.yDia + GameCanvas.hCommand)) / this.hItem) >= 0 && n <= vecListEvent.size() - 1) {
            this.idSelect = n;
            this.idSelect = this.resetSelect(this.idSelect, vecListEvent.size() - 1, false);
            boolean bl = false;
            if (GameCanvas.isPointSelect(this.xDia + this.wDia - 30, this.yDia + GameCanvas.hCommand, 30, this.hDia - GameCanvas.hCommand) && this.idSelect > 0) {
                bl = true;
            }
            MainEvent mainEvent = (MainEvent)vecListEvent.elementAt(this.idSelect);
            this.doEvent(bl, mainEvent);
            GameCanvas.isPointerSelect = false;
        }
        if (cmdList != null) {
            for (n = 0; n < cmdList.size(); ++n) {
                iCommand iCommand2 = (iCommand)cmdList.elementAt(n);
                iCommand2.updatePointer();
            }
        }
    }

    public void doEvent(boolean isre, MainEvent mevent) {
        if (isre) {
            mevent.isRemove = true;
            return;
        }
        mVector mVector2 = null;
        if (PaintInfoGameScreen.numMess > 0) {
            --PaintInfoGameScreen.numMess;
        }
        switch (mevent.IDCmd) {
            case 0: {
                MsgChat.setIndexFocus(mevent.nameEvent);
                GameCanvas.start_Chat_Dialog();
                mevent.isNew = 0;
                break;
            }
            case 1: {
                mVector2 = new mVector("EventScreen vec2");
                mVector2.addElement(new iCommand(T.chapnhan, 1, 1, (AvMain)mevent));
                mVector2.addElement(new iCommand(T.tuchoi, 1, 2, (AvMain)mevent));
                mVector2.addElement(new iCommand(T.close, -1));
                GameCanvas.start_Select_Dialog(mevent.nameEvent + T.yeucauketban, mVector2);
                mevent.isNew = 0;
                break;
            }
            case 2: {
                mVector2 = new mVector("EventScreen vec3");
                mVector2.addElement(new iCommand(T.chapnhan, 2, 1, (AvMain)mevent));
                mVector2.addElement(new iCommand(T.tuchoi, 2, 0, (AvMain)mevent));
                mVector2.addElement(new iCommand(T.close, -1));
                GameCanvas.start_Select_Dialog(mevent.nameEvent + T.loimoiParty, mVector2);
                mevent.isNew = 0;
                break;
            }
            case 3: {
                mVector2 = new mVector("EventScreen vec4");
                mVector2.addElement(new iCommand(T.chapnhan, 3, 1, (AvMain)mevent));
                mVector2.addElement(new iCommand(T.tuchoi, 3, 0, (AvMain)mevent));
                mVector2.addElement(new iCommand(T.close, -1));
                GameCanvas.start_Select_Dialog(mevent.nameEvent + T.hoigiaodich, mVector2);
                mevent.isNew = 0;
                break;
            }
            case 4: {
                mVector2 = new mVector("EventScreen vec5");
                mVector2.addElement(new iCommand(T.chapnhan, 4, 1, (AvMain)mevent));
                mVector2.addElement(new iCommand(T.info, 4, 2, (AvMain)mevent));
                mVector2.addElement(new iCommand(T.cancel, 4, 0, (AvMain)mevent));
                GameCanvas.start_Select_Dialog(mevent.nameEvent + T.hoithachdau + mevent.numThachDau + " " + T.coin + ".", mVector2);
                mevent.isNew = 0;
                break;
            }
            case 5: {
                mVector2 = new mVector("EventScreen vec6");
                mVector2.addElement(new iCommand(T.chapnhan, 5, 1, (AvMain)mevent));
                mVector2.addElement(new iCommand(T.close, -1));
                GameCanvas.start_Select_Dialog(mevent.nameEvent + T.moivaoclan, mVector2);
                mevent.isNew = 0;
            }
        }
    }

    public static MainEvent setEvent(String name, byte type) {
        for (int i = 0; i < vecListEvent.size(); ++i) {
            MainEvent mainEvent = (MainEvent)vecListEvent.elementAt(i);
            if (mainEvent.isRemove) {
                vecListEvent.removeElement(mainEvent);
                --i;
                continue;
            }
            if (mainEvent.IDCmd != type || mainEvent.nameEvent.compareTo(name) != 0) continue;
            return mainEvent;
        }
        return null;
    }

    public static MainEvent setEventShow(String name, byte type) {
        for (int i = 0; i < vecEventShow.size(); ++i) {
            MainEvent mainEvent = (MainEvent)vecEventShow.elementAt(i);
            if (mainEvent.IDCmd != type || mainEvent.nameEvent.compareTo(name) != 0) continue;
            return mainEvent;
        }
        return null;
    }

    public static int setIndexEvent(String name, byte type) {
        for (int i = 0; i < vecListEvent.size(); ++i) {
            MainEvent mainEvent = (MainEvent)vecListEvent.elementAt(i);
            if (mainEvent.IDCmd != type || mainEvent.nameEvent.compareTo(name) != 0) continue;
            return i;
        }
        return -1;
    }

    public void addEvent(String namecheck, byte type, String content, int numThachdau) {
        MainEvent mainEvent = EventScreen.setEvent(namecheck, type);
        if (mainEvent == null) {
            mainEvent = new MainEvent(0, type, namecheck, content);
            vecListEvent.addElement(mainEvent);
            this.updateList();
            this.min = this.cameraDia.yCam / this.hItem;
            this.max = this.min + this.maxSizeList;
            if (this.max > vecListEvent.size()) {
                this.max = vecListEvent.size();
                this.min = this.max - this.maxSizeList;
            }
            if (this.min < 0) {
                this.min = 0;
            }
        } else if (type == 0) {
            mainEvent.contentEvent = content;
        }
        if (namecheck.compareTo(T.tinden) != 0) {
            ++PaintInfoGameScreen.numMess;
            mainEvent.isNew = 1;
            mainEvent.numThachDau = numThachdau;
            if (type != 0 || namecheck.compareTo(T.tabBangHoi) != 0 && namecheck.compareTo(T.tabThuLinh) != 0) {
                MainEvent mainEvent2 = EventScreen.setEventShow(namecheck, type);
                if (mainEvent2 == null) {
                    MainEvent mainEvent3 = new MainEvent(mainEvent.IDObj, mainEvent.IDCmd, mainEvent.nameEvent, mainEvent.contentEvent);
                    vecEventShow.addElement(mainEvent3);
                } else if (type == 0) {
                    mainEvent2.contentEvent = mainEvent.contentEvent;
                }
            }
        }
    }
}

