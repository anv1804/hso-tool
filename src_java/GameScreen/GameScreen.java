/*
 * Decompiled with CFR 0.152.
 */
package GameScreen;

import Build_Indo.IndoServer;
import CLib.mFont;
import CLib.mGraphics;
import CLib.mHashtable;
import CLib.mImage;
import CLib.mSound;
import CLib.mSystem;
import CLib.mVector;
import GameEffect.AnimateEffect;
import GameEffect.EffectAuto;
import GameEffect.EffectEnd;
import GameEffect.EffectMonster;
import GameEffect.EffectNum;
import GameEffect.EffectSkill;
import GameEffect.MainEffect;
import GameObjects.DataSkillEff;
import GameObjects.GameData;
import GameObjects.IArrow;
import GameObjects.Item;
import GameObjects.MagicBeam;
import GameObjects.MainHelp;
import GameObjects.MainItem;
import GameObjects.MainMonster;
import GameObjects.MainObject;
import GameObjects.MainRMS;
import GameObjects.ObjectData;
import GameObjects.Object_Effect_Skill;
import GameObjects.Pet;
import GameObjects.Player;
import GameObjects.ThacNuoc;
import GameObjects.TimecountDown;
import GameScreen.Info_Other_Player;
import GameScreen.List_Server;
import GameScreen.MainScreen;
import GameScreen.PaintInfoGameScreen;
import GameScreen.TabScreenNew;
import InterfaceComponents.ChatTextField;
import InterfaceComponents.TabShopNew;
import InterfaceComponents.iCommand;
import Main.GameCanvas;
import Main.ImageEffect;
import Main.ImageEffectAuto;
import Map.ItemMap;
import Map.MainItemMap;
import Model.AvMain;
import Model.CRes;
import Model.CharCountDown;
import Model.EffectManager;
import Model.Menu2;
import Model.Point;
import Model.T;
import Thread_More.LoadMap;
import Thread_More.MiniMap;
import netcommand.global.GlobalService;

public class GameScreen
extends MainScreen {
    public static int ID_DLG_BUY_KC = -32059;
    public static final byte CHIEN_BINH = 0;
    public static final byte SAT_THU = 1;
    public static final byte PHAP_SU = 2;
    public static final byte XA_THU = 3;
    public static short[] idmap18;
    public static Player player;
    public static Pet pet;
    public static MainObject ObjFocus;
    public static mVector VecNum;
    public static mVector Vecplayers;
    public static mVector VecInfoServer;
    public static mVector VecInfoChar;
    public static mVector vecEffInMap;
    public static mVector vecStep;
    public static mHashtable HashImageItemMap;
    public static mHashtable HashImageNPC;
    public static mVector vecWeather;
    public static MainHelp help;
    public static PaintInfoGameScreen infoGame;
    public iCommand cmdMenu;
    public iCommand cmdGiaotiep;
    public iCommand cmdMyseft;
    public iCommand cmdAddfriend;
    public iCommand cmdInfoChar;
    public iCommand cmdChat;
    public iCommand cmdMoiParty;
    public iCommand cmdShowChat;
    public iCommand cmdNewParty;
    public iCommand cmdParty;
    public iCommand cmdBuy_Sell;
    public iCommand cmdSetWeather;
    public iCommand cmdChangeMap;
    public iCommand cmdChucNang;
    public iCommand cmdListFriend;
    public iCommand cmdAutoFire;
    public iCommand cmdSetPk;
    public iCommand cmdSetDoSat;
    public iCommand cmdEndHelp;
    public iCommand cmdAutoItem;
    public iCommand cmdAutoMPHP;
    public iCommand cmdThachDau;
    public iCommand cmdAutoBuff;
    public iCommand cmdShowAuto;
    public iCommand cmdAddMemClan;
    public iCommand cmdQuickChat;
    public iCommand cmdInfoVantieu;
    public iCommand cmdplayerStore;
    public iCommand cmdinfoMiNuong;
    public static long timeCheckDelHash;
    public static byte timePaintCmdGiaotiep;
    public static boolean isMoveCamera;
    public static boolean isFinishHelp;
    public static int xMoveCam;
    public static int yMoveCam;
    public static int xCur;
    public static int yCur;
    public static int timeResetCam;
    public static int demNumSoundEff;
    public static String nameSpecialRegion;
    public static long timeSpRegion;
    public static long timeArena;
    public static mVector vecHightEffAuto;
    public static boolean isInPetArea;
    public static mVector arrowsUp;
    public static mVector vecTimecountDown;
    public static boolean isHideOderPlayer;
    public static boolean isHideFullOderPlayer;
    public static boolean isShowHoiSinh;
    public static boolean isMapLang;
    public static String[] textServer;
    public int xRec;
    public int yRec;
    public int wRec;
    public int hRec;
    public int colorRec;
    public boolean isFullScreen = false;
    public mImage imgCombo;
    public static CharCountDown charcountdonw;
    public static mVector vecDataeff;
    public boolean isCombo = false;
    public static boolean isReArea;
    int p;
    int o;
    MainObject ob;
    MainObject maxob = new MainObject();
    public static MainItemMap tr;
    public static MainItemMap maxtr;
    public static int dx;
    public static int dy;
    public static String goiMes;
    int iii;
    public mVector menuNgua = new mVector("GameScr menungua");
    public static short[][] posSkill;
    int xStart;
    int yStart;
    int cmxMini;
    int cmyMini;

    public static GameScreen gI() {
        if (GameCanvas.game == null) {
            GameCanvas.game = new GameScreen();
        }
        return GameCanvas.game;
    }

    public void Show() {
        this.left = null;
        this.right = null;
        this.center = null;
        if (!GameCanvas.isTouch) {
            this.left = this.cmdMenu;
        }
        super.Show();
        isMoveCamera = false;
        GameCanvas.clearAll();
    }

    public void checkRemoveImage() {
        if (!GameCanvas.isTouch) {
            this.checkClear();
        }
        mSystem.freeDataChangeMap();
    }

    public GameScreen() {
        this.cmdAddfriend = new iCommand(T.addFriend, 0);
        this.cmdInfoChar = new iCommand(T.info, 1);
        this.cmdChat = new iCommand(T.trochuyen, 2);
        this.cmdMoiParty = new iCommand(T.moiParty, 3);
        this.cmdNewParty = new iCommand(T.newParty, 5);
        this.cmdBuy_Sell = new iCommand(T.buySell, 7);
        this.cmdSetWeather = new iCommand("mua", 25, this);
        this.cmdChucNang = new iCommand(T.chucnang, 14);
        this.cmdAddMemClan = new iCommand(T.addmemclan, 15);
        this.cmdGiaotiep = new iCommand(T.giaotiep, 0, this);
        this.cmdMenu = new iCommand(T.menu, 1, this);
        this.cmdShowChat = new iCommand(T.tinnhan, 4, this);
        this.cmdParty = new iCommand(T.party, 6, this);
        this.cmdSetPk = new iCommand(T.setPk, 8, this);
        this.cmdSetDoSat = new iCommand(T.dosat, 10, this);
        this.cmdChangeMap = new iCommand(T.changeArea, 12, this);
        this.cmdListFriend = new iCommand(T.listFriend, 15, this);
        this.cmdAutoFire = new iCommand(T.autoFire, 16, this);
        this.cmdEndHelp = new iCommand(T.endHelp, 17, this);
        this.cmdAutoItem = new iCommand(T.autoItem, 18, this);
        this.cmdAutoMPHP = new iCommand(T.autoHP, 19, this);
        this.cmdThachDau = new iCommand(T.thachdau, 20, this);
        this.cmdAutoBuff = new iCommand(T.autoBuff, 21, this);
        this.cmdShowAuto = new iCommand(T.info, 22, this);
        this.cmdInfoVantieu = new iCommand(T.info, 27, this);
        this.cmdQuickChat = new iCommand(T.chat, 28, this);
        this.cmdplayerStore = new iCommand(T.gianHang, 30, this);
        this.cmdinfoMiNuong = new iCommand(T.info, 31, this);
    }

    public void setCaptionCmd() {
        this.cmdAddfriend.caption = T.addFriend;
        this.cmdInfoChar.caption = T.info;
        this.cmdChat.caption = T.trochuyen;
        this.cmdMoiParty.caption = T.moiParty;
        this.cmdNewParty.caption = T.newParty;
        this.cmdBuy_Sell.caption = T.buySell;
        this.cmdChucNang.caption = T.chucnang;
        this.cmdAddMemClan.caption = T.addmemclan;
        this.cmdGiaotiep.caption = T.giaotiep;
        this.cmdMenu.caption = T.menu;
        this.cmdShowChat.caption = T.tinnhan;
        this.cmdParty.caption = T.party;
        this.cmdSetPk.caption = T.setPk;
        this.cmdSetDoSat.caption = T.dosat;
        this.cmdChangeMap.caption = T.changeArea;
        this.cmdListFriend.caption = T.listFriend;
        this.cmdAutoFire.caption = T.autoFire;
        this.cmdEndHelp.caption = T.endHelp;
        this.cmdAutoItem.caption = T.autoItem;
        this.cmdAutoMPHP.caption = T.autoHP;
        this.cmdThachDau.caption = T.thachdau;
        this.cmdAutoBuff.caption = T.autoBuff;
        this.cmdShowAuto.caption = T.info;
        this.cmdInfoVantieu.caption = T.info;
        this.cmdQuickChat.caption = T.chat;
    }

    public void commandTab(int index, int sub) {
        switch (index) {
            case 0: {
                break;
            }
            case 1: {
                break;
            }
            case 2: {
                GameCanvas.AllInfo.Show(this);
            }
        }
        super.commandTab(index, sub);
    }

    public void commandMenu(int index, int sub) {
        switch (index) {
            case 0: {
                if (Menu2.objSelect == null) break;
                GlobalService.gI().Friend((byte)0, Menu2.objSelect.name);
                break;
            }
            case 1: {
                if (Menu2.objSelect == null) break;
                GlobalService.gI().Re_Info_Other_Object(Menu2.objSelect.name, Info_Other_Player.VIEW);
                break;
            }
            case 2: {
                if (Menu2.objSelect != null) {
                    GameCanvas.msgchat.addNewChat(Menu2.objSelect.name, "", "", (byte)0, true);
                }
                GameCanvas.start_Chat_Dialog();
                break;
            }
            case 3: {
                if (Menu2.objSelect == null) break;
                GlobalService.gI().Party((byte)1, Menu2.objSelect.name);
                break;
            }
            case 5: {
                GlobalService.gI().Party((byte)0, "");
                break;
            }
            case 7: {
                if (Menu2.objSelect == null) break;
                GlobalService.gI().Buy_Sell((byte)0, Menu2.objSelect.name, (byte)0, (short)0, 0);
                break;
            }
            case 14: {
                this.MenuChucNang();
                break;
            }
            case 15: {
                if (Menu2.objSelect == null) break;
                GlobalService.gI().Add_And_AnS_MemClan((byte)10, Menu2.objSelect.name);
            }
        }
        super.commandMenu(index, sub);
    }

    public void MenuChucNang() {
        mVector mVector2 = new mVector("GameScr menu3");
        mVector2.addElement(this.cmdListFriend);
        String string = T.off + " " + T.dosat;
        if (GameScreen.player.typePk != 0) {
            mVector2.addElement(this.cmdSetPk);
            string = T.on + " " + T.dosat;
        }
        this.cmdSetDoSat.caption = string;
        mVector2.addElement(this.cmdSetDoSat);
        this.cmdAutoFire.caption = Player.isAutoFire > -1 ? T.off + T.autoFire : T.on + T.autoFire;
        mVector2.addElement(this.cmdAutoFire);
        this.cmdAutoItem.caption = Player.autoItem != null ? T.off + T.autoItem : T.on + T.autoItem;
        mVector2.addElement(this.cmdAutoItem);
        this.cmdAutoMPHP.caption = Player.isAutoHPMP ? T.off + T.autoHP : T.on + T.autoHP;
        mVector2.addElement(this.cmdAutoMPHP);
        if (GameScreen.help.Step >= 0) {
            mVector2.addElement(this.cmdEndHelp);
        }
        GameCanvas.menu2.startAt(mVector2, 2, T.chucnang, false, null);
    }

    public void doArea() {
        GlobalService.gI().Request_Area();
        isReArea = true;
        GameCanvas.start_Wait_Dialog(T.danglaydulieu, new iCommand(T.close, -1));
    }

    public void commandPointer(int index, int subIndex) {
        switch (index) {
            case 0: {
                if (infoGame.isMapThachdau()) {
                    return;
                }
                if (ObjFocus == null) break;
                if (ObjFocus.isSelling() && !GameCanvas.isTouch) {
                    this.cmdplayerStore.perform();
                    break;
                }
                if (!player.setFirePlayer(ObjFocus) || GameScreen.ObjFocus.typeObject != 0) {
                    ObjFocus.GiaoTiep();
                    if (Player.isAutoFire == 1) {
                        Player.setCurAutoFire();
                    }
                    player.resetAction();
                    timePaintCmdGiaotiep = 0;
                    break;
                }
                if (this.center != this.cmdGiaotiep) break;
                this.center = null;
                break;
            }
            case 1: {
                GameCanvas.end_Dialog();
                if (player.isSelling()) {
                    this.do_Show_SellItem();
                    break;
                }
                GameCanvas.AllInfo.Show(GameCanvas.game);
                GlobalService.gI().send_cmd_server((byte)59);
                if (help.setStep_Next(1, 9)) {
                    GameCanvas.AllInfo.selectTab = 0;
                    help.NextStep();
                    help.setNext();
                    break;
                }
                if (!help.setStep_Next(6, 2)) break;
                help.NextStep();
                help.setNext();
                break;
            }
            case 4: {
                GameCanvas.start_Chat_Dialog();
                break;
            }
            case 6: {
                if (Player.party == null) break;
                GameCanvas.start_Party_Dialog();
                break;
            }
            case 8: {
                mVector mVector2 = new mVector("GameScr menu");
                for (int i = 0; i < T.mColorPk.length; ++i) {
                    if (i <= 0 && GameScreen.player.typePk == -1) continue;
                    iCommand iCommand2 = new iCommand(T.mColorPk[i], 9, i == 0 ? -1 : i, (AvMain)this);
                    if (i > 0) {
                        iCommand2.setFraCaption(AvMain.fraPk, 3, i * 3);
                    }
                    mVector2.addElement(iCommand2);
                }
                GameCanvas.menu2.startAt(mVector2, 4, T.setPk, false, null);
                break;
            }
            case 9: {
                GlobalService.gI().set_Pk((byte)subIndex);
                break;
            }
            case 10: {
                if (GameScreen.player.typePk != 0) {
                    GameCanvas.start_Left_Dialog(T.hoibatdosat, new iCommand(T.on, 24, this));
                    break;
                }
                GlobalService.gI().set_Pk((byte)-1);
                break;
            }
            case 12: {
                this.doArea();
                break;
            }
            case 13: {
                GlobalService.gI().Change_Area((byte)subIndex);
                break;
            }
            case 15: {
                if (!List_Server.isLoadFriend) {
                    GlobalService.gI().Friend((byte)4, "");
                    GameCanvas.start_Wait_Dialog(T.pleaseWait, new iCommand(T.cancel, -1));
                    break;
                }
                List_Server.gI().vecListServer = List_Server.vecMyFriend;
                List_Server.gI().typeList = 0;
                List_Server.gI().updateList();
                List_Server.gI().setMinMax();
                List_Server.gI().setXmove();
                List_Server.gI().setCmd();
                List_Server.gI().nameList = T.listFriend;
                List_Server.gI().page = (byte)99;
                List_Server.gI().Show(this);
                break;
            }
            case 16: {
                if (Player.isAutoFire > -1) {
                    Player.isAutoFire = (byte)-1;
                    Player.isCurAutoFire = false;
                    break;
                }
                Player.isAutoFire = 0;
                Player.isCurAutoFire = true;
                break;
            }
            case 17: {
                GameScreen.help.p = null;
                GameScreen.help.Step = -1;
                GameScreen.help.Next = 0;
                help.setNext();
                help.SaveStep();
                break;
            }
            case 18: {
                if (Player.autoItem != null) {
                    Player.autoItem.isremove = true;
                    break;
                }
                GameCanvas.start_Auto_Item_Dialog();
                break;
            }
            case 19: {
                if (Player.isAutoHPMP) {
                    Player.isAutoHPMP = false;
                    MainRMS.setSaveAuto();
                    break;
                }
                GameCanvas.start_Auto_HPMP_Dialog();
                break;
            }
            case 20: {
                if (Menu2.objSelect == null) break;
                GlobalService.gI().Thach_Dau((byte)0, Menu2.objSelect.name);
                break;
            }
            case 21: {
                GameCanvas.start_Auto_Buff();
                break;
            }
            case 22: {
                PaintInfoGameScreen.isShowInfoAuto = !PaintInfoGameScreen.isShowInfoAuto;
                MainRMS.setSaveTouch();
                break;
            }
            case 23: {
                break;
            }
            case 24: {
                GlobalService.gI().set_Pk((byte)0);
                GameCanvas.end_Dialog();
                break;
            }
            case 25: {
                GameScreen.AddEffWeather((byte)0, false, -4, 300);
                break;
            }
            case 26: {
                MainItem mainItem;
                if (subIndex < 0 || subIndex > this.menuNgua.size() - 1 || (mainItem = (MainItem)this.menuNgua.elementAt(subIndex)) == null) break;
                GlobalService.gI().Use_Potion((short)mainItem.Id);
                break;
            }
            case 27: {
                GlobalService.gI().Dynamic_Menu((short)-56, (byte)0, (byte)0);
                break;
            }
            case 28: {
                mVector mVector3 = new mVector("GameScr menuchat");
                for (int i = 0; i < T.mQuickChat.length; ++i) {
                    iCommand iCommand3 = new iCommand(T.mQuickChat[i], 29, i, (AvMain)this);
                    mVector3.addElement(iCommand3);
                }
                GameCanvas.menu2.startAt(mVector3, 4, T.chat, false, null);
                break;
            }
            case 29: {
                GameScreen.player.strChatPopup = T.mQuickChat[subIndex];
                GlobalService.gI().chatPopup(T.mQuickChat[subIndex]);
                break;
            }
            case 30: {
                if (ObjFocus == null || !ObjFocus.isSelling()) break;
                GlobalService.gI().do_Buy_Sell_Item(1, null, "", (short)GameScreen.ObjFocus.ID, 0, (byte)0);
                break;
            }
            case 31: {
                if (ObjFocus == null || !ObjFocus.isMiNuong()) break;
                GlobalService.gI().RequestInfo_MiNuong((byte)0, (short)(GameScreen.ObjFocus.ID + 1000));
            }
        }
        super.commandPointer(index, subIndex);
    }

    public void doSellItem() {
        GameCanvas.end_Dialog();
        Item.VecItem_Sell_in_store.removeAllElements();
        Item.VecItemSell.removeAllElements();
        mVector mVector2 = new mVector("Sell");
        byte by = 0;
        TabShopNew tabShopNew = new TabShopNew(Item.VecInvetoryPlayer, (byte)16, T.tabhanhtrang, -1, by);
        mVector2.addElement(tabShopNew);
        mVector mVector3 = new mVector("info");
        TabShopNew tabShopNew2 = new TabShopNew(mVector3, (byte)15, T.gianHang, -1, (byte)0);
        mVector2.addElement(tabShopNew2);
        GameCanvas.shopNpc = new TabScreenNew();
        GameCanvas.shopNpc.selectTab = 0;
        GameCanvas.shopNpc.addMoreTab(mVector2);
        GameCanvas.shopNpc.Show(GameScreen.gI());
    }

    public void do_Show_SellItem() {
        GameCanvas.end_Dialog();
        mVector mVector2 = new mVector("Sell");
        byte by = 0;
        TabShopNew tabShopNew = new TabShopNew(Item.VecInvetoryPlayer, (byte)16, T.tabhanhtrang, -1, by);
        mVector2.addElement(tabShopNew);
        mVector mVector3 = new mVector("info");
        TabShopNew tabShopNew2 = new TabShopNew(mVector3, (byte)15, T.gianHang, -1, (byte)0);
        mVector2.addElement(tabShopNew2);
        GameCanvas.shopNpc = new TabScreenNew();
        GameCanvas.shopNpc.selectTab = 0;
        GameCanvas.shopNpc.addMoreTab(mVector2);
        GameCanvas.shopNpc.Show(GameScreen.gI());
    }

    public void paintMonsterEffect(mGraphics g) {
        for (int i = 0; i < EffectMonster.listEffectMonster.size(); ++i) {
            EffectMonster effectMonster = (EffectMonster)EffectMonster.listEffectMonster.elementAt(i);
            effectMonster.paint(g);
        }
    }

    public void paintMonsterDieEffect(mGraphics g) {
        for (int i = 0; i < EffectMonster.listEffectMonster.size(); ++i) {
            EffectMonster effectMonster = (EffectMonster)EffectMonster.listEffectMonster.elementAt(i);
            effectMonster.paintDie(g);
        }
    }

    public void paint(mGraphics g) {
        try {
            int n;
            int n2;
            Object object;
            int n3;
            Object object2;
            int n4;
            dx = 0;
            dy = 0;
            if (LoadMap.timeVibrateScreen > 0) {
                if (LoadMap.timeVibrateScreen > 100) {
                    dy = CRes.random_Am_0(3);
                    if (LoadMap.timeVibrateScreen == 101) {
                        LoadMap.timeVibrateScreen = 0;
                    }
                } else {
                    dy = CRes.random_Am_0(3);
                    dx = CRes.random_Am(0, 2);
                }
                --LoadMap.timeVibrateScreen;
            }
            g.translate(dx, dy);
            g.translate(-MainScreen.cameraMain.xCam, -MainScreen.cameraMain.yCam);
            if (GameCanvas.mapBack != null) {
                GameCanvas.mapBack.paint(g);
            }
            if (LoadMap.idTile == 9) {
                g.setColor(367554);
                g.fillRect(MainScreen.cameraMain.xCam, MainScreen.cameraMain.yCam, GameCanvas.w, GameCanvas.h, false);
            }
            GameCanvas.loadmap.paint(g);
            infoGame.paintShip(g);
            if (this.isFullScreen) {
                g.drawRecAlpa(0, 0, GameCanvas.loadmap.mapW * 24, GameCanvas.loadmap.mapH * 24, this.colorRec);
            } else if (this.wRec > 0) {
                g.fillRecAlpla(this.xRec, this.yRec, this.wRec, this.hRec, this.colorRec);
            }
            for (n4 = 0; n4 < vecDataeff.size(); ++n4) {
                object2 = (DataSkillEff)vecDataeff.elementAt(n4);
                if (object2 == null) continue;
                ((DataSkillEff)object2).paintTop(g, ((DataSkillEff)object2).x, ((DataSkillEff)object2).y);
            }
            for (n4 = 0; n4 < LoadMap.Thacnuoc.size(); ++n4) {
                object2 = (ThacNuoc)LoadMap.Thacnuoc.elementAt(n4);
                ((ThacNuoc)object2).paint(g);
            }
            for (n4 = 0; n4 < vecStep.size(); ++n4) {
                object2 = (Point)vecStep.elementAt(n4);
                ((Point)object2).paint(g);
            }
            for (n4 = 0; n4 < vecEffInMap.size(); ++n4) {
                object2 = (Point)vecEffInMap.elementAt(n4);
                ((Point)object2).paint(g);
            }
            if (GameScreen.help.Step >= 0 && !GameCanvas.menu2.isShowMenu && GameCanvas.currentDialog == null && GameCanvas.currentScreen == this && !ChatTextField.isShow && GameCanvas.subDialog == null && GameCanvas.currentScreen == GameCanvas.game) {
                help.paintHelpFrist(g);
            }
            if (GameCanvas.isTouch && Player.timeFocus > 0 && GameScreen.player.posTransRoad != null && Player.xFocus >= 0) {
                PaintInfoGameScreen.fraFocusIngame.drawFrame((4 - Player.timeFocus / 2) % PaintInfoGameScreen.fraFocusIngame.nFrame, Player.xFocus, Player.yFocus, 0, 3, g);
            }
            EffectManager.lowEffects.paintAll(g);
            this.paintMonsterDieEffect(g);
            ItemMap.paintDieHouseArena(g);
            try {
                this.p = 0;
                this.o = 0;
                this.maxob.y = 10000;
                GameScreen.maxtr.y = 10000;
                while (this.p < Vecplayers.size() || this.o < LoadMap.mItemMap.size()) {
                    this.ob = this.maxob;
                    tr = maxtr;
                    if (this.p < Vecplayers.size()) {
                        this.ob = (MainObject)Vecplayers.elementAt(this.p);
                    }
                    if (this.o < LoadMap.mItemMap.size()) {
                        tr = (MainItemMap)LoadMap.mItemMap.elementAt(this.o);
                    }
                    if (tr == null || this.ob.y + this.ob.ysai < GameScreen.tr.y + LoadMap.wTile) {
                        if (MainObject.isInScreen(this.ob) && !this.ob.isStop && !this.ob.isRemove) {
                            this.ob.paint(g);
                            if (this.ob.chat != null && this.ob.chat.strChat != null) {
                                this.ob.chat.paint(g);
                            }
                        }
                        ++this.p;
                        if (tr != null) continue;
                        ++this.o;
                        continue;
                    }
                    if (tr.isInScreen() && tr != null) {
                        tr.paint(g);
                    }
                    ++this.o;
                }
            }
            catch (Exception exception) {
                exception.printStackTrace();
                mSystem.outloi("loi gameScreen paint Doi tuong");
            }
            for (n3 = 0; n3 < arrowsUp.size(); ++n3) {
                object2 = (IArrow)arrowsUp.elementAt(n3);
                if (object2 == null) continue;
                ((IArrow)object2).paint(g);
            }
            for (n3 = 0; n3 < vecDataeff.size(); ++n3) {
                object2 = (DataSkillEff)vecDataeff.elementAt(n3);
                if (object2 == null) continue;
                ((DataSkillEff)object2).paintBottom(g, ((DataSkillEff)object2).x, ((DataSkillEff)object2).y);
            }
            this.paintMonsterEffect(g);
            for (n3 = 0; n3 < LoadMap.vecPointChange.size(); ++n3) {
                object2 = (Point)LoadMap.vecPointChange.elementAt(n3);
                g.drawRegion(AvMain.imgSelect, 0, 0, 12, 16, LoadMap.mTranPointChangeMap[((Point)object2).dis], ((Point)object2).x + GameCanvas.gameTick % 6 * ((Point)object2).vx, ((Point)object2).y + GameCanvas.gameTick % 6 * ((Point)object2).vy, 3, false);
                AvMain.Font3dWhite(g, ((Point)object2).name, ((Point)object2).x2 + GameCanvas.gameTick % 6 * ((Point)object2).vx, ((Point)object2).y2 + GameCanvas.gameTick % 6 * ((Point)object2).vy, ((Point)object2).f);
            }
            if (ObjFocus != null && GameScreen.ObjFocus.typeObject != 1) {
                ObjFocus.paintNameFocus(g);
            }
            n3 = VecNum.size();
            EffectManager.hiEffects.paintAll(g);
            if (vecHightEffAuto.size() > 0) {
                for (int i = 0; i < vecHightEffAuto.size(); ++i) {
                    object = (EffectAuto)vecHightEffAuto.elementAt(i);
                    if (object == null) continue;
                    ((EffectAuto)object).paint(g);
                }
            }
            for (n2 = 0; n2 < n3; ++n2) {
                object = (MainEffect)VecNum.elementAt(n2);
                if (!MainEffect.isInScreen(((MainEffect)object).x, ((MainEffect)object).y, 10, 10) || ((MainEffect)object).isRemove || ((MainEffect)object).isStop) continue;
                ((MainEffect)object).paint(g);
            }
            for (n2 = 0; n2 < vecWeather.size(); ++n2) {
                object = (AnimateEffect)vecWeather.elementAt(n2);
                ((AnimateEffect)object).paint(g);
            }
            MainObject.paintFocus(g);
            GameCanvas.resetTrans(g);
            if (timePaintCmdGiaotiep > 0 && this.cmdGiaotiep != null) {
                this.cmdGiaotiep.paint(g, this.cmdGiaotiep.xCmd, this.cmdGiaotiep.yCmd);
            }
            if (!(GameCanvas.menu2.isShowMenu && Menu2.isNPCMenu != 2 || GameCanvas.currentDialog != null && (GameScreen.help.Step != 5 || GameScreen.help.Next >= 8) && (!GameCanvas.isTouch || GameScreen.help.Step != 0 && GameScreen.help.Step != 1) || GameCanvas.currentScreen != this || ChatTextField.isShow || GameCanvas.subDialog != null || GameCanvas.currentScreen != GameCanvas.game || !PaintInfoGameScreen.isShowInGame && PaintInfoGameScreen.hShowInGame >= 100)) {
                if (GameScreen.player.currentQuest == null) {
                    String string;
                    if (GameCanvas.isTouch) {
                        g.translate(GameCanvas.w - GameCanvas.minimap.maxX * MiniMap.wMini, 0 - PaintInfoGameScreen.hShowInGame);
                    } else {
                        g.translate(GameCanvas.w - GameCanvas.minimap.maxX * MiniMap.wMini - 3, GameCanvas.h - 23 - GameCanvas.minimap.maxY * MiniMap.wMini + PaintInfoGameScreen.hShowInGame);
                    }
                    if (GameCanvas.isTouch) {
                        if (!infoGame.isMapThachdau() && !infoGame.isMapchienthanh()) {
                            AvMain.paintDialog(g, GameCanvas.minimap.maxX * MiniMap.wMini - 40, GameCanvas.minimap.maxY * MiniMap.wMini - 19, 40, 35, 1);
                            mFont.tahoma_7b_black.drawString(g, T.Area + LoadMap.getAreaPaint(), GameCanvas.minimap.maxX * MiniMap.wMini - 37 + 17, GameCanvas.minimap.maxY * MiniMap.wMini + 3, 2, false);
                        }
                        string = "";
                        if (infoGame.isMapArena(GameCanvas.loadmap.idMap)) {
                            infoGame.paintTimeHS(g);
                            string = LoadMap.getTimeArena(timeArena);
                            if (!string.equals("")) {
                                g.fillRect(GameCanvas.minimap.maxX * MiniMap.wMini - GameCanvas.w / 2 - 18, GameCanvas.minimap.maxY * MiniMap.wMini - 38, 36, 17, 0, 60, false);
                                AvMain.Font3dWhite(g, string, GameCanvas.minimap.maxX * MiniMap.wMini - GameCanvas.w / 2, GameCanvas.minimap.maxY * MiniMap.wMini - 35, 2);
                            }
                        } else {
                            string = LoadMap.getTimeSpecialRegion();
                            if (!string.equals("")) {
                                mFont.tahoma_7_white.drawString(g, string, GameCanvas.minimap.maxX * MiniMap.wMini - 42, GameCanvas.minimap.maxY * MiniMap.wMini + 3, 1, false);
                            }
                        }
                    } else {
                        string = "";
                        if (GameCanvas.isSmallScreen) {
                            mFont.tahoma_7_black.drawString(g, T.Area + LoadMap.getAreaPaint(), 14, -13, 2, false);
                            if (infoGame.isMapArena(GameCanvas.loadmap.idMap)) {
                                infoGame.paintTimeHS(g);
                                string = LoadMap.getTimeArena(timeArena);
                                if (!string.equals("")) {
                                    AvMain.Font3dWhite(g, string, 14, -30, 1);
                                }
                            } else {
                                string = LoadMap.getTimeSpecialRegion();
                                if (!string.equals("")) {
                                    mFont.tahoma_7_white.drawString(g, string, 14, -30, 1, false);
                                }
                            }
                        } else {
                            AvMain.paintDialog(g, GameCanvas.minimap.maxX * MiniMap.wMini - 37, -17, 40, 35, 1);
                            mFont.tahoma_7b_black.drawString(g, T.Area + LoadMap.getAreaPaint(), GameCanvas.minimap.maxX * MiniMap.wMini - 34 + 17, -14, 2, false);
                            if (infoGame.isMapArena(GameCanvas.loadmap.idMap)) {
                                infoGame.paintTimeHS(g);
                                string = LoadMap.getTimeArena(timeArena);
                                if (!string.equals("")) {
                                    AvMain.Font3dWhite(g, string, GameCanvas.minimap.maxX * MiniMap.wMini - 39, -14, 1);
                                }
                            } else {
                                string = LoadMap.getTimeSpecialRegion();
                                if (!string.equals("")) {
                                    mFont.tahoma_7_white.drawString(g, string, GameCanvas.minimap.maxX * MiniMap.wMini - 39, -14, 1, false);
                                }
                            }
                        }
                    }
                    GameCanvas.minimap.paint(g);
                    GameCanvas.resetTrans(g);
                    infoGame.paintPos_minimap(g, GameCanvas.w - GameCanvas.minimap.maxX * MiniMap.wMini + 21, 0);
                    if (player != null && !mSystem.isj2me) {
                        if (GameScreen.player.Action == 4) {
                            g.fillRect(0, 0, GameCanvas.w, GameCanvas.h, 0, 60, false);
                        } else if (GameScreen.player.ispaintHit) {
                            PaintInfoGameScreen.paintHitscr(g, GameScreen.player.isMaxdame);
                        }
                    }
                    infoGame.paintInfoPlayer(g, 0, 0 - PaintInfoGameScreen.hShowInGame, !GameCanvas.isSmallScreen, mFont.tahoma_7_white);
                    infoGame.paintInfoThachDau(g, 0, 0 - PaintInfoGameScreen.hShowInGame);
                    infoGame.paintInfoThachDauOtherPlayer(g, GameCanvas.w / 2 + 20, 0 - PaintInfoGameScreen.hShowInGame);
                    infoGame.paintInfoFocus(g);
                    if (!GameCanvas.isTouch && GameScreen.player.Action != 4) {
                        infoGame.paintKillPlayer(g);
                    }
                    infoGame.PaintIconPlayer(g);
                    if (!GameCanvas.menu2.isShowMenu) {
                        infoGame.paintPoiterAll(g);
                    }
                    if (PaintInfoGameScreen.numMess > 0) {
                        g.drawImage(AvMain.imgMess, PaintInfoGameScreen.xMess, PaintInfoGameScreen.yMess - PaintInfoGameScreen.hShowInGame, 0, false);
                        string = PaintInfoGameScreen.numMess + "";
                        if (PaintInfoGameScreen.numMess > 20) {
                            string = "20+";
                        }
                        AvMain.Font3dWhite(g, string, PaintInfoGameScreen.xMess + 17, PaintInfoGameScreen.yMess - 1 - PaintInfoGameScreen.hShowInGame, 0);
                    }
                }
                GameCanvas.resetTrans(g);
                if (GameScreen.infoGame.strInfoServer == null && PaintInfoGameScreen.isShowInGame && !GameCanvas.menu2.isShowMenu) {
                    if (GameCanvas.isTouch) {
                        this.paintCmd(g);
                    } else {
                        this.paintCmd_OnlyText(g);
                    }
                }
                if (this.imgCombo != null) {
                    g.drawImage(this.imgCombo, 5, 46, 0, false);
                }
            }
            GameCanvas.resetTrans(g);
            if (!GameCanvas.isVNLanguage && !IndoServer.isIndoSv) {
                if (!(isFinishHelp || GameScreen.help.Step < 0 || GameCanvas.menu2.isShowMenu || GameCanvas.currentDialog != null || ChatTextField.isShow || GameCanvas.subDialog != null || GameCanvas.currentScreen != GameCanvas.game)) {
                    help.paintHelpLast(g);
                }
            } else if (GameScreen.help.Step >= 0 && !GameCanvas.menu2.isShowMenu && GameCanvas.currentDialog == null && !ChatTextField.isShow && GameCanvas.subDialog == null && GameCanvas.currentScreen == GameCanvas.game) {
                help.paintHelpLast(g);
            }
            infoGame.paintNameMap(g);
            infoGame.paintIconClan(g);
            if (PaintInfoGameScreen.paint18plush == 1 && this.canpaint18plus()) {
                PaintInfoGameScreen.paintinfo18plush(g);
            }
            for (n = 0; n < vecTimecountDown.size(); ++n) {
                object = (TimecountDown)vecTimecountDown.elementAt(n);
                if (object == null) continue;
                ((TimecountDown)object).paint(g);
            }
            if (charcountdonw != null) {
                charcountdonw.paint(g, GameCanvas.w / 2 - 31, GameCanvas.h - 30);
            }
            GameCanvas.resetTrans(g);
            if (textServer != null && textServer.length > 0) {
                for (n = 0; n < textServer.length; ++n) {
                    AvMain.Font3dWhite(g, textServer[n], GameCanvas.w - 10, GameCanvas.minimap.maxY * MiniMap.wMini + 15 + n * GameCanvas.hText, 1);
                }
            }
        }
        catch (Exception exception) {
            exception.printStackTrace();
        }
    }

    public boolean canpaint18plus() {
        if (idmap18 != null) {
            for (int i = 0; i < idmap18.length; ++i) {
                if (idmap18[i] != GameCanvas.loadmap.idMap) continue;
                return false;
            }
        }
        return true;
    }

    public void checkPaintNamearena() {
        if (GameCanvas.gameTick % 50 == 0) {
            MainObject mainObject;
            int n;
            for (n = 0; n < Vecplayers.size() - 1; ++n) {
                mainObject = (MainObject)Vecplayers.elementAt(n);
                mainObject.Namearena = false;
            }
            for (n = 0; n < Vecplayers.size() - 1; ++n) {
                mainObject = (MainObject)Vecplayers.elementAt(n);
                if (mainObject == null || mainObject.typeObject != 0) continue;
                for (int i = n + 1; i < Vecplayers.size(); ++i) {
                    MainObject mainObject2 = (MainObject)Vecplayers.elementAt(i);
                    if (mainObject2.typeObject != 0 || mainObject2 == null || CRes.abs(mainObject.x - mainObject2.x) > 20 || CRes.abs(mainObject.y - mainObject2.y) > 20) continue;
                    mainObject.Namearena = true;
                    mainObject2.Namearena = true;
                }
            }
        }
    }

    public void update() {
        try {
            Object object;
            Object object2;
            int n;
            for (n = 0; n < vecDataeff.size(); ++n) {
                object2 = (DataSkillEff)vecDataeff.elementAt(n);
                if (object2 == null) continue;
                ((DataSkillEff)object2).update();
                if (!((DataSkillEff)object2).wantDetroy) continue;
                vecDataeff.removeElement(object2);
                --n;
            }
            for (n = 0; n < vecEffInMap.size(); ++n) {
                object2 = (Point)vecEffInMap.elementAt(n);
                ((Point)object2).updateInMap();
                if (!((Point)object2).isRemove) continue;
                vecEffInMap.removeElement(object2);
                --n;
            }
            for (n = 0; n < vecStep.size(); ++n) {
                object2 = (Point)vecStep.elementAt(n);
                ((Point)object2).updateInMap();
                if (!((Point)object2).isRemove) continue;
                vecStep.removeElement(object2);
                --n;
            }
            for (n = 0; n < vecTimecountDown.size(); ++n) {
                object2 = (TimecountDown)vecTimecountDown.elementAt(n);
                ((TimecountDown)object2).update();
                if (!((TimecountDown)object2).wantdestroy) continue;
                vecTimecountDown.removeElement(object2);
            }
            if (charcountdonw != null) {
                charcountdonw.update();
                if (GameScreen.charcountdonw.WantDestroy) {
                    charcountdonw = null;
                }
            }
            for (n = 0; n < LoadMap.Thacnuoc.size(); ++n) {
                object2 = (ThacNuoc)LoadMap.Thacnuoc.elementAt(n);
                ((ThacNuoc)object2).update();
            }
            if (GameScreen.help.Step >= 0) {
                help.updateHelp();
            }
            if (Player.timeFocus > 0) {
                --Player.timeFocus;
            }
            if (GameCanvas.mapBack != null) {
                GameCanvas.mapBack.updateCloud();
            }
            for (n = 0; n < LoadMap.mItemMap.size(); ++n) {
                object2 = (MainItemMap)LoadMap.mItemMap.elementAt(n);
                if (((MainItemMap)object2).TypeItem != 1) continue;
                ((MainItemMap)object2).update();
            }
            for (n = 0; n < Vecplayers.size(); ++n) {
                object2 = (MainObject)Vecplayers.elementAt(n);
                if (((MainObject)object2).isStop && !((MainObject)object2).isRemove) {
                    ++((MainObject)object2).timeStop;
                    if (((MainObject)object2).timeStop < 5) continue;
                    ((MainObject)object2).isRemove = true;
                    continue;
                }
                if (object2 == null || ((MainObject)object2).isRemove) {
                    Vecplayers.removeElementAt(n);
                    --n;
                    continue;
                }
                ((MainObject)object2).update();
                ((MainObject)object2).ySort = ((MainObject)object2).y + ((MainObject)object2).ysai;
            }
            for (n = arrowsUp.size() - 1; n >= 0; --n) {
                object2 = (IArrow)arrowsUp.elementAt(n);
                ((IArrow)object2).update();
                if (!((IArrow)object2).wantDestroy) continue;
                arrowsUp.removeElementAt(n);
            }
            CRes.quickSort(Vecplayers);
            this.checkPaintNamearena();
            for (n = 0; n < VecNum.size(); ++n) {
                object2 = (MainEffect)VecNum.elementAt(n);
                if (object2 == null || ((MainEffect)object2).isRemove) {
                    VecNum.removeElement(object2);
                    --n;
                    continue;
                }
                if (((MainEffect)object2).isStop) continue;
                ((MainEffect)object2).update();
            }
            if (LoadMap.isShowEffAuto == LoadMap.EFF_PHOBANG_END) {
                MainScreen.cameraMain.moveCamera(MainScreen.cameraMain.xLimit / 2, MainScreen.cameraMain.yLimit / 2);
            } else if (GameScreen.player.currentQuest == null) {
                if (isMoveCamera) {
                    MainScreen.cameraMain.setCameraWithLim(xCur - xMoveCam, yCur - yMoveCam);
                } else {
                    MainScreen.cameraMain.moveCamera(GameScreen.player.x - GameCanvas.hw, GameScreen.player.y - GameCanvas.hh);
                }
            } else {
                MainObject mainObject = MainObject.get_Object(GameScreen.player.currentQuest.idNPCChat, (byte)2);
                if (mainObject != null) {
                    MainScreen.cameraMain.moveCamera(GameScreen.player.x - GameCanvas.hw, mainObject.y - GameCanvas.hh - GameCanvas.hh / 4);
                } else {
                    MainScreen.cameraMain.moveCamera(GameScreen.player.x - GameCanvas.hw, GameScreen.player.y - GameCanvas.hh);
                }
            }
            MainScreen.cameraMain.UpdateCamera();
            GameCanvas.minimap.miniCamera.moveCamera((GameScreen.player.x / LoadMap.wTile - GameCanvas.minimap.maxX / 2) * MiniMap.wMini, (GameScreen.player.y / LoadMap.wTile - GameCanvas.minimap.maxY / 2) * MiniMap.wMini);
            GameCanvas.minimap.miniCamera.UpdateCamera();
            boolean bl = false;
            for (int i = 0; i < vecWeather.size(); ++i) {
                bl = true;
                object = (AnimateEffect)vecWeather.elementAt(i);
                ((AnimateEffect)object).update();
                if (((AnimateEffect)object).type == 4) {
                    if (!((AnimateEffect)object).isStop) continue;
                    vecWeather.removeElement(object);
                    --i;
                    continue;
                }
                if (((AnimateEffect)object).type == 5) {
                    if (!((AnimateEffect)object).isStop || ((AnimateEffect)object).list.size() != 0) continue;
                    vecWeather.removeElement(object);
                    --i;
                    continue;
                }
                if (((AnimateEffect)object).list.size() != 0) continue;
                vecWeather.removeElement(object);
                --i;
            }
            if (bl) {
                AnimateEffect.updateWind();
            }
            if (GameCanvas.gameTick % 200 == 0) {
                ImageEffect.SetRemove();
                ImageEffectAuto.setRemove();
                DataSkillEff.SetRemove();
                CRes.setRemoveCharPartInfo();
                GameData.SetRemove();
                MainObject.SetRemove();
                EffectAuto.SetRemove();
            }
            if (ObjFocus != null && GameScreen.ObjFocus.isRemove) {
                ObjFocus = null;
            }
            if (Player.autoItem != null && Player.autoItem.isremove) {
                Player.autoItem = null;
                MainRMS.setSaveAuto();
            }
            if (timePaintCmdGiaotiep > 0) {
                if (this.cmdGiaotiep != null) {
                    MainObject mainObject = MainObject.get_Object(this.cmdGiaotiep.IdGiaotiep, (byte)0);
                    if (mainObject == null) {
                        timePaintCmdGiaotiep = 0;
                    } else {
                        this.cmdGiaotiep.setPosXY(mainObject.x - MainScreen.cameraMain.xCam, mainObject.y - MainScreen.cameraMain.yCam - mainObject.hOne - 30);
                    }
                }
                timePaintCmdGiaotiep = (byte)(timePaintCmdGiaotiep - 1);
            }
            if (!GameCanvas.isTouch && (GameCanvas.timeNow - timeCheckDelHash) / 1000L > 300L) {
                this.checkClear();
            }
            if (demNumSoundEff > 0 && GameCanvas.gameTick % 30 == 0) {
                --demNumSoundEff;
            }
            EffectManager.hiEffects.updateAll();
            EffectManager.lowEffects.updateAll();
            if (vecHightEffAuto.size() > 0) {
                for (int i = 0; i < vecHightEffAuto.size(); ++i) {
                    object = (EffectAuto)vecHightEffAuto.elementAt(i);
                    if (((EffectAuto)object).wantdestroy) {
                        vecHightEffAuto.removeElement(object);
                    }
                    if (object == null) continue;
                    ((EffectAuto)object).update();
                }
            }
            infoGame.countTimeHS();
            for (int i = 0; i < EffectMonster.listEffectMonster.size(); ++i) {
                object = (EffectMonster)EffectMonster.listEffectMonster.elementAt(i);
                ((EffectMonster)object).update();
            }
        }
        catch (Exception exception) {
            exception.printStackTrace();
        }
    }

    public void checkClear() {
        timeCheckDelHash = GameCanvas.timeNow;
        ObjectData.checkDelHash(MainMonster.HashImageMonster);
        ObjectData.checkDelHash(HashImageItemMap);
        ObjectData.checkDelHash(Item.HashImageIconClan);
    }

    public void updatekey() {
        if (!Player.isLockKey) {
            super.updatekey();
        }
        if (player != null) {
            player.updateKey();
        }
    }

    public void updatePointer() {
        if (GameCanvas.isTouch) {
            if (timePaintCmdGiaotiep > 0 && this.cmdGiaotiep != null) {
                this.cmdGiaotiep.updatePointer();
            }
            super.updatePointer();
            infoGame.updatePoiterAll();
        }
    }

    public void doMenu() {
        if (GameScreen.player.Action == 1) {
            player.resetMove();
            GameScreen.player.posTransRoad = null;
            player.resetAction();
        }
        mVector mVector2 = new mVector("GameScr menu2");
        mVector2.addElement(this.cmdMyseft);
        mVector2.addElement(this.cmdChucNang);
        if (Player.party != null) {
            mVector2.addElement(this.cmdParty);
        }
        if (GameScreen.player.typePk == 0 || ObjFocus != null && GameScreen.ObjFocus.typePk == 0) {
            mVector2.addElement(this.cmdGiaotiep);
        }
        mVector2.addElement(this.cmdChangeMap);
        GameCanvas.menu2.startAt(mVector2, 2, T.menuChinh, false, null);
        if (help.setStep_Next(1, 9) || help.setStep_Next(6, 2)) {
            Menu2.isHelp = true;
        }
        player.resetAction();
    }

    public void doMenuUseNgua(mVector vec) {
        this.menuNgua = new mVector("GameScr menungua2");
        this.menuNgua = vec;
        mVector mVector3 = new mVector("GameScr newvec");
        for (int i = 0; i < vec.size(); ++i) {
            MainItem mainItem = (MainItem)vec.elementAt(i);
            if (mainItem == null) continue;
            String string = mainItem.itemName;
            iCommand iCommand2 = new iCommand(string, 26, i, (AvMain)this);
            mVector3.addElement(iCommand2);
            GameCanvas.menu2.startAt(mVector3, 2, T.TuseNgua, false, null);
        }
    }

    public void doMenuAuto() {
        mVector mVector2 = new mVector("GameScr menu3");
        GameScreen.gI().cmdAutoFire.caption = Player.isAutoFire > -1 ? T.off + T.autoFire : T.on + T.autoFire;
        mVector2.addElement(GameScreen.gI().cmdAutoFire);
        GameScreen.gI().cmdAutoItem.caption = Player.autoItem != null ? T.off + T.autoItem : T.on + T.autoItem;
        mVector2.addElement(GameScreen.gI().cmdAutoItem);
        GameScreen.gI().cmdAutoMPHP.caption = Player.isAutoHPMP ? T.off + T.autoHP : T.on + T.autoHP;
        mVector2.addElement(GameScreen.gI().cmdAutoMPHP);
        mVector2.addElement(GameScreen.gI().cmdAutoBuff);
        this.cmdShowAuto.caption = T.on + T.showAuto;
        if (PaintInfoGameScreen.isShowInfoAuto) {
            this.cmdShowAuto.caption = T.off + T.showAuto;
        }
        mVector2.addElement(GameScreen.gI().cmdShowAuto);
        GameCanvas.menu2.startAt(mVector2, 2, T.auto, false, null);
    }

    public static void addEffectKill(int type, int idFrom, byte temFrom, mVector vec) {
        Object_Effect_Skill object_Effect_Skill = new Object_Effect_Skill((short)idFrom, temFrom);
        if (mSystem.isj2me && infoGame.ismapHouse(GameCanvas.loadmap.idMap)) {
            boolean bl = false;
            if (idFrom == GameScreen.player.ID) {
                bl = true;
            } else if (EffectSkill.countSkillArena <= 3) {
                EffectSkill.countSkillArena = (byte)(EffectSkill.countSkillArena + 1);
                bl = true;
            } else {
                bl = false;
            }
            GameScreen.StartAddEffectSkill(type, object_Effect_Skill, vec);
        } else {
            GameScreen.StartAddEffectSkill(type, object_Effect_Skill, vec);
        }
    }

    public static void StartAddEffectSkill(int type, Object_Effect_Skill objfire, mVector vec) {
        if (type == 119) {
            EffectSkill effectSkill = new EffectSkill(114, objfire, vec, 0);
            GameScreen.addEffect2Vector(effectSkill);
            EffectSkill effectSkill2 = new EffectSkill(115, objfire, vec, 0);
            GameScreen.addEffect2Vector(effectSkill2);
        } else if (type == 118) {
            EffectSkill effectSkill = new EffectSkill(65, objfire, vec, 0);
            GameScreen.addEffect2Vector(effectSkill);
            mVector mVector3 = new mVector();
            for (int i = 0; i < vec.size(); ++i) {
                Object_Effect_Skill object_Effect_Skill2 = (Object_Effect_Skill)vec.elementAt(i);
                if (object_Effect_Skill2 == null) continue;
                mVector3.addElement(object_Effect_Skill2);
                EffectSkill effectSkill3 = new EffectSkill(62, objfire, mVector3, 0);
                GameScreen.addEffect2Vector(effectSkill3);
                mVector3.removeAllElements();
            }
        } else if (type == 117) {
            EffectSkill effectSkill = new EffectSkill(20, objfire, vec, 0);
            GameScreen.addEffect2Vector(effectSkill);
            EffectSkill effectSkill4 = new EffectSkill(91, objfire, vec, 0);
            GameScreen.addEffect2Vector(effectSkill4);
        } else if (type == 116) {
            EffectSkill effectSkill = new EffectSkill(53, objfire, vec, 0);
            GameScreen.addEffect2Vector(effectSkill);
            EffectSkill effectSkill5 = new EffectSkill(77, objfire, vec, 0);
            GameScreen.addEffect2Vector(effectSkill5);
        } else if (type == 123) {
            EffectSkill effectSkill = new EffectSkill(60, objfire, vec, 0);
            GameScreen.addEffect2Vector(effectSkill);
            mVector mVector4 = new mVector();
            for (int i = 0; i < vec.size(); ++i) {
                Object_Effect_Skill object_Effect_Skill3 = (Object_Effect_Skill)vec.elementAt(i);
                if (object_Effect_Skill3 == null) continue;
                mVector4.addElement(object_Effect_Skill3);
                EffectSkill effectSkill6 = new EffectSkill(49, objfire, mVector4, 0);
                GameScreen.addEffect2Vector(effectSkill6);
                mVector4.removeAllElements();
            }
        } else if (type == 122) {
            mVector mVector5 = new mVector();
            for (int i = 0; i < vec.size(); ++i) {
                Object_Effect_Skill object_Effect_Skill4 = (Object_Effect_Skill)vec.elementAt(i);
                if (object_Effect_Skill4 == null) continue;
                mVector5.addElement(object_Effect_Skill4);
                EffectSkill effectSkill = new EffectSkill(51, objfire, mVector5, 0);
                GameScreen.addEffect2Vector(effectSkill);
                mVector5.removeAllElements();
            }
            EffectSkill effectSkill = new EffectSkill(66, objfire, vec, 0);
            GameScreen.addEffect2Vector(effectSkill);
        } else if (type == 121) {
            EffectSkill effectSkill = new EffectSkill(34, objfire, vec, 0);
            GameScreen.addEffect2Vector(effectSkill);
            mVector mVector6 = new mVector();
            for (int i = 0; i < vec.size(); ++i) {
                Object_Effect_Skill object_Effect_Skill5 = (Object_Effect_Skill)vec.elementAt(i);
                if (object_Effect_Skill5 == null) continue;
                mVector6.addElement(object_Effect_Skill5);
                EffectSkill effectSkill7 = new EffectSkill(55, objfire, mVector6, 0);
                GameScreen.addEffect2Vector(effectSkill7);
                mVector6.removeAllElements();
            }
        } else if (type == 120) {
            mVector mVector7 = new mVector();
            for (int i = 0; i < vec.size(); ++i) {
                Object_Effect_Skill object_Effect_Skill6 = (Object_Effect_Skill)vec.elementAt(i);
                if (object_Effect_Skill6 == null) continue;
                mVector7.addElement(object_Effect_Skill6);
                EffectSkill effectSkill = new EffectSkill(54, objfire, mVector7, 0);
                GameScreen.addEffect2Vector(effectSkill);
                mVector7.removeAllElements();
            }
            EffectSkill effectSkill = new EffectSkill(11, objfire, vec, 0);
            GameScreen.addEffect2Vector(effectSkill);
        } else if (infoGame.isMapchienthanh() || infoGame.ismapHouse(GameCanvas.loadmap.idMap)) {
            if (objfire.ID == GameScreen.player.ID) {
                EffectSkill effectSkill = new EffectSkill(type, objfire, vec, 0);
                GameScreen.addEffect2Vector(effectSkill);
            } else {
                EffectSkill effectSkill = new EffectSkill(type, objfire, vec, 0);
                if (effectSkill.levelPaint == -1) {
                    if (EffectManager.hiEffects.size() <= 20) {
                        GameScreen.addEffect2Vector(effectSkill);
                    }
                } else if (EffectManager.lowEffects.size() <= 20) {
                    GameScreen.addEffect2Vector(effectSkill);
                }
            }
        } else {
            EffectSkill effectSkill = new EffectSkill(type, objfire, vec, 0);
            GameScreen.addEffect2Vector(effectSkill);
        }
    }

    public static void addEffectKill(int type, int idFrom, byte temFrom, int idTo, byte temTo, int hpshow, int hplast) {
        mVector mVector2 = new mVector("GameScr vec3");
        Object_Effect_Skill object_Effect_Skill = new Object_Effect_Skill((short)idTo, temTo);
        object_Effect_Skill.setHP(hpshow, hplast);
        mVector2.addElement(object_Effect_Skill);
        Object_Effect_Skill object_Effect_Skill2 = new Object_Effect_Skill((short)idFrom, temFrom);
        EffectSkill effectSkill = new EffectSkill(type, object_Effect_Skill2, mVector2, 0);
        GameScreen.addEffect2Vector(effectSkill);
    }

    public static void addEffectKill(int type, int idFrom, byte temFrom, int idTo, byte temTo, int hpshow, int hplast, byte sub) {
        mVector mVector2 = new mVector("GameScr vec");
        Object_Effect_Skill object_Effect_Skill = new Object_Effect_Skill((short)idTo, temTo);
        object_Effect_Skill.setHP(hpshow, hplast);
        mVector2.addElement(object_Effect_Skill);
        Object_Effect_Skill object_Effect_Skill2 = new Object_Effect_Skill((short)idFrom, temFrom);
        EffectSkill effectSkill = new EffectSkill(type, object_Effect_Skill2, mVector2, sub);
        GameScreen.addEffect2Vector(effectSkill);
    }

    public static void addEffectKill(int type, int idFrom, byte temFrom, mVector vec, byte sub) {
        Object_Effect_Skill object_Effect_Skill = new Object_Effect_Skill((short)idFrom, temFrom);
        if (!EffectSkill.isMultiTarget(type)) {
            EffectSkill effectSkill = new EffectSkill(type, object_Effect_Skill, vec, sub);
            GameScreen.addEffect2Vector(effectSkill);
        } else {
            for (int i = 0; i < vec.size(); ++i) {
                mVector mVector3 = new mVector("GameScr vector");
                Object_Effect_Skill object_Effect_Skill2 = (Object_Effect_Skill)vec.elementAt(i);
                mVector3.addElement(object_Effect_Skill2);
                EffectSkill effectSkill = new EffectSkill(type, object_Effect_Skill, mVector3, sub);
                GameScreen.addEffect2Vector(effectSkill);
            }
        }
    }

    public static void StartEffect_Chiemthanh(int type, int idFrom, byte temFrom, mVector vec, byte sub) {
        block3: {
            MainObject mainObject;
            block2: {
                mainObject = MainObject.get_Object(idFrom, temFrom);
                if (mainObject.x > 216) break block2;
                for (int i = 0; i < 3; ++i) {
                    EffectSkill effectSkill = new EffectSkill(mainObject.x, mainObject.y + 12, mainObject.x + posSkill[0][i], mainObject.y + posSkill[1][i], (byte)0);
                    EffectManager.addHiEffect(effectSkill);
                }
                break block3;
            }
            if (mainObject.x < 384) break block3;
            for (int i = 0; i < 3; ++i) {
                EffectSkill effectSkill = new EffectSkill(mainObject.x, mainObject.y + 12, mainObject.x + posSkill[0][i], mainObject.y + posSkill[1][i], (byte)1);
                EffectManager.addHiEffect(effectSkill);
            }
        }
    }

    public static void addEffectKillSubTime(int type, int idFrom, byte temFrom, mVector vec, byte sub, int time) {
        Object_Effect_Skill object_Effect_Skill = new Object_Effect_Skill((short)idFrom, temFrom);
        EffectSkill effectSkill = new EffectSkill(type, object_Effect_Skill, vec, sub);
        effectSkill.timeRemove = time;
        GameScreen.addEffect2Vector(effectSkill);
    }

    public static void addEffectKillSubTime(int type, int idFrom, byte temFrom, int idTo, byte temTo, int hpshow, int hplast, byte sub, int time) {
        mVector mVector2 = new mVector("GameScr vec2");
        Object_Effect_Skill object_Effect_Skill = new Object_Effect_Skill((short)idTo, temTo);
        object_Effect_Skill.setHP(hpshow, hplast);
        mVector2.addElement(object_Effect_Skill);
        Object_Effect_Skill object_Effect_Skill2 = new Object_Effect_Skill((short)idFrom, temFrom);
        EffectSkill effectSkill = new EffectSkill(type, object_Effect_Skill2, mVector2, sub);
        GameScreen.addEffect2Vector(effectSkill);
    }

    public static MainItemMap addEffectAuto(String key, String value) {
        EffectAuto effectAuto = new EffectAuto(key, value);
        return effectAuto;
    }

    public static void addEffectKillTime(int type, int idFrom, byte temFrom, mVector vec, int timeRemove, int subType) {
        Object_Effect_Skill object_Effect_Skill = new Object_Effect_Skill((short)idFrom, temFrom);
        EffectSkill effectSkill = new EffectSkill(type, object_Effect_Skill, vec, subType);
        effectSkill.timeRemove = timeRemove;
        GameScreen.addEffect2Vector(effectSkill);
    }

    public static void addEffect2Vector(MainEffect eff) {
        if (eff.levelPaint != -1) {
            EffectManager.addHiEffect(eff);
        } else {
            EffectManager.addLowEffect(eff);
        }
    }

    public static void addEffectEndKill(int type, int x, int y) {
        EffectEnd effectEnd = new EffectEnd(type, x, y);
        GameScreen.addEffect2Vector(effectEnd);
    }

    public static void addEffectEndFromSv(int type, int id, int x, int y) {
        EffectEnd effectEnd = new EffectEnd(type, id, x, y);
        GameScreen.addEffect2Vector(effectEnd);
    }

    public static void addEffectEndKill_Direction(int type, int x, int y, int direction, byte levelPaint) {
        EffectEnd effectEnd = new EffectEnd(type, x, y, direction, levelPaint);
        GameScreen.addEffect2Vector(effectEnd);
    }

    public static void addEffectEndKill_Time(int type, int x, int y, long timeRemove) {
        EffectEnd effectEnd = new EffectEnd(type, x, y, timeRemove);
        GameScreen.addEffect2Vector(effectEnd);
    }

    public static void addEffectKill(int type, int x, int y, int time, short id, byte tem) {
        EffectSkill effectSkill = new EffectSkill(type, x, y, time, id, tem);
        GameScreen.addEffect2Vector(effectSkill);
    }

    public static void addEffectNew(int type, int x, int y, int time, short id, byte tem, short idFrom, byte temFrom, boolean addLow) {
        EffectSkill effectSkill = new EffectSkill(type, x, y, time, id, tem);
        effectSkill.setObjFrom(idFrom, temFrom);
        if (addLow) {
            EffectManager.addLowEffect(effectSkill);
        } else {
            EffectManager.addHiEffect(effectSkill);
        }
    }

    public static void addEffectNum(String content, int x, int y, int typeColor, int idFrom) {
        if (infoGame.isMapchienthanh() && idFrom != GameScreen.player.ID) {
            return;
        }
        EffectNum effectNum = new EffectNum(content, x, y, typeColor);
        int n5 = GameScreen.find_Index_Stop(VecNum);
        if (n5 == VecNum.size()) {
            VecNum.addElement(effectNum);
        } else {
            VecNum.setElementAt(effectNum, n5);
        }
    }

    public static void addEffectNum(String content, int x, int y, int typeColor) {
        EffectNum effectNum = new EffectNum(content, x, y, typeColor);
        int n4 = GameScreen.find_Index_Stop(VecNum);
        if (n4 == VecNum.size()) {
            VecNum.addElement(effectNum);
        } else {
            VecNum.setElementAt(effectNum, n4);
        }
    }

    public static void addEffectNum(String content, int x, int y, int typeColor, int sub, int idFrom) {
        if (infoGame.isMapchienthanh() && idFrom != GameScreen.player.ID) {
            return;
        }
        EffectNum effectNum = new EffectNum(content, x, y, typeColor, sub);
        int n6 = GameScreen.find_Index_Stop(VecNum);
        if (n6 == VecNum.size()) {
            VecNum.addElement(effectNum);
        } else {
            VecNum.setElementAt(effectNum, n6);
        }
    }

    public static void addPlayer(MainObject obj) {
        Vecplayers.addElement(obj);
    }

    public short[] updateFindRoad(int xF, int yF, int xTo, int yTo, int maxSize) {
        int n6;
        int n7;
        int n8;
        short s;
        short s2;
        int n9;
        int n10;
        int n11;
        block31: {
            block32: {
                if (MainObject.getDistance(xF * LoadMap.wTile, yF * LoadMap.wTile, xTo * LoadMap.wTile, yTo * LoadMap.wTile) <= LoadMap.wTile) {
                    return null;
                }
                this.xStart = (byte)this.cmxMini;
                this.yStart = (byte)this.cmyMini;
                xF -= this.xStart;
                yF -= this.yStart;
                xTo -= this.xStart;
                yTo -= this.yStart;
                for (n11 = 0; n11 < GameCanvas.loadmap.mapFind.length; ++n11) {
                    for (n10 = 0; n10 < GameCanvas.loadmap.mapFind[n11].length; ++n10) {
                        n9 = (this.yStart + n10) * GameCanvas.loadmap.mapW + (this.xStart + n11);
                        if (n9 >= GameCanvas.loadmap.mapType.length - 1) continue;
                        GameCanvas.loadmap.mapFind[n11][n10] = (byte)(GameCanvas.loadmap.mapType[n9] == 1 || GameCanvas.loadmap.mapType[n9] == -1 ? -1 : 0);
                    }
                }
                n11 = 0;
                n10 = xF;
                n9 = yF;
                s2 = (short)n10;
                s = (short)n9;
                GameCanvas.loadmap.mapFind[n10][n9] = 1;
                n11 = 2;
                n8 = GameCanvas.loadmap.mapFind.length;
                n7 = GameCanvas.loadmap.mapFind[0].length;
                int n12 = 0;
                do {
                    if (++n12 > 1000) {
                        short[] sArray = new short[maxSize + 1];
                        return sArray;
                    }
                    int n13 = -1;
                    int n14 = -1;
                    if (n10 + 1 < n8 && GameCanvas.loadmap.mapFind[n10 + 1][n9] == 0) {
                        GameCanvas.loadmap.mapFind[n10 + 1][n9] = (byte)n11;
                        n13 = n10 + 1;
                        n14 = n9;
                    }
                    if (n10 - 1 >= 0 && GameCanvas.loadmap.mapFind[n10 - 1][n9] == 0) {
                        GameCanvas.loadmap.mapFind[n10 - 1][n9] = (byte)n11;
                        if (n13 != -1) {
                            if (CRes.setDis(n13, n14, xTo, yTo) > CRes.setDis(n10 - 1, n9, xTo, yTo)) {
                                n13 = n10 - 1;
                                n14 = n9;
                            }
                        } else {
                            n13 = n10 - 1;
                            n14 = n9;
                        }
                    }
                    if (n9 + 1 < n7 && GameCanvas.loadmap.mapFind[n10][n9 + 1] == 0) {
                        GameCanvas.loadmap.mapFind[n10][n9 + 1] = (byte)n11;
                        if (n13 != -1) {
                            if (CRes.setDis(n13, n14, xTo, yTo) > CRes.setDis(n10, n9 + 1, xTo, yTo)) {
                                n13 = n10;
                                n14 = n9 + 1;
                            }
                        } else {
                            n13 = n10;
                            n14 = n9 + 1;
                        }
                    }
                    if (n9 - 1 >= 0 && GameCanvas.loadmap.mapFind[n10][n9 - 1] == 0) {
                        GameCanvas.loadmap.mapFind[n10][n9 - 1] = (byte)n11;
                        if (n13 != -1) {
                            if (CRes.setDis(n13, n14, xTo, yTo) > CRes.setDis(n10, n9 - 1, xTo, yTo)) {
                                n13 = n10;
                                n14 = n9 - 1;
                            }
                        } else {
                            n13 = n10;
                            n14 = n9 - 1;
                        }
                    }
                    n6 = -1;
                    if (n13 != -1) {
                        n6 = 0;
                        n10 = n13;
                        n9 = n14;
                    } else {
                        n9 = 1000;
                        n10 = 1000;
                    }
                    for (int n15 = 0; n15 < n8; n15 = (int)((short)(n15 + 1))) {
                        for (int n16 = 0; n16 < n7; n16 = (int)((short)(n16 + 1))) {
                            if (GameCanvas.loadmap.mapFind[n15][n16] <= 1 || !this.setContinue(n15, n16, GameCanvas.loadmap.mapFind) || GameCanvas.loadmap.mapFind[n15][n16] + CRes.setDis(n15, n16, xTo, yTo) >= n11 + CRes.setDis(n10, n9, xTo, yTo)) continue;
                            n10 = n15;
                            n9 = n16;
                            n11 = GameCanvas.loadmap.mapFind[n15][n16];
                            n6 = 0;
                        }
                    }
                    if (n10 == xTo && n9 == yTo) break block31;
                    if (n6 != 0) break block32;
                } while ((n11 = (int)((short)(n11 + 1))) <= maxSize);
                short[] sArray = new short[n11];
                return sArray;
            }
            short[] sArray = new short[maxSize + 1];
            return sArray;
        }
        if (n11 >= 127) {
            short[] sArray = new short[maxSize + 1];
            return sArray;
        }
        n6 = 0;
        short[] sArray = new short[n11];
        while (true) {
            sArray[n6] = (short)((n10 << 8) + n9);
            if (n10 + 1 < n8 && GameCanvas.loadmap.mapFind[n10 + 1][n9] == GameCanvas.loadmap.mapFind[n10][n9] - 1) {
                n10 = (short)(n10 + 1);
            } else if (n10 - 1 >= 0 && GameCanvas.loadmap.mapFind[n10 - 1][n9] == GameCanvas.loadmap.mapFind[n10][n9] - 1) {
                n10 = (short)(n10 - 1);
            } else if (n9 + 1 < n7 && GameCanvas.loadmap.mapFind[n10][n9 + 1] == GameCanvas.loadmap.mapFind[n10][n9] - 1) {
                n9 = (short)(n9 + 1);
            } else if (n9 - 1 >= 0 && GameCanvas.loadmap.mapFind[n10][n9 - 1] == GameCanvas.loadmap.mapFind[n10][n9] - 1) {
                n9 = (short)(n9 - 1);
            }
            if (n10 == s2 && n9 == s) break;
            ++n6;
        }
        sArray[n11 - 1] = (short)((xF << 8) + yF);
        return sArray;
    }

    private boolean setContinue(int i, int j, byte[][] mapFind) {
        if (i + 1 < mapFind.length && mapFind[i + 1][j] == 0) {
            return true;
        }
        if (i - 1 >= 0 && mapFind[i - 1][j] == 0) {
            return true;
        }
        if (j + 1 < mapFind[i].length && mapFind[i][j + 1] == 0) {
            return true;
        }
        return j - 1 >= 0 && mapFind[i][j - 1] == 0;
    }

    public static int find_Index_Stop(mVector vec) {
        int n = vec.size();
        for (int i = 0; i < vec.size(); ++i) {
            MainEffect mainEffect = (MainEffect)vec.elementAt(i);
            if (!mainEffect.isStop || mainEffect.isRemove) continue;
            return i;
        }
        return n;
    }

    public static void Remove_ChangeMap() {
        LoadMap.mItemMap.removeAllElements();
        vecStep.removeAllElements();
        vecDataeff.removeAllElements();
    }

    public static void Remove_ChangeArea() {
    }

    public static void RemoveLoadMap() {
        for (int i = 0; i < Vecplayers.size(); ++i) {
            MainObject mainObject = (MainObject)Vecplayers.elementAt(i);
            if (mainObject == player || mainObject == pet) continue;
            mainObject.isRemove = true;
            mainObject.isBinded = false;
            mainObject.isDongBang = false;
            mainObject.isSleep = false;
            mainObject.isStun = false;
            mainObject.isMoveOut = false;
            mainObject.isno = false;
            mainObject.isnoBoss84 = false;
        }
        VecNum.removeAllElements();
        vecStep.removeAllElements();
        vecDataeff.removeAllElements();
        EffectManager.hiEffects.reMoveAll();
        EffectManager.lowEffects.reMoveAll();
        GameScreen.player.effAuto = null;
        MiniMap.vecNPC_Map.removeAllElements();
        vecHightEffAuto.removeAllElements();
        GameScreen.player.isBinded = false;
        GameScreen.player.isSleep = false;
        GameScreen.player.isStun = false;
        GameScreen.player.isDongBang = false;
        GameScreen.player.isMoveOut = false;
        GameScreen.player.isno = false;
        GameScreen.player.isnoBoss84 = false;
        ItemMap.isPaintDieHouseArena = false;
    }

    public static boolean isWater(int x, int y) {
        return GameCanvas.loadmap.getTile(x, y) == 2;
    }

    public static void AddEffWeather(byte type, boolean isSt, int num, int time) {
        AnimateEffect animateEffect = new AnimateEffect(type, isSt, num, time);
        vecWeather.addElement(animateEffect);
    }

    public static boolean setIsInScreen(int x, int y) {
        return x >= MainScreen.cameraMain.xCam && x <= MainScreen.cameraMain.xCam + GameCanvas.w && y >= MainScreen.cameraMain.yCam && y <= MainScreen.cameraMain.yCam + GameCanvas.h;
    }

    public static void addSoundEff(int id) {
        if (demNumSoundEff <= 3) {
            mSound.playSound(id, mSound.volumeSound);
            ++demNumSoundEff;
        }
    }

    public static void addEffInMap(int x, int y, int type, int Dir) {
        if (GameCanvas.lowGraphic) {
            return;
        }
        if (type == 0) {
            if (GameCanvas.gameTick % 4 == 0) {
                mSound.playSound(46, mSound.volumeSound);
            }
        } else if (type == 1 && GameCanvas.gameTick % 4 == 0) {
            mSound.playSound(51, mSound.volumeSound);
        }
        if (LoadMap.idTile != 3 && LoadMap.idTile != 8) {
            return;
        }
        Point point = new Point();
        point.x = x;
        point.y = y;
        point.color = type;
        point.fRe = 40;
        if (point.color == 0) {
            point.fRe = CRes.random(28, 43);
            point.maxframe = 1;
            point.isSmall = true;
            point.fSmall = CRes.random(point.fRe / 2 - 5, point.fRe / 2 + 6);
        } else if (point.color == 1) {
            return;
        }
        if (Dir == 1) {
            point.dis = 0;
        } else if (Dir == 0) {
            point.dis = 3;
        } else if (Dir == 2) {
            point.dis = 6;
        } else if (Dir == 3) {
            point.dis = 5;
        }
        if (point.color == 0) {
            vecStep.addElement(point);
        } else {
            vecEffInMap.addElement(point);
        }
    }

    public static void checkAddEff(int type, int x, int y, int time, short id, byte tem, short idFrom, byte temFrom, boolean addLow) {
        if (addLow) {
            for (int i = 0; i < EffectManager.lowEffects.size(); ++i) {
                MainEffect mainEffect = (MainEffect)EffectManager.lowEffects.elementAt(i);
                if (mainEffect == null || mainEffect.isRemove || mainEffect.typeEffect != type || mainEffect.objBeKillMain == null || mainEffect.objBeKillMain.ID != id) continue;
                mainEffect.setTimeRemove((short)time);
                mainEffect.objBeKillMain.vx = 0;
                mainEffect.objBeKillMain.vy = 0;
                mainEffect.objBeKillMain.toX = x;
                mainEffect.objBeKillMain.toY = y;
                if (type == 101) {
                    mainEffect.objBeKillMain.isSleep = true;
                } else if (type == 102) {
                    mainEffect.objBeKillMain.isStun = true;
                } else if (type == 107) {
                    mainEffect.objBeKillMain.isno = true;
                } else if (type == 103) {
                    mainEffect.objBeKillMain.isnoBoss84 = true;
                } else if (type == 100) {
                    mainEffect.objBeKillMain.isDongBang = true;
                } else {
                    mainEffect.objBeKillMain.isBinded = true;
                }
                return;
            }
            GameScreen.addEffectNew(type, x, y, time, id, tem, idFrom, (byte)1, type != 100);
        } else {
            for (int i = 0; i < EffectManager.hiEffects.size(); ++i) {
                MainEffect mainEffect = (MainEffect)EffectManager.hiEffects.elementAt(i);
                if (mainEffect == null || mainEffect.isRemove || mainEffect.typeEffect != type || mainEffect.objBeKillMain == null || mainEffect.objBeKillMain.ID != id) continue;
                mainEffect.setTimeRemove((short)time);
                mainEffect.objBeKillMain.vx = 0;
                mainEffect.objBeKillMain.vy = 0;
                mainEffect.objBeKillMain.toX = x;
                mainEffect.objBeKillMain.toY = y;
                if (type == 101) {
                    mainEffect.objBeKillMain.isSleep = true;
                } else if (type == 102) {
                    mainEffect.objBeKillMain.isStun = true;
                } else if (type == 107) {
                    mainEffect.objBeKillMain.isno = true;
                } else if (type == 103) {
                    mainEffect.objBeKillMain.isnoBoss84 = true;
                } else {
                    mainEffect.objBeKillMain.isBinded = true;
                }
                return;
            }
            GameScreen.addEffectNew(type, x, y, time, id, tem, idFrom, (byte)1, false);
        }
    }

    public static MainObject findOwner(MainObject owner) {
        for (int i = 0; i < Vecplayers.size(); ++i) {
            MainObject mainObject2 = (MainObject)Vecplayers.elementAt(i);
            if (mainObject2 == null || !mainObject2.findOwner(owner)) continue;
            return mainObject2;
        }
        return null;
    }

    public static MainObject findChar(short id) {
        for (int i = 0; i < Vecplayers.size(); ++i) {
            MainObject mainObject = (MainObject)Vecplayers.elementAt(i);
            if (mainObject == null || mainObject.typeObject != 0 || mainObject.ID != id) continue;
            return mainObject;
        }
        return null;
    }

    public static MainObject findMonster(short id) {
        for (int i = 0; i < Vecplayers.size(); ++i) {
            MainObject mainObject = (MainObject)Vecplayers.elementAt(i);
            if (mainObject == null || mainObject.typeObject != 1 || mainObject.ID != id) continue;
            return mainObject;
        }
        return null;
    }

    public static MainObject findObjByteCat(short id, byte cat) {
        for (int i = 0; i < Vecplayers.size(); ++i) {
            MainObject mainObject = (MainObject)Vecplayers.elementAt(i);
            if (mainObject == null || mainObject.typeObject != cat || mainObject.ID != id) continue;
            return mainObject;
        }
        return null;
    }

    public static MainObject findObj(short id) {
        for (int i = 0; i < Vecplayers.size(); ++i) {
            MainObject mainObject = (MainObject)Vecplayers.elementAt(i);
            if (mainObject == null || mainObject.ID != id) continue;
            return mainObject;
        }
        return null;
    }

    public static String converSecon2minutes(int n) {
        int n2 = n % 60;
        int n3 = n / 60 % 60;
        if (n3 <= 0) {
            return n2 + "";
        }
        return n3 + ":" + n2;
    }

    public static void startNewMagicBeam(int type, MainObject from, MainObject to, int x, int y, int power, short effect, int effTail, int effEnd) {
        MagicBeam magicBeam = new MagicBeam();
        magicBeam.set(type, x, y, power, effect, from, to);
        magicBeam.setEff(effTail, effEnd);
        arrowsUp.addElement(magicBeam);
    }

    public boolean isGameScr() {
        return true;
    }

    static {
        player = new Player(0, (byte)0, "unname", 0, 0);
        pet = null;
        ObjFocus = null;
        VecNum = new mVector("GameScr VecNum");
        Vecplayers = new mVector("GameScr Vecplayers");
        VecInfoServer = new mVector("GameScr VecInfoServer");
        VecInfoChar = new mVector("GameScr menu");
        vecEffInMap = new mVector("GameScr vecEffInMap");
        vecStep = new mVector("GameScr vecStep");
        HashImageItemMap = new mHashtable();
        HashImageNPC = new mHashtable();
        vecWeather = new mVector("GameScr vecWeather");
        help = new MainHelp();
        infoGame = new PaintInfoGameScreen();
        timePaintCmdGiaotiep = 0;
        isMoveCamera = false;
        isFinishHelp = false;
        demNumSoundEff = 0;
        nameSpecialRegion = "";
        timeSpRegion = 0L;
        timeArena = -1L;
        vecHightEffAuto = new mVector("Hight EffectAuto");
        isInPetArea = false;
        arrowsUp = new mVector();
        vecTimecountDown = new mVector("Time");
        isHideOderPlayer = false;
        isHideFullOderPlayer = false;
        isShowHoiSinh = false;
        isMapLang = false;
        vecDataeff = new mVector();
        isReArea = false;
        maxtr = new MainItemMap();
        dx = 0;
        dy = 0;
        goiMes = "";
        posSkill = new short[][]{{-72, 0, 120}, {144, 192, 120}};
    }
}
