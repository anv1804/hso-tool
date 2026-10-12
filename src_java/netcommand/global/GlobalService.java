/*
 * Decompiled with CFR 0.152.
 */
package netcommand.global;

import CLib.TField;
import CLib.mGraphics;
import CLib.mSystem;
import CLib.mVector;
import GameObjects.MainObject;
import GameObjects.Object_Effect_Skill;
import GameObjects.item_sell;
import GameScreen.GameScreen;
import GameScreen.LoginScreen;
import GameScreen.PaintInfoGameScreen;
import InterfaceComponents.iCommand;
import Main.GameCanvas;
import Model.T;
import Skill.ListSkill;
import netcommand.Cmd_Message;
import netcommand.global.GlobalLogicHandler;

public class GlobalService
extends Cmd_Message {
    protected static GlobalService instance;
    long timeCheck;

    public static GlobalService gI() {
        if (instance == null) {
            instance = new GlobalService();
        }
        return instance;
    }

    public void login(String user, String pass, String version, String clinePro, String pro, String agent, int id, byte area) {
        this.init((byte)1);
        try {
            this.m.writer().writeUTF(user);
            this.m.writer().writeUTF(pass);
            this.m.writer().writeUTF(version);
            this.m.writer().writeUTF(clinePro);
            this.m.writer().writeUTF(pro);
            this.m.writer().writeUTF(agent);
            this.m.writer().writeByte(mGraphics.zoomLevel);
            this.m.writer().writeByte(GameCanvas.device);
            this.m.writer().writeInt(id);
            this.m.writer().writeByte(area);
            this.m.writer().writeByte((byte)(TField.isQwerty ? 1 : 0));
            this.m.writer().writeByte(GameCanvas.IndexRes);
            this.m.writer().writeByte(LoginScreen.indexInfoLogin);
            this.m.writer().writeByte(GameCanvas.lowGraphic ? 1 : 0);
            this.m.writer().writeShort(GameCanvas.IndexCharPar);
            this.m.writer().writeUTF(GameCanvas.stringPackageName);
            GlobalLogicHandler.isDisConect = false;
            GlobalLogicHandler.timeReconnect = 0L;
            GlobalLogicHandler.isMelogin = false;
        }
        catch (Exception exception) {
            // empty catch block
        }
        this.send();
        if (id == -1) {
            GameCanvas.start_Wait_Dialog(T.dangdangnhap, new iCommand(T.close, 7));
            GameCanvas.countLogin = mSystem.currentTimeMillis();
        } else {
            GameCanvas.start_Wait_Dialog(T.dangketnoilai, new iCommand(T.close, 7));
        }
    }

    public void infoTranpost() {
    }

    public void player_move(short x, short y) {
        this.init((byte)4);
        try {
            this.m.writer().writeShort(x);
            this.m.writer().writeShort(y);
        }
        catch (Exception exception) {
            // empty catch block
        }
        this.send();
    }

    public void char_info(short id) {
        this.init((byte)5);
        try {
            this.m.writer().writeShort(id);
        }
        catch (Exception exception) {
            // empty catch block
        }
        this.send();
    }

    public void requestInapPurchare(byte type, String reciep, byte index) {
        this.init((byte)-93);
        try {
            this.m.writer().writeByte(type);
            this.m.writer().writeUTF(reciep);
            this.m.writer().writeByte(index);
        }
        catch (Exception exception) {
            // empty catch block
        }
        this.send();
    }

    public void new_npc_info(short id) {
        this.init((byte)-44);
        try {
            this.m.writer().writeShort(id);
        }
        catch (Exception exception) {
            // empty catch block
        }
        this.send();
    }

    public void monster_info(short id) {
        this.init((byte)7);
        try {
            this.m.writer().writeShort(id);
        }
        catch (Exception exception) {
            // empty catch block
        }
        this.send();
    }

    public void Ok_Change_Map() {
        this.init((byte)12);
        this.send();
    }

    public void fire_monster(mVector vec, byte typekill) {
        this.init((byte)9);
        try {
            this.m.writer().writeByte(typekill);
            this.m.writer().writeByte(vec.size());
            for (int i = 0; i < vec.size(); ++i) {
                Object_Effect_Skill object_Effect_Skill = (Object_Effect_Skill)vec.elementAt(i);
                this.m.writer().writeShort(object_Effect_Skill.ID);
            }
            ListSkill.doSetTimeAtt();
        }
        catch (Exception exception) {
            // empty catch block
        }
        this.send();
        com.silverknight.HsoBridge.noteFarmPacketSent();
    }

    public void nap_tien(short type, String[] minfo) {
        this.init((byte)-53);
        try {
            this.m.writer().writeShort(type);
            this.m.writer().writeByte(minfo.length);
            for (int i = 0; i < minfo.length; ++i) {
                this.m.writer().writeUTF(minfo[i]);
            }
        }
        catch (Exception exception) {
            // empty catch block
        }
        this.send();
    }

    public void load_image_data_part_char(byte type, short id) {
        if (GameCanvas.currentScreen == GameCanvas.selectChar) {
            // empty if block
        }
        this.init((byte)-52);
        try {
            this.m.writer().writeByte(type);
            this.m.writer().writeShort(id);
        }
        catch (Exception exception) {
            // empty catch block
        }
        this.send();
    }

    public void load_image_data_auto_eff(short id) {
        this.init((byte)-49);
        try {
            this.m.writer().writeShort(id);
        }
        catch (Exception exception) {
            // empty catch block
        }
        this.send();
    }

    public void load_image(short id) {
        this.init((byte)-51);
        try {
            this.m.writer().writeShort(id);
        }
        catch (Exception exception) {
            // empty catch block
        }
        this.send();
    }

    public void create_char(byte bytclass, String name, byte bythair, byte byteye, byte bythead, byte index) {
        this.init((byte)14);
        try {
            this.m.writer().writeByte(bytclass);
            this.m.writer().writeUTF(name);
            this.m.writer().writeByte(bythair);
            this.m.writer().writeByte(byteye);
            this.m.writer().writeByte(bythead);
        }
        catch (Exception exception) {
            // empty catch block
        }
        this.send();
    }

    public void select_char(byte typeSelect, int charid) {
        this.init((byte)13);
        try {
            this.m.writer().writeByte(typeSelect);
            this.m.writer().writeInt(charid);
        }
        catch (Exception exception) {
            // empty catch block
        }
        this.send();
        GameCanvas.start_Wait_Dialog(T.pleaseWait, null);
    }

    public void getlist_from_npc(byte idnpc) {
        this.init((byte)23);
        try {
            this.m.writer().writeByte(idnpc);
        }
        catch (Exception exception) {
            // empty catch block
        }
        this.send();
    }

    public void buy_item(byte typebuy, short idbuy, short soluong) {
        this.init((byte)24);
        try {
            this.m.writer().writeByte(typebuy);
            this.m.writer().writeShort(idbuy);
            this.m.writer().writeShort(soluong);
        }
        catch (Exception exception) {
            // empty catch block
        }
        this.send();
    }

    public void quest(short id, byte main_sub, byte type) {
        this.init((byte)52);
        try {
            this.m.writer().writeShort(id);
            this.m.writer().writeByte(type);
            this.m.writer().writeByte(main_sub);
        }
        catch (Exception exception) {
            // empty catch block
        }
        this.send();
        if (GameScreen.help.setStep_Next(0, -5)) {
            ++GameScreen.help.Next;
            GameScreen.help.SaveStep((byte)0, (byte)0);
            GameScreen.help.setNext();
        } else if (GameScreen.help.setStep_Next(8, 10)) {
            GameCanvas.end_Dialog();
            GameScreen.help.p = null;
            GameScreen.help.NextStep();
            GameScreen.help.setNext();
            GameScreen.help.SaveStep();
        }
    }

    public void Dynamic_Menu(short idNPC, byte idMenu, byte index) {
        mSystem.outz("Gui command -30 idNPC = " + idNPC + " , idMenu = " + idMenu + " , index = " + index);
        this.init((byte)-30);
        try {
            this.m.writer().writeShort(idNPC);
            this.m.writer().writeByte(idMenu);
            this.m.writer().writeByte(index);
        }
        catch (Exception exception) {
            // empty catch block
        }
        this.send();
    }

    public void Item_More_Info(byte inVen_Wear, byte id) {
        this.init((byte)21);
        try {
            this.m.writer().writeByte(inVen_Wear);
            this.m.writer().writeByte(id);
        }
        catch (Exception exception) {
            // empty catch block
        }
        this.send();
    }

    public void Use_Item(byte id, byte index) {
        this.init((byte)11);
        try {
            this.m.writer().writeByte(id);
            this.m.writer().writeByte(index);
        }
        catch (Exception exception) {
            // empty catch block
        }
        this.send();
    }

    public void Use_Potion(short id) {
        this.init((byte)32);
        try {
            this.m.writer().writeShort(id);
        }
        catch (Exception exception) {
            // empty catch block
        }
        this.send();
    }

    public void delete_Item(byte type, short id, byte typedelete) {
        this.init((byte)18);
        try {
            this.m.writer().writeByte(type);
            this.m.writer().writeShort(id);
            this.m.writer().writeByte(typedelete);
        }
        catch (Exception exception) {
            // empty catch block
        }
        this.send();
    }

    public void Get_Item_Map(short id, byte type) {
        this.init((byte)20);
        try {
            this.m.writer().writeShort(id);
            this.m.writer().writeByte(type);
        }
        catch (Exception exception) {
            // empty catch block
        }
        this.send();
    }

    public void Add_Base_Skill_Point(byte type, byte index, short value) {
        this.init((byte)22);
        try {
            this.m.writer().writeByte(type);
            this.m.writer().writeByte(index);
            this.m.writer().writeShort(value);
        }
        catch (Exception exception) {
            // empty catch block
        }
        this.send();
    }

    public void Add_Base_Skill_Point(byte type, byte index) {
        this.init((byte)22);
        try {
            this.m.writer().writeByte(type);
            this.m.writer().writeByte(index);
        }
        catch (Exception exception) {
            // empty catch block
        }
        this.send();
    }

    public void getItemTem(short id) {
        this.init((byte)28);
        try {
            this.m.writer().writeShort(id);
        }
        catch (Exception exception) {
            // empty catch block
        }
        this.send();
    }

    public void gohome(byte type) {
        this.init((byte)31);
        try {
            this.m.writer().writeByte(type);
        }
        catch (Exception exception) {
            // empty catch block
        }
        this.send();
    }

    public void chatPopup(String chat) {
        if (chat == null || chat.trim().length() == 0) {
            return;
        }
        this.init((byte)27);
        try {
            this.m.writer().writeUTF(chat);
        }
        catch (Exception exception) {
            // empty catch block
        }
        this.send();
        GameCanvas.msgchat.addNewChat(T.tinden, GameScreen.player.name + ": ", chat, (byte)1, false);
    }

    public void chatTab(String name, String chat) {
        if (name == null || chat == null) {
            return;
        }
        this.init((byte)34);
        try {
            this.m.writer().writeUTF(name);
            this.m.writer().writeUTF(chat);
        }
        catch (Exception exception) {
            // empty catch block
        }
        this.send();
    }

    public void Friend(byte type, String name) {
        this.init((byte)35);
        try {
            this.m.writer().writeByte(type);
            this.m.writer().writeUTF(name);
        }
        catch (Exception exception) {
            // empty catch block
        }
        this.send();
    }

    public void Register(String user, String pass) {
        this.init((byte)39);
        try {
            this.m.writer().writeUTF(user);
            this.m.writer().writeUTF(pass);
        }
        catch (Exception exception) {
            // empty catch block
        }
        this.send();
    }

    public void Party(byte type, String name) {
        this.init((byte)48);
        try {
            this.m.writer().writeByte(type);
            if (type != 0 && type != 5 && type != 4) {
                this.m.writer().writeUTF(name);
            }
        }
        catch (Exception exception) {
            // empty catch block
        }
        this.send();
    }

    public void Buy_Sell(byte type, String name, byte typeItem, short idItem, int money) {
        this.init((byte)36);
        try {
            this.m.writer().writeByte(type);
            switch (type) {
                case 0: 
                case 1: {
                    this.m.writer().writeUTF(name);
                    break;
                }
                case 2: 
                case 3: {
                    this.m.writer().writeByte(typeItem);
                    this.m.writer().writeShort(idItem);
                    break;
                }
                case 7: {
                    this.m.writer().writeInt(money);
                    break;
                }
                case 9: {
                    this.m.writer().writeUTF(name);
                    break;
                }
            }
        }
        catch (Exception exception) {
            // empty catch block
        }
        this.send();
    }

    public void BuffMore(byte type, byte tem, mVector vec) {
        this.init((byte)40);
        try {
            this.m.writer().writeByte(type);
            this.m.writer().writeByte(tem);
            this.m.writer().writeByte(vec.size());
            for (int i = 0; i < vec.size(); ++i) {
                Object_Effect_Skill object_Effect_Skill = (Object_Effect_Skill)vec.elementAt(i);
                this.m.writer().writeShort(object_Effect_Skill.ID);
            }
        }
        catch (Exception exception) {
            // empty catch block
        }
        this.send();
    }

    public void fire_Pk(mVector vec, byte typekill) {
        this.init((byte)6);
        try {
            this.m.writer().writeByte(typekill);
            this.m.writer().writeByte(vec.size());
            for (int i = 0; i < vec.size(); ++i) {
                Object_Effect_Skill object_Effect_Skill = (Object_Effect_Skill)vec.elementAt(i);
                this.m.writer().writeShort(object_Effect_Skill.ID);
            }
        }
        catch (Exception exception) {
            // empty catch block
        }
        this.send();
    }

    public void set_Pk(byte pk) {
        this.init((byte)42);
        try {
            this.m.writer().writeByte(pk);
        }
        catch (Exception exception) {
            // empty catch block
        }
        this.send();
    }

    public void Re_Info_Other_Object(String name, byte type) {
        this.init((byte)49);
        try {
            this.m.writer().writeUTF(name);
            this.m.writer().writeByte(type);
        }
        catch (Exception exception) {
            // empty catch block
        }
        this.send();
    }

    public void Change_Area(byte area) {
        this.init((byte)51);
        try {
            this.m.writer().writeByte(area);
        }
        catch (Exception exception) {
            // empty catch block
        }
        this.send();
    }

    public void Request_Area() {
        this.init((byte)54);
        this.send();
    }

    public void Save_RMS_Server(byte type, byte id, byte[] mdata) {
        this.init((byte)55);
        try {
            this.m.writer().writeByte(type);
            this.m.writer().writeByte(id);
            int n = 0;
            if (mdata != null) {
                n = mdata.length;
            }
            this.m.writer().writeShort(n);
            for (int i = 0; i < n; ++i) {
                this.m.writer().writeByte(mdata[i]);
            }
        }
        catch (Exception exception) {
            // empty catch block
        }
        this.send();
    }

    public void set_Page(byte page) {
        this.init((byte)56);
        try {
            this.m.writer().writeByte(page);
        }
        catch (Exception exception) {
            // empty catch block
        }
        this.send();
    }

    public void chat_npc(byte idnpc) {
        this.init((byte)60);
        try {
            this.m.writer().writeByte(idnpc);
        }
        catch (Exception exception) {
            // empty catch block
        }
        this.send();
    }

    public void Mon_Capchar(byte num) {
        this.init((byte)-28);
        try {
            this.m.writer().writeByte(PaintInfoGameScreen.mValueHotKey[num]);
            mSystem.outloi("so goi len" + num);
        }
        catch (Exception exception) {
            // empty catch block
        }
        this.send();
        int n = MainObject.vecCapchar.size();
        if (n == 5) {
            MainObject.vecCapchar.removeElementAt(0);
        }
        if (!GameCanvas.isTouch && TField.isQwerty) {
            MainObject.vecCapchar.addElement("" + PaintInfoGameScreen.mValueChar[num]);
        } else {
            MainObject.vecCapchar.addElement("" + PaintInfoGameScreen.mValueHotKey[num]);
        }
        MainObject.strCapchar = "";
        for (int i = 0; i < MainObject.vecCapchar.size(); ++i) {
            MainObject.strCapchar = MainObject.strCapchar + MainObject.vecCapchar.elementAt(i);
        }
    }

    public void Update_Char_Chest(byte type, short id, byte tem, short num) {
        this.init((byte)65);
        try {
            this.m.writer().writeByte(type);
            this.m.writer().writeShort(id);
            this.m.writer().writeByte(tem);
            this.m.writer().writeShort(num);
        }
        catch (Exception exception) {
            // empty catch block
        }
        this.send();
    }

    public void Update_Pet_Container(byte type, short id, byte tem, short num) {
        mSystem.outz("Send desposit/withdraw pet message");
        this.init((byte)44);
        try {
            this.m.writer().writeByte(type);
            this.m.writer().writeShort(id);
            this.m.writer().writeByte(tem);
            this.m.writer().writeShort(num);
        }
        catch (Exception exception) {
            // empty catch block
        }
        this.send();
    }

    public void dialog_Server(short id, byte type, byte value) {
        this.init((byte)-32);
        try {
            this.m.writer().writeShort(id);
            this.m.writer().writeByte(type);
            this.m.writer().writeByte(value);
        }
        catch (Exception exception) {
            // empty catch block
        }
        this.send();
    }

    public void send_cmd_server(byte cmd) {
        this.init(cmd);
        this.send();
    }

    public void Rebuild_Item(byte type, short id, byte tem) {
        this.init((byte)67);
        try {
            this.m.writer().writeByte(type);
            this.m.writer().writeShort(id);
            this.m.writer().writeByte(tem);
        }
        catch (Exception exception) {
            // empty catch block
        }
        this.send();
    }

    public void KhamNgoc(byte type, int idItem, int idG1, int idG2, int idG3) {
        this.init((byte)-100);
        try {
            switch (type) {
                case 0: {
                    this.m.writer().writeByte(type);
                    this.m.writer().writeShort(idItem);
                    this.m.writer().writeShort(idG1);
                    this.m.writer().writeShort(idG2);
                    this.m.writer().writeShort(idG3);
                    break;
                }
                case 1: {
                    this.m.writer().writeByte(type);
                    this.m.writer().writeShort(idItem);
                    break;
                }
                case 2: {
                    this.m.writer().writeByte(type);
                    this.m.writer().writeShort(idItem);
                    this.m.writer().writeShort(idG1);
                    break;
                }
            }
        }
        catch (Exception exception) {
            // empty catch block
        }
        this.send();
    }

    public void Replace_Item(byte type, short id) {
        this.init((byte)73);
        try {
            this.m.writer().writeByte(type);
            this.m.writer().writeShort(id);
        }
        catch (Exception exception) {
            // empty catch block
        }
        this.send();
    }

    public void useMount(byte type) {
        this.init((byte)-97);
        try {
            this.m.writer().writeByte(type);
        }
        catch (Exception exception) {
            // empty catch block
        }
        this.send();
    }

    public void Thach_Dau(byte type, String name) {
        this.init((byte)68);
        try {
            this.m.writer().writeByte(type);
            this.m.writer().writeUTF(name);
        }
        catch (Exception exception) {
            // empty catch block
        }
        this.send();
    }

    public void NextClan(byte type) {
        this.init((byte)69);
        try {
            this.m.writer().writeByte(type);
        }
        catch (Exception exception) {
            // empty catch block
        }
        this.send();
    }

    public void InvenClan(byte type) {
        this.init((byte)69);
        try {
            this.m.writer().writeByte(type);
        }
        catch (Exception exception) {
            // empty catch block
        }
        this.send();
    }

    public void Add_And_AnS_MemClan(byte type, String name) {
        this.init((byte)69);
        try {
            this.m.writer().writeByte(type);
            this.m.writer().writeUTF(name);
        }
        catch (Exception exception) {
            // empty catch block
        }
        this.send();
    }

    public void ChucNang_Clan(byte type, int id) {
        this.init((byte)69);
        try {
            this.m.writer().writeByte(type);
            this.m.writer().writeInt(id);
        }
        catch (Exception exception) {
            // empty catch block
        }
        this.send();
    }

    public void gop_Xu_Luong_Clan(byte type, int num) {
        this.init((byte)69);
        try {
            this.m.writer().writeByte(type);
            this.m.writer().writeInt(num);
        }
        catch (Exception exception) {
            // empty catch block
        }
        this.send();
    }

    public void info_Mem_Clan(byte type, String name) {
        this.init((byte)69);
        try {
            this.m.writer().writeByte(type);
            this.m.writer().writeUTF(name);
        }
        catch (Exception exception) {
            // empty catch block
        }
        this.send();
    }

    public void PhongCap_Clan(byte type, byte chucvu, String str) {
        this.init((byte)69);
        try {
            this.m.writer().writeByte(type);
            this.m.writer().writeByte(chucvu);
            this.m.writer().writeUTF(str);
        }
        catch (Exception exception) {
            // empty catch block
        }
        this.send();
    }

    public void Delete_Mem_Clan(byte type, String name) {
        this.init((byte)69);
        try {
            this.m.writer().writeByte(type);
            this.m.writer().writeUTF(name);
        }
        catch (Exception exception) {
            // empty catch block
        }
        this.send();
    }

    public void Change_Slo_NoiQuy_Clan(byte type, String name) {
        this.init((byte)69);
        try {
            this.m.writer().writeByte(type);
            this.m.writer().writeUTF(name);
        }
        catch (Exception exception) {
            // empty catch block
        }
        this.send();
    }

    public void Chat_World(String name) {
        this.init((byte)71);
        try {
            this.m.writer().writeUTF(name);
        }
        catch (Exception exception) {
            // empty catch block
        }
        this.send();
    }

    public void Rebuild_Wing(byte type, int wing, short id) {
        this.init((byte)77);
        try {
            this.m.writer().writeByte(type);
            this.m.writer().writeInt(wing);
            this.m.writer().writeShort(id);
        }
        catch (Exception exception) {
            // empty catch block
        }
        this.send();
    }

    public void UpdateData() {
        this.init((byte)-57);
        try {
            this.m.writer().writeByte(GameCanvas.IndexCharPar);
        }
        catch (Exception exception) {
            // empty catch block
        }
        this.send();
    }

    public void Pet_Eat(short idpet, short iditem, byte cat, byte type) {
        this.init((byte)45);
        try {
            this.m.writer().writeShort(idpet);
            this.m.writer().writeShort(iditem);
            this.m.writer().writeByte(cat);
            this.m.writer().writeByte(type);
        }
        catch (Exception exception) {
            // empty catch block
        }
        this.send();
    }

    public void Update_Pet_Eat(byte type, short id) {
        this.init((byte)44);
        try {
            this.m.writer().writeByte(type);
            this.m.writer().writeShort(id);
        }
        catch (Exception exception) {
            // empty catch block
        }
        this.send();
        mSystem.outz("cho an pet");
    }

    public void doPaymentNokia(String content) {
        this.init((byte)-76);
        try {
            this.m.writer().writeUTF(content);
        }
        catch (Exception exception) {
            // empty catch block
        }
        this.send();
    }

    public void request_LotteryItems(byte step, byte idSelectedItem) {
        this.init((byte)-91);
        try {
            this.m.writer().writeByte(step);
            if (step == 1) {
                this.m.writer().writeByte(idSelectedItem);
            } else if (step == 2) {
                this.m.writer().writeByte(idSelectedItem);
            }
        }
        catch (Exception exception) {
            // empty catch block
        }
        this.send();
    }

    public void DoKhacItem(byte type, byte cat, short iditem) {
        this.init((byte)-91);
        try {
            this.m.writer().writeByte(type);
            this.m.writer().writeByte(cat);
            this.m.writer().writeShort(iditem);
        }
        catch (Exception exception) {
            // empty catch block
        }
        this.send();
    }

    public void sendMoreServerInfo(short idNPC, short idMenu, String[] minfo) {
        this.init((byte)-31);
        try {
            this.m.writer().writeShort(idNPC);
            this.m.writer().writeShort(idMenu);
            int n = minfo.length;
            this.m.writer().writeByte(n);
            for (int i = 0; i < n; ++i) {
                this.m.writer().writeUTF(minfo[i]);
            }
        }
        catch (Exception exception) {
            exception.printStackTrace();
        }
        this.send();
    }

    public void sendMoreServerInfo(short idNPC, short idMenu, mVector minfo) {
        this.init((byte)-31);
        try {
            this.m.writer().writeShort(idNPC);
            this.m.writer().writeShort(idMenu);
            int n = minfo.size();
            this.m.writer().writeByte(n);
            for (int i = 0; i < n; ++i) {
                String string = (String)minfo.elementAt(i);
                this.m.writer().writeUTF(string);
            }
        }
        catch (Exception exception) {
            exception.printStackTrace();
        }
        this.send();
    }

    public void arena(byte step) {
        this.init((byte)37);
        try {
            this.m.writer().writeByte(step);
        }
        catch (Exception exception) {
            // empty catch block
        }
        this.send();
    }

    public void useShip(byte index) {
        this.init((byte)-98);
        try {
            this.m.writer().writeByte(index);
        }
        catch (Exception exception) {
            // empty catch block
        }
        this.send();
    }

    public void doSendThachDau(byte type, String name) {
        this.init((byte)-101);
        try {
            this.m.writer().writeByte(type);
            this.m.writer().writeUTF(name);
        }
        catch (Exception exception) {
            // empty catch block
        }
        this.send();
    }

    public void do_Buy_Sell_Item(int type, mVector vec, String slogan, short idChar, int iditem, byte cat) {
        this.init((byte)-102);
        try {
            this.m.writer().writeByte(type);
            switch (type) {
                case 0: {
                    this.m.writer().writeByte(vec.size());
                    for (int i = 0; i < vec.size(); ++i) {
                        item_sell item_sell2 = (item_sell)vec.elementAt(i);
                        if (item_sell2 == null) continue;
                        this.m.writer().writeShort(item_sell2.id);
                        this.m.writer().writeInt(item_sell2.price);
                        this.m.writer().writeShort(item_sell2.soluuong);
                        this.m.writer().writeByte(item_sell2.cat);
                    }
                    this.m.writer().writeUTF(slogan);
                    break;
                }
                case 1: {
                    this.m.writer().writeShort(idChar);
                    break;
                }
                case 2: {
                    this.m.writer().writeShort(iditem);
                    this.m.writer().writeShort(idChar);
                    this.m.writer().writeByte(cat);
                    break;
                }
                case 5: {
                    this.m.writer().writeShort(idChar);
                    this.m.writer().writeShort(iditem);
                }
            }
            this.send();
        }
        catch (Exception exception) {
            // empty catch block
        }
    }

    public void RequestInfo_MiNuong(byte type, short id) {
        this.init((byte)-103);
        try {
            this.m.writer().writeByte(type);
            switch (type) {
                case 0: {
                    this.m.writer().writeShort(id);
                    break;
                }
                case 1: {
                    break;
                }
            }
        }
        catch (Exception exception) {
            // empty catch block
        }
        this.send();
    }

    public void RequestMaterialTemplate(short id) {
        this.init((byte)-106);
        try {
            this.m.writer().writeShort(id);
        }
        catch (Exception exception) {
            // empty catch block
        }
        this.send();
    }

    public void Hop_rac(byte type, short id, byte tem) {
        this.init((byte)-105);
        try {
            this.m.writer().writeByte(type);
            this.m.writer().writeShort(id);
            this.m.writer().writeByte(tem);
        }
        catch (Exception exception) {
            // empty catch block
        }
        this.send();
    }

    public void Hop_rac(byte type) {
        this.init((byte)-105);
        try {
            this.m.writer().writeByte(type);
        }
        catch (Exception exception) {
            // empty catch block
        }
        this.send();
    }

    public void doUseMaterial(short id) {
        this.init((byte)-107);
        try {
            this.m.writer().writeShort(id);
        }
        catch (Exception exception) {
            // empty catch block
        }
        this.send();
    }

    public void doNapple(byte typeSandbox, String receipt, String productId) {
        this.init((byte)-109);
        try {
            this.m.writer().writeByte(typeSandbox);
            this.m.writer().writeUTF(receipt);
            this.m.writer().writeUTF(productId);
        }
        catch (Exception exception) {
            // empty catch block
        }
        this.send();
    }

    public void requestinfoMount(byte type) {
        this.init((byte)-110);
        try {
            this.m.writer().writeByte(type);
        }
        catch (Exception exception) {
            // empty catch block
        }
        this.send();
    }

    public void doSendNewInapp(String productId, String token, String orderID) {
        this.init((byte)-75);
        try {
            this.m.writer().writeUTF(productId);
            this.m.writer().writeUTF(token);
            this.m.writer().writeUTF(orderID);
        }
        catch (Exception exception) {
            // empty catch block
        }
        this.send();
    }
}
