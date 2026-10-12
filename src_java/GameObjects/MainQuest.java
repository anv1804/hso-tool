/*
 * Decompiled with CFR 0.152.
 */
package GameObjects;

import CLib.mFont;
import CLib.mVector;
import GameObjects.MainMonster;
import GameObjects.MainObject;
import GameScreen.GameScreen;
import InterfaceComponents.MainTabNew;
import InterfaceComponents.iCommand;
import Main.GameCanvas;
import Model.AvMain;
import Model.T;
import netcommand.global.GlobalService;

public class MainQuest
extends AvMain {
    public static final int Q_NHAT_ITEM = 0;
    public static final int Q_KILL_MONSTER = 1;
    public static final int Q_CHUYEN_DO = 2;
    public static final int Q_TALK = 3;
    public static final int TYPE_ITEM = 0;
    public static final int TYPE_MONSTER = 1;
    public static final int NHAN = 0;
    public static final int TRA = 1;
    public static mVector vecQuestList = new mVector("MainQuest vecQuestList");
    public static mVector vecQuestFinish = new mVector("MainQuest vecQuestFinish");
    public static mVector vecQuestDoing_Main = new mVector("MainQuest vecQuestDoing_Main");
    public static mVector vecQuestDoing_Sub = new mVector("MainQuest vecQuestDoing_Sub");
    public byte typeQuest;
    public int ID;
    public int idNPC_To;
    public int idNPC_From;
    public int idNPCChat;
    public int nhantra;
    public boolean isComplete = false;
    public boolean isMain;
    public String name;
    public String strDetail;
    public String strShortDetail;
    public String[] mDetail;
    public String[] mstrTalk;
    public String[] mstrHelp;
    public String strDetailTalk;
    public String strDetailHelp;
    public String strShowDialog;
    public short[] mIdQuest;
    public short[] mtotalQuest;
    public short[] mQuestGot;
    int step = 0;

    public MainQuest(int ID, boolean isMain, String name, int idNpcFrom, String strDetailTalk, byte typeQuest, String strDetailHelp) {
        this.ID = ID;
        this.isMain = isMain;
        this.idNPC_From = idNpcFrom;
        this.idNPCChat = idNpcFrom;
        this.name = name;
        this.strDetailTalk = strDetailTalk;
        this.strDetailHelp = strDetailHelp;
        this.typeQuest = typeQuest;
        this.nhantra = 0;
        this.mstrTalk = mFont.split(strDetailTalk, ">");
    }

    public MainQuest(int ID, boolean isMain, String name, int strDetailHelp, String idNpcTo, String strShortDetail) {
        this.ID = ID;
        this.isMain = isMain;
        this.idNPC_To = strDetailHelp;
        this.idNPCChat = strDetailHelp;
        this.name = name;
        this.strDetailTalk = idNpcTo;
        this.strDetailHelp = strShortDetail;
        this.mstrHelp = mFont.tahoma_7_white.splitFontArray(strShortDetail, MainTabNew.wblack - 35);
        this.nhantra = 1;
        this.mstrTalk = mFont.split(idNpcTo, ">");
    }

    public MainQuest(int ID, boolean isMain, String name, String strDetailHelp, byte typeQuest, String strShortDetail, int idNpcTo, short[] mid, short[] mtotal, short[] mget, int typeItem_Monster) {
        this.ID = ID;
        this.isMain = isMain;
        this.idNPC_To = idNpcTo;
        this.idNPCChat = idNpcTo;
        this.name = name;
        this.typeQuest = typeQuest;
        this.strShortDetail = strShortDetail;
        this.strDetailHelp = strDetailHelp;
        this.mIdQuest = mid;
        this.mtotalQuest = mtotal;
        this.mQuestGot = mget;
    }

    public MainQuest(int ID, boolean isMain, String name, String strDetailHelp, int idNpcTo, String strShortDetail) {
        this.ID = ID;
        this.isMain = isMain;
        this.name = name;
        this.strShortDetail = strShortDetail;
        this.strDetailHelp = strDetailHelp;
        this.idNPC_To = idNpcTo;
        this.idNPCChat = idNpcTo;
    }

    public void commandPointer(int index, int subIndex) {
        switch (index) {
            case 0: {
                if (GameCanvas.menu2.runText != null && !GameCanvas.menu2.runText.nextDlgStep()) {
                    return;
                }
                this.nextStep();
                break;
            }
            case 1: {
                String string;
                if (GameCanvas.menu2.runText != null && !GameCanvas.menu2.runText.nextDlgStep()) {
                    return;
                }
                MainObject mainObject = MainObject.get_Object(this.idNPCChat, (byte)2);
                if (mainObject != null) {
                    mainObject.chat = null;
                }
                if ((string = this.strDetailHelp) == null) {
                    string = "sai roi";
                }
                GameScreen.player.chat = null;
                if (this.typeQuest == 3) {
                    GlobalService.gI().quest((short)this.ID, (byte)(!this.isMain ? 1 : 0), (byte)1);
                } else {
                    GameCanvas.start_Quest_Dialog(string, this.name, this.ID, this.nhantra, (byte)(!this.isMain ? 1 : 0));
                }
                GameScreen.player.currentQuest = null;
                GameScreen.gI().center = null;
                GameCanvas.clearKeyHold();
                GameCanvas.isPointerSelect = false;
            }
        }
    }

    public void beginQuest() {
        this.step = 0;
        GameScreen.player.currentQuest = this;
        this.nextStep();
    }

    public void show_Info_Quest_Doing() {
        if (this.strShortDetail == null || MainObject.get_Object(this.idNPCChat, (byte)2) == null) {
            return;
        }
        MainObject.get_Object((int)this.idNPCChat, (byte)2).strChatPopup = this.strDetailHelp;
    }

    public iCommand setCmd() {
        iCommand iCommand2 = null;
        iCommand2 = this.step < this.mstrTalk.length - 1 ? new iCommand(T.next, 0, this) : new iCommand(T.next, 1, this);
        return iCommand2;
    }

    public void nextStep() {
        if (MainObject.get_Object(this.idNPCChat, (byte)2) == null) {
            GameScreen.player.currentQuest = null;
            return;
        }
        if (this.mstrTalk[this.step].trim().startsWith("0")) {
            MainObject.get_Object((int)this.idNPCChat, (byte)2).chat = null;
            mVector mVector2 = new mVector("MainQuest menu");
            iCommand iCommand2 = this.setCmd();
            mVector2.addElement(iCommand2);
            GameCanvas.menu2.startAt_NPC(mVector2, this.mstrTalk[this.step].substring(1, this.mstrTalk[this.step].length()), GameScreen.player.ID, (byte)0, true, 0);
        } else {
            GameScreen.player.chat = null;
            mVector mVector3 = new mVector("MainQuest menu2");
            iCommand iCommand3 = this.setCmd();
            mVector3.addElement(iCommand3);
            GameCanvas.menu2.startAt_NPC(mVector3, this.mstrTalk[this.step].substring(1, this.mstrTalk[this.step].length()), this.idNPCChat, (byte)2, true, 0);
        }
        ++this.step;
    }

    public static void updateQuestKillMonster(int idMonster) {
        int n2;
        MainQuest mainQuest;
        int n3;
        MainMonster mainMonster = (MainMonster)MainObject.get_Object(idMonster, (byte)1);
        if (mainMonster == null) {
            return;
        }
        String string = "";
        for (n3 = 0; n3 < vecQuestDoing_Main.size(); ++n3) {
            mainQuest = (MainQuest)vecQuestDoing_Main.elementAt(n3);
            if (mainQuest.typeQuest != 1) continue;
            for (n2 = 0; n2 < mainQuest.mIdQuest.length; ++n2) {
                if (mainQuest.mIdQuest[n2] != mainMonster.catalogyMonster || mainQuest.mQuestGot[n2] >= mainQuest.mtotalQuest[n2]) continue;
                int n4 = n2;
                mainQuest.mQuestGot[n4] = (short)(mainQuest.mQuestGot[n4] + 1);
                if (string.length() > 0) {
                    string = string + " , ";
                }
                string = string + mainQuest.mQuestGot[n2] + "/" + mainQuest.mtotalQuest[n2];
            }
        }
        for (n3 = 0; n3 < vecQuestDoing_Sub.size(); ++n3) {
            mainQuest = (MainQuest)vecQuestDoing_Sub.elementAt(n3);
            if (mainQuest.typeQuest != 1) continue;
            for (n2 = 0; n2 < mainQuest.mIdQuest.length; ++n2) {
                if (mainQuest.mIdQuest[n2] != mainMonster.catalogyMonster || mainQuest.mQuestGot[n2] >= mainQuest.mtotalQuest[n2]) continue;
                int n5 = n2;
                mainQuest.mQuestGot[n5] = (short)(mainQuest.mQuestGot[n5] + 1);
                if (string.length() > 0) {
                    string = string + " , ";
                }
                string = string + mainQuest.mQuestGot[n2] + "/" + mainQuest.mtotalQuest[n2];
            }
        }
        if (string.length() > 0) {
            string = T.giet + mainMonster.name + ": " + string;
            GameCanvas.addInfoChar(string);
        }
    }

    public static void updateQuestGetItem(int idItem, String name) {
        int n2;
        MainQuest mainQuest;
        int n3;
        String string2 = "";
        for (n3 = 0; n3 < vecQuestDoing_Main.size(); ++n3) {
            mainQuest = (MainQuest)vecQuestDoing_Main.elementAt(n3);
            if (mainQuest.typeQuest != 0) continue;
            for (n2 = 0; n2 < mainQuest.mIdQuest.length; ++n2) {
                if (mainQuest.mIdQuest[n2] != idItem || mainQuest.mQuestGot[n2] >= mainQuest.mtotalQuest[n2]) continue;
                int n4 = n2;
                mainQuest.mQuestGot[n4] = (short)(mainQuest.mQuestGot[n4] + 1);
                if (string2.length() > 0) {
                    string2 = string2 + " , ";
                }
                string2 = string2 + mainQuest.mQuestGot[n2] + "/" + mainQuest.mtotalQuest[n2];
            }
        }
        for (n3 = 0; n3 < vecQuestDoing_Sub.size(); ++n3) {
            mainQuest = (MainQuest)vecQuestDoing_Sub.elementAt(n3);
            if (mainQuest.typeQuest != 0) continue;
            for (n2 = 0; n2 < mainQuest.mIdQuest.length; ++n2) {
                System.out.println(mainQuest.mIdQuest[n2] + "  " + idItem);
                if (mainQuest.mIdQuest[n2] != idItem || mainQuest.mQuestGot[n2] >= mainQuest.mtotalQuest[n2]) continue;
                int n5 = n2;
                mainQuest.mQuestGot[n5] = (short)(mainQuest.mQuestGot[n5] + 1);
                if (string2.length() > 0) {
                    string2 = string2 + " , ";
                }
                string2 = string2 + mainQuest.mQuestGot[n2] + "/" + mainQuest.mtotalQuest[n2];
            }
        }
        if (string2.length() > 0) {
            string2 = T.nhat + name + ": " + string2;
            GameCanvas.addInfoChar(string2);
        }
    }
}

