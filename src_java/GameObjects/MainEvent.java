/*
 * Decompiled with CFR 0.152.
 */
package GameObjects;

import Main.GameCanvas;
import Model.AvMain;
import netcommand.global.GlobalService;

public class MainEvent
extends AvMain {
    public int IDObj;
    public int IDCmd;
    public int isNew;
    public int numThachDau;
    public String nameEvent;
    public String contentEvent;
    public static final byte CHAT = 0;
    public static final byte KET_BAN = 1;
    public static final byte PARTY = 2;
    public static final byte BUY_SELL = 3;
    public static final byte THACH_DAU = 4;
    public static final byte MOI_VAO_CLAN = 5;
    public boolean isRemove = false;

    public MainEvent(int Obj, int Cmd, String name, String content) {
        this.IDObj = Obj;
        this.IDCmd = Cmd;
        this.nameEvent = name;
        this.contentEvent = content;
    }

    public void commandPointer(int index, int subIndex) {
        switch (index) {
            case 1: {
                GlobalService.gI().Friend((byte)subIndex, this.nameEvent);
                this.isRemove = true;
                GameCanvas.end_Dialog();
                break;
            }
            case 2: {
                if (subIndex == 1) {
                    GlobalService.gI().Party((byte)2, this.nameEvent);
                }
                this.isRemove = true;
                GameCanvas.end_Dialog();
                break;
            }
            case 3: {
                if (subIndex == 1) {
                    GlobalService.gI().Buy_Sell((byte)1, this.nameEvent, (byte)0, (short)0, 0);
                }
                this.isRemove = true;
                GameCanvas.end_Dialog();
                break;
            }
            case 4: {
                if (subIndex == 1) {
                    GlobalService.gI().Thach_Dau((byte)1, this.nameEvent);
                } else if (subIndex == 2) {
                    GlobalService.gI().Re_Info_Other_Object(this.nameEvent, (byte)1);
                }
                this.isRemove = true;
                GameCanvas.end_Dialog();
                break;
            }
            case 5: {
                if (subIndex == 1) {
                    GlobalService.gI().Add_And_AnS_MemClan((byte)11, this.nameEvent);
                }
                this.isRemove = true;
                GameCanvas.end_Dialog();
                break;
            }
        }
    }
}

