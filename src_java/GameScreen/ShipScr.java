/*
 * Decompiled with CFR 0.152.
 */
package GameScreen;

import CLib.mGraphics;
import CLib.mSystem;
import GameEffect.EffectAuto;
import GameScreen.GameScreen;
import GameScreen.MainScreen;
import InterfaceComponents.iCommand;
import Main.GameCanvas;
import Map.BackGround;
import Model.T;
import Thread_More.LoadMap;
import netcommand.global.GlobalService;

public class ShipScr
extends MainScreen {
    public static ShipScr instance;
    public byte ideffect = (byte)-1;
    public byte typeMap = 1;
    public EffectAuto eff;
    public int speed = 1;
    public short maxTime;
    public long last;
    public long curr;
    public long timeDone;
    public long timeStart;
    public boolean isSpeed;
    iCommand cmdFaster;
    String timeC;
    public int xShip;
    public int yShip;
    public short timeSpeed;
    long timeMove = 0L;

    public static ShipScr gI() {
        if (instance == null) {
            instance = new ShipScr();
        }
        return instance;
    }

    public void setShipScr(short maxTime) {
        this.isSpeed = false;
        this.left = null;
        this.right = null;
        this.center = this.cmdFaster = new iCommand(T.speedUp, -1, this);
        this.timeStart = mSystem.currentTimeMillis();
        this.maxTime = maxTime;
        this.timeDone = this.timeStart + (long)(maxTime * 1000);
        this.xShip = 0;
        this.yShip = GameCanvas.h * 2 / 3;
        this.speed = 0;
        this.timeSpeed = 0;
        this.ideffect = (byte)50;
        this.eff = new EffectAuto(this.ideffect, this.xShip, this.yShip, 0, 0, 1, 0);
        this.timeMove = maxTime * 1000 / (GameCanvas.w + 120);
    }

    public void update() {
        this.timeC = LoadMap.getTimeCountDown(this.timeStart, this.maxTime);
        BackGround.updateSky();
        this.updateShip();
        if (this.isSpeed || this.timeDone - mSystem.currentTimeMillis() <= 10000L) {
            this.center = null;
        }
        GameScreen.player.dyWater = 0;
        GameScreen.player.Action = 0;
        GameScreen.player.x = this.xShip + this.speed + 30;
        GameScreen.player.y = this.yShip - 40;
        GameScreen.player.Direction = 3;
        GameScreen.player.updateActionPerson();
        super.update();
    }

    public void updateShip() {
        this.curr = mSystem.currentTimeMillis();
        if (!this.isSpeed) {
            if (this.curr - this.last >= this.timeMove) {
                ++this.speed;
                this.last = this.curr;
            }
            if (this.timeDone - mSystem.currentTimeMillis() <= 0L) {
                this.timeDone = mSystem.currentTimeMillis() + 20000L;
                GlobalService.gI().useShip(this.typeMap);
            }
        } else {
            this.speed += 2;
            if (this.xShip + this.speed >= GameCanvas.w) {
                this.timeDone = mSystem.currentTimeMillis() + 20000L;
                GlobalService.gI().useShip(this.typeMap);
            }
        }
        if (this.eff != null) {
            this.eff.update();
            this.eff.x = this.speed;
        }
    }

    public void paint(mGraphics g) {
        if (GameCanvas.currentScreen != this) {
            return;
        }
        g.setColor(-13286542);
        g.fillRect(0, 0, GameCanvas.w, GameCanvas.h, true);
        BackGround.paint_SeaAuto(g);
        this.paintShip(g);
        GameScreen.player.paintPlayer(g, -1);
        BackGround.paint_SeaCloud(g);
        super.paint(g);
    }

    public void paintShip(mGraphics g) {
        if (this.eff != null) {
            this.eff.paint(g);
        }
    }

    public void Show() {
        this.lastScreen = GameCanvas.game;
        super.Show();
        GameCanvas.clearAll();
        GameScreen.infoGame.resetShip();
    }

    public void updatekey() {
        super.updatekey();
    }

    public void updatePointer() {
        super.updatePointer();
    }

    public void commandPointer(int index, int subIndex) {
        switch (index) {
            case -1: {
                GlobalService.gI().useShip((byte)2);
            }
        }
    }
}

