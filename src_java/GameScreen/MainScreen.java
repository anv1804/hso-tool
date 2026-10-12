/*
 * Decompiled with CFR 0.152.
 */
package GameScreen;

import CLib.mGraphics;
import Main.GameCanvas;
import Model.AvMain;
import Model.mCamera;

public class MainScreen
extends AvMain {
    MainScreen mainScreen;
    public MainScreen lastScreen;
    public static mCamera cameraMain;
    public static mCamera cameraSub;
    public String name = "";

    public void Show() {
        GameCanvas.clearKeyPressed();
        GameCanvas.currentScreen = this;
        GameCanvas.end_Dialog();
        GameCanvas.menu2.isShowMenu = false;
    }

    public void Show(MainScreen screen) {
        if (screen != null) {
            this.lastScreen = screen;
        }
        GameCanvas.clearKeyPressed();
        GameCanvas.currentScreen = this;
        GameCanvas.end_Dialog();
        GameCanvas.menu2.isShowMenu = false;
    }

    public void paint(mGraphics g) {
        super.paint(g);
    }

    public void update() {
    }

    public void keyPress(int keyCode) {
    }

    public void keyBack() {
    }

    public boolean isGameScr() {
        return false;
    }
}

