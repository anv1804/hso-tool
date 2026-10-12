/*
 * Decompiled with CFR 0.152.
 */
package Thread_More;

import CLib.mSystem;
import InterfaceComponents.IAction;
import Model.CRes;

public class Load_Data_And_Img {
    public static LoadData load = new LoadData();

    public static class LoadData
    implements Runnable {
        int iWpsPlash = -1;
        int jWpsPlash = -1;
        int iImgWeaPone = -1;
        int jImgWeaPone = -1;
        int indexWeapone = -1;
        int indexImgEffect = -1;
        int indexTreeImg = -1;
        int indexMap = -1;
        int indexArrow = -1;
        IAction iaLoadMap;
        public boolean isInitGame = false;
        public byte[] byteMap = null;

        public void loadWpsPlash(int i, int j) {
            if (this.iWpsPlash == -1 && this.jWpsPlash == -1) {
                this.iWpsPlash = i;
                this.jWpsPlash = j;
            }
        }

        public void loadImgWeaPone(int i, int j, int index) {
            if (this.iImgWeaPone == -1 && this.jImgWeaPone == -1) {
                this.iImgWeaPone = i;
                this.jImgWeaPone = j;
                this.indexWeapone = index;
            }
        }

        public void run() {
            while (true) {
                try {
                    Thread.sleep(500L);
                }
                catch (InterruptedException interruptedException) {
                    interruptedException.printStackTrace();
                    mSystem.println("loi Cres 6");
                }
                if (this.iWpsPlash != -1 && this.jWpsPlash != -1) {
                    CRes.GetWPSplashInfo(0, this.iWpsPlash, this.jWpsPlash);
                    this.iWpsPlash = -1;
                    this.jWpsPlash = -1;
                }
                if (this.iImgWeaPone == -1 || this.jImgWeaPone == -1) continue;
                CRes.getImgWeaPone(this.iImgWeaPone, this.jImgWeaPone, this.indexWeapone);
                this.iImgWeaPone = -1;
                this.jImgWeaPone = -1;
            }
        }
    }
}

