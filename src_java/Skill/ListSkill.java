/*
 * Decompiled with CFR 0.152.
 */
package Skill;

import CLib.mVector;
import GameObjects.MainBuff;
import GameObjects.MainObject;
import GameObjects.Object_Effect_Skill;
import GameObjects.Pet;
import GameObjects.Player;
import GameScreen.GameScreen;
import Main.GameCanvas;
import Model.CRes;
import Model.T;
import Skill.MainListSkill;
import Skill.Skill;
import Thread_More.LoadMap;
import netcommand.global.GlobalService;

public class ListSkill
extends MainListSkill {
    MainObject obj;
    MainObject objBeKill;
    int skill;
    int plash;
    int frame;
    int index;
    mVector vecObjBeKill;
    public byte classbuff = (byte)-1;
    public byte typebuff = 0;
    public static short GlobalCountdown = 0;
    public static long lastTime = 0L;
    public static long lastTimeAttack;
    public static int limitTimeAtt;

    public void setFireSkill(MainObject obj, mVector vec, int IndexSkill, byte classBuff) {
        if (vec == null || vec.size() <= 0) {
            return;
        }
        if (IndexSkill > MainListSkill.mSkillAllClasses[obj.clazz].length - 1) {
            return;
        }
        this.classbuff = classBuff;
        this.obj = obj;
        Object_Effect_Skill object_Effect_Skill = (Object_Effect_Skill)vec.elementAt(0);
        this.objBeKill = MainObject.get_Object(object_Effect_Skill.ID, object_Effect_Skill.tem);
        if (this.objBeKill == null && GameScreen.ObjFocus != null && GameScreen.ObjFocus.ID == object_Effect_Skill.ID) {
            this.objBeKill = GameScreen.ObjFocus;
        }
        if (this.objBeKill == null && com.silverknight.HsoBridge.pinnedPlayerId != -1 && object_Effect_Skill.ID == com.silverknight.HsoBridge.pinnedPlayerId) {
            this.objBeKill = com.silverknight.HsoBridge.findPinnedPlayer();
        }
        if (this.objBeKill == null && com.silverknight.HsoBridge.pinnedMonsterId != -1 && object_Effect_Skill.ID == com.silverknight.HsoBridge.pinnedMonsterId) {
            this.objBeKill = com.silverknight.HsoBridge.findPinnedMonster();
        }
        this.vecObjBeKill = vec;
        if (this.obj == null || this.obj.Action == 4 || this.objBeKill == null || this.objBeKill.Action == 4) {
            if (this.obj != null) {
                this.obj.KillFire = -1;
                this.obj.IDAttack = -1;
                this.obj.vecObjFocusSkill = null;
            }
            return;
        }
        this.skill = obj.typeObject == 9 ? MainListSkill.mSkillAllClasses[6][IndexSkill] : MainListSkill.mSkillAllClasses[obj.clazz][IndexSkill];
        this.index = IndexSkill;
        obj.KillFire = IndexSkill;
        this.plash = MainListSkill.mPlash[this.skill];
        this.frame = MainListSkill.mFramePlash[this.skill];
        if (obj.clazz == 3) {
            this.plash /= 2;
            this.frame /= 2;
        }
        if (obj == GameScreen.player) {
            this.FireSkill(MainListSkill.getRange(IndexSkill));
        } else {
            this.FireSkill(MainListSkill.mRange[this.skill]);
        }
    }

    public void setVecBeKill(mVector vec) {
        this.vecObjBeKill = vec;
    }

    public void FireSkill(int range) {
        boolean isPinned = (
            (this.objBeKill != null && (
                (com.silverknight.HsoBridge.pinnedPlayerId != -1 && this.objBeKill.typeObject == 0 && this.objBeKill.ID == com.silverknight.HsoBridge.pinnedPlayerId) ||
                (com.silverknight.HsoBridge.pinnedMonsterId != -1 && this.objBeKill.typeObject == 1 && this.objBeKill.ID == com.silverknight.HsoBridge.pinnedMonsterId)
            )) ||
            (this.obj == GameScreen.player && (com.silverknight.HsoBridge.pinnedPlayerId != -1 || com.silverknight.HsoBridge.pinnedMonsterId != -1))
        );
        int n2 = MainObject.getDistance(this.obj.x, this.obj.y, this.objBeKill.x, this.objBeKill.y - this.objBeKill.hOne / 4);
        if (isPinned || n2 <= range || this.obj.typeObject == 9) {
            if (this.obj != GameScreen.player || this.setMove_Skill()) {
                this.obj.toX = this.obj.x;
                this.obj.toY = this.obj.y;
                this.obj.vx = 0;
                this.obj.vy = 0;
                this.obj.posTransRoad = null;
                if (this.obj.typeObject != 9) {
                    this.obj.beginFire();
                    this.obj.PlashNow.setPlash(this.plash);
                    this.obj.KillFire = -1;
                    this.obj.Direction = this.skill == 53 ? 1 : this.setDirection(this.obj, this.objBeKill);
                    this.setEff_More();
                }
            }
        } else {
            int n3;
            int n4;
            int n5;
            int n6 = CRes.angle(this.obj.x - this.objBeKill.x, this.obj.y - this.objBeKill.y);
            int n7 = 0;
            do {
                n4 = this.objBeKill.x + CRes.cos(CRes.fixangle(n6 % 360)) * (range - 5) / 1000;
                n3 = this.objBeKill.y + CRes.sin(CRes.fixangle(n6 % 360)) * (range - 5) / 1000;
                n5 = GameCanvas.loadmap.getTile(n4, n3);
                n6 += CRes.random_Am_0(90);
                if (++n7 <= 12) continue;
                n5 = 0;
                this.obj.posTransRoad = null;
                GameCanvas.addInfoChar(T.farfocus);
                this.obj.KillFire = -1;
                this.obj.IDAttack = -1;
                this.obj.vecObjFocusSkill = null;
                this.setGoBack();
                return;
            } while (n5 == -1 || n5 == 1);
            if (this.obj == GameScreen.player) {
                this.obj.toX = this.obj.x;
                this.obj.toY = this.obj.y;
                GameScreen.player.xStopMove = 0;
                GameScreen.player.yStopMove = 0;
                this.obj.posTransRoad = GameCanvas.game.updateFindRoad(n4 / LoadMap.wTile, n3 / LoadMap.wTile, this.obj.x / LoadMap.wTile, this.obj.y / LoadMap.wTile, 12);
                Player.xFocus = -1;
                Player.yFocus = -1;
                if (this.obj.posTransRoad == null) {
                    this.obj.toX = n4;
                    this.obj.toY = n3;
                    this.obj.xFire = n4;
                    this.obj.yFire = n3;
                    this.obj.posTransRoad = null;
                } else if (this.obj.posTransRoad.length > 12) {
                    this.obj.posTransRoad = null;
                    GameCanvas.addInfoChar(T.farfocus);
                    this.setGoBack();
                    this.obj.KillFire = -1;
                    this.obj.IDAttack = -1;
                    this.obj.vecObjFocusSkill = null;
                } else {
                    this.obj.xFire = n4;
                    this.obj.yFire = n3;
                    GameScreen.player.xStopMove = this.obj.xFire;
                    GameScreen.player.yStopMove = this.obj.yFire;
                }
                this.obj.countAutoMove = 0;
            } else {
                this.obj.toX = n4;
                this.obj.xFire = n4;
                this.obj.toY = n3;
                this.obj.yFire = n3;
            }
            if (this.obj.typeObject == 1) {
                this.obj.IDAttack = this.objBeKill.ID;
            } else {
                this.obj.vecObjFocusSkill = this.vecObjBeKill;
            }
        }
    }

    public void setGoBack() {
        if (this.obj == GameScreen.player && Player.isAutoFire == 1 && Player.isCurAutoFire) {
            this.objBeKill.isSend = true;
            this.obj.toX = this.obj.x;
            this.obj.toY = this.obj.y;
            this.obj.xStopMove = Player.xBeginAutoFire;
            this.obj.yStopMove = Player.yBeginAutofire;
            this.obj.posTransRoad = GameCanvas.game.updateFindRoad(this.obj.xStopMove / LoadMap.wTile, this.obj.yStopMove / LoadMap.wTile, this.obj.x / LoadMap.wTile, this.obj.y / LoadMap.wTile, 20);
            Player.xFocus = -1;
            Player.yFocus = -1;
            if (this.obj.posTransRoad != null && this.obj.posTransRoad.length > 20) {
                this.obj.posTransRoad = null;
            }
        }
    }

    public void fireSkillFree() {
        if (this.obj == null) {
            return;
        }
        if (this.objBeKill == null) {
            this.obj.KillFire = -1;
            this.obj.IDAttack = -1;
            this.obj.vecObjFocusSkill = null;
            return;
        }
        if (this.obj != GameScreen.player || this.setMove_Skill()) {
            this.obj.toX = this.obj.x;
            this.obj.toY = this.obj.y;
            this.obj.vx = 0;
            this.obj.vy = 0;
            this.obj.beginFire();
            this.obj.PlashNow.setPlash(this.plash);
            this.obj.KillFire = -1;
            this.obj.Direction = this.skill == 53 ? 1 : this.setDirection(this.obj, this.objBeKill);
            this.setEff_More();
        }
    }

    public void updateEffSkill() {
        if (this.obj == null) {
            return;
        }
        if (this.objBeKill == null) {
            this.obj.KillFire = -1;
            this.obj.IDAttack = -1;
            this.obj.vecObjFocusSkill = null;
            return;
        }
        if (this.obj.typeObject == 9) {
            if (this.classbuff > -1) {
                GameScreen.addEffectKillSubTime(MainListSkill.mEffectKill[this.skill], this.obj.ID, this.obj.typeObject, this.vecObjBeKill, (byte)MainListSkill.mBuffAllClasses[this.classbuff][this.index], 500);
                Pet pet = (Pet)this.obj;
                pet.setState((byte)4);
            }
        } else if (this.obj.fplash == this.frame) {
            if (this.obj == GameScreen.player) {
                Skill skill = MainListSkill.getSkillFormId(this.index);
                int dur = (skill != null) ? skill.performDur : 350;
                long l = GameCanvas.timeNow - lastTime;
                if (l > (long)GlobalCountdown) {
                    GlobalCountdown = dur;
                    lastTime = GameCanvas.timeNow;
                    this.sendSkill();
                }
            }
            if (this.classbuff > -1) {
                GameScreen.addEffectKillSubTime(MainListSkill.mEffectKill[this.skill], this.obj.ID, this.obj.typeObject, this.vecObjBeKill, (byte)MainListSkill.mBuffAllClasses[this.classbuff][this.index], 1250);
            } else {
                GameScreen.addEffectKill(MainListSkill.mEffectKill[this.skill], this.obj.ID, this.obj.typeObject, this.vecObjBeKill);
                if (this.obj == GameScreen.player && this.objBeKill != null && (com.silverknight.HsoBridge.pinnedPlayerId != -1 || com.silverknight.HsoBridge.pinnedMonsterId != -1)) {
                    GameScreen.addEffectEndKill(MainListSkill.mEffectKill[this.skill], this.objBeKill.x, this.objBeKill.y - this.objBeKill.hOne / 2);
                }
            }
        }
    }

    public void setEff_More() {
        if (this.obj == GameScreen.player) {
            GameScreen.player.setAddDelaySkill(this.skill, this.index);
        }
        switch (this.skill) {
            case 23: 
            case 41: {
                GameScreen.addEffectKillTime(29, this.obj.ID, this.obj.typeObject, this.vecObjBeKill, 300, 0);
                break;
            }
            case 22: 
            case 36: 
            case 44: {
                GameScreen.addEffectKillTime(29, this.obj.ID, this.obj.typeObject, this.vecObjBeKill, 300, 3);
                break;
            }
            case 20: 
            case 46: {
                GameScreen.addEffectKillTime(29, this.obj.ID, this.obj.typeObject, this.vecObjBeKill, 300, 4);
                break;
            }
            case 18: 
            case 19: {
                GameScreen.addEffectKillTime(29, this.obj.ID, this.obj.typeObject, this.vecObjBeKill, 300, 1);
                break;
            }
            case 5: 
            case 21: {
                GameScreen.addEffectKillTime(29, this.obj.ID, this.obj.typeObject, this.vecObjBeKill, 400, 0);
                break;
            }
            case 27: 
            case 37: {
                GameScreen.addEffectKillTime(29, this.obj.ID, this.obj.typeObject, this.vecObjBeKill, 400, 3);
                break;
            }
            case 39: 
            case 51: {
                GameScreen.addEffectKillTime(29, this.obj.ID, this.obj.typeObject, this.vecObjBeKill, 400, 4);
                break;
            }
            case 24: 
            case 40: {
                GameScreen.addEffectKillTime(29, this.obj.ID, this.obj.typeObject, this.vecObjBeKill, 400, 1);
                break;
            }
            case 42: 
            case 43: {
                GameScreen.addEffectKillTime(29, this.obj.ID, this.obj.typeObject, this.vecObjBeKill, 600, 0);
                break;
            }
            case 14: 
            case 34: {
                GameScreen.addEffectKillTime(29, this.obj.ID, this.obj.typeObject, this.vecObjBeKill, 600, 3);
                break;
            }
            case 6: 
            case 47: 
            case 48: {
                GameScreen.addEffectKillTime(29, this.obj.ID, this.obj.typeObject, this.vecObjBeKill, 600, 4);
                break;
            }
            case 49: 
            case 50: {
                GameScreen.addEffectKillTime(29, this.obj.ID, this.obj.typeObject, this.vecObjBeKill, 400, 1);
                break;
            }
            case 52: {
                GameScreen.addEffectKillTime(29, this.obj.ID, this.obj.typeObject, this.vecObjBeKill, 600, 1);
                break;
            }
            case 53: {
                GameScreen.addEffectKillTime(30, this.obj.ID, this.obj.typeObject, this.vecObjBeKill, 600, 1);
                break;
            }
            case 56: 
            case 61: 
            case 62: 
            case 66: {
                GameScreen.addEffectKillTime(29, this.obj.ID, this.obj.typeObject, this.vecObjBeKill, 300, 4);
                break;
            }
            case 54: 
            case 58: 
            case 64: 
            case 68: {
                GameScreen.addEffectKillTime(29, this.obj.ID, this.obj.typeObject, this.vecObjBeKill, 600, 0);
                break;
            }
            case 55: 
            case 59: 
            case 65: 
            case 69: {
                GameScreen.addEffectKillTime(29, this.obj.ID, this.obj.typeObject, this.vecObjBeKill, 600, 3);
                break;
            }
            case 57: 
            case 60: 
            case 63: 
            case 67: {
                GameScreen.addEffectKillTime(29, this.obj.ID, this.obj.typeObject, this.vecObjBeKill, 400, 1);
                break;
            }
        }
    }

    public boolean setMove_Skill() {
        return true;
    }

    public boolean setMove(int x, int xend, int y, int yend) {
        short[] sArray = GameScreen.gI().updateFindRoad(xend / LoadMap.wTile, yend / LoadMap.wTile, x / LoadMap.wTile, y / LoadMap.wTile, 12);
        if (sArray == null) {
            int n5 = GameCanvas.loadmap.getTile(xend, yend);
            if (n5 != -1 && n5 != 1) {
                return this.setSendMove(x, xend, y, yend);
            }
            GameCanvas.addInfoChar(T.farfocus);
            this.setGoBack();
            if (this.obj != null) {
                this.obj.KillFire = -1;
                this.obj.IDAttack = -1;
                this.obj.vecObjFocusSkill = null;
            }
            return false;
        }
        if (sArray.length > 12) {
            GameCanvas.addInfoChar(T.farfocus);
            this.setGoBack();
            if (this.obj != null) {
                this.obj.KillFire = -1;
                this.obj.IDAttack = -1;
                this.obj.vecObjFocusSkill = null;
            }
            return false;
        }
        return true;
    }

    public boolean setSendMove(int x, int xend, int y, int yend) {
        int n5 = 0;
        int n6 = 0;
        int n7 = 0;
        int n8 = 0;
        int n9 = 0;
        int n10 = CRes.abs(x - xend);
        int n11 = CRes.abs(y - yend);
        if (n10 >= 4) {
            n5 = x > xend ? -6 : 6;
        }
        if (n11 >= 4) {
            n6 = y > yend ? -6 : 6;
        }
        n7 = n10 / 6;
        n8 = n11 / 6;
        n9 = n10 > n11 ? n7 : n8;
        for (int i = 0; i < n9; ++i) {
            int n12;
            if (i < n7) {
                x += n5;
            }
            if (i < n8) {
                y += n6;
            }
            if ((n12 = GameCanvas.loadmap.getTile(x, y)) != 1 && n12 != -1) continue;
            GameCanvas.addInfoChar(T.farfocus);
            this.setGoBack();
            if (this.obj != null) {
                this.obj.KillFire = -1;
                this.obj.IDAttack = -1;
            }
            return false;
        }
        return true;
    }

    public static void doSetTimeAtt() {
        lastTimeAttack = System.currentTimeMillis();
    }

    public static boolean canAttack() {
        long l = System.currentTimeMillis();
        long l2 = l - lastTimeAttack;
        return l2 > (long)limitTimeAtt;
    }

    public void sendSkill() {
        mVector mVector2;
        this.vecObjBeKill = mVector2 = GameScreen.player.setSkillLan(this.index, this.objBeKill);
        if (this.classbuff > -1) {
            this.setSendBuff(mVector2);
        } else if (this.objBeKill.typeObject == 1) {
            short origX = (short)this.obj.x;
            short origY = (short)this.obj.y;
            boolean needMove = (this.obj == GameScreen.player && this.objBeKill != null
                    && MainObject.getDistance(origX, origY, this.objBeKill.x, this.objBeKill.y) > 60);
            if (needMove) {
                com.silverknight.HsoBridge.lastRemoteMoveTime = System.currentTimeMillis();
                com.silverknight.HsoBridge.lastRemoteTargetX = (short)this.objBeKill.x;
                com.silverknight.HsoBridge.lastRemoteTargetY = (short)this.objBeKill.y;
                GlobalService.gI().player_move((short)this.objBeKill.x, (short)this.objBeKill.y);
            }
            if (mVector2.size() <= 7) {
                GlobalService.gI().fire_monster(mVector2, (byte)this.index);
            } else {
                for (int i = 0; i < mVector2.size(); i += 7) {
                    mVector chunk = new mVector("Chunked skill targets");
                    for (int j = i; j < i + 7 && j < mVector2.size(); ++j) {
                        chunk.addElement(mVector2.elementAt(j));
                    }
                    GlobalService.gI().fire_monster(chunk, (byte)this.index);
                }
            }
        } else if (this.objBeKill.typeObject == 0) {
            // Nếu đang có remote strike thread chạy -> bỏ qua, tránh gói gửi sai thứ tự
            if (com.silverknight.HsoBridge.isStrikeActive) {
                com.silverknight.HsoBridge.logDebug("sendSkill PK: skipped (strike in progress)");
                return;
            }
            int dist = MainObject.getDistance(this.obj.x, this.obj.y, this.objBeKill.x, this.objBeKill.y);
            com.silverknight.HsoBridge.logDebug("sendSkill PK: player.typePk=" + this.obj.typePk + " target=" + this.objBeKill.name + "(id=" + this.objBeKill.ID + ",typePk=" + this.objBeKill.typePk + ") dist=" + dist + " skillId=" + this.index + " targets=" + mVector2.size());
            if (this.obj == GameScreen.player && this.objBeKill != null && dist > 50) {
                com.silverknight.HsoBridge.executeRemotePkStrike(this.objBeKill, this.index, mVector2);
            } else {
                GlobalService.gI().fire_Pk(mVector2, (byte)this.index);
            }
        }
    }

    public void setSendBuff(mVector vec) {
        GlobalService.gI().BuffMore((byte)this.index, this.objBeKill.typeObject, vec);
    }

    public boolean setBuff(int type) {
        for (int i = 0; i < this.obj.vecBuff.size(); ++i) {
            MainBuff mainBuff = (MainBuff)this.obj.vecBuff.elementAt(i);
            if (mainBuff.typeBuff != type) continue;
            return false;
        }
        return true;
    }

    static {
        limitTimeAtt = 600;
    }
}

