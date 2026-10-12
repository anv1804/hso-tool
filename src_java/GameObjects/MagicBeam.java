/*
 * Decompiled with CFR 0.152.
 */
package GameObjects;

import CLib.mGraphics;
import GameEffect.EffectEnd;
import GameObjects.IArrow;
import GameObjects.MagicLogic;
import GameObjects.MainObject;
import GameScreen.GameScreen;
import Model.EffectManager;
import Model.FrameImage;

public class MagicBeam
extends IArrow {
    public int power;
    public int type;
    public short effect;
    public int hpLost;
    public int frame;
    public int idfollow;
    public int idend;
    public int w;
    public int h;
    public int x;
    public int y;
    public int Efftail;
    public int Effend;
    MainObject owner;
    MainObject target;
    public MagicLogic arrow = new MagicLogic();
    byte fra;
    private FrameImage img;

    public void setAngle(int angle) {
        this.arrow.setAngle(angle);
    }

    public void setEff(int tail, int end) {
        this.Efftail = tail;
        this.Effend = end;
    }

    public void set(int type, int x, int y, int power, short effect, MainObject owner, MainObject target) {
        this.arrow.set(type, x, y, owner.getDir(), target);
        this.type = type;
        this.owner = owner;
        this.effect = effect;
        if (type == 20) {
            type = 2;
        }
        this.hpLost = power;
        this.Efftail = -1;
        this.Effend = -1;
        this.power = power;
        this.wantDestroy = false;
        this.img = new FrameImage(effect);
        this.target = target;
    }

    public void update() {
        this.arrow.updateAngle();
        this.x = this.arrow.x;
        this.y = this.arrow.y;
        if (this.Efftail != -1) {
            EffectEnd effectEnd = new EffectEnd(57, this.Efftail, this.x, this.y);
            EffectManager.addHiEffect(effectEnd);
        }
        if (this.arrow.isEnd) {
            this.onArrowTouchTarget();
        }
    }

    public void onArrowTouchTarget() {
        if (this.Effend != -1) {
            if (this.power < 0) {
                GameScreen.addEffectNum("" + this.power, this.target.x, this.target.y - this.target.hOne, 2, this.target.ID);
            }
            if (this.power > 0) {
                GameScreen.addEffectNum("+" + this.power, this.target.x, this.target.y - this.target.hOne, 0, this.target.ID);
            }
            GameScreen.addEffectEndFromSv(57, this.Effend, this.x, this.y);
        }
        this.wantDestroy = true;
    }

    public void paint(mGraphics g) {
        if (this.img != null) {
            this.img.drawFrameEffectSkill(this.fra, this.x, this.y, this.arrow.headTransform, 3, g);
        }
    }

    public void SetEffFollow(int id) {
        this.idfollow = id;
    }
}

