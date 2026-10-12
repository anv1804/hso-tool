/*
 * Decompiled with CFR 0.152.
 */
package GameObjects;

import CLib.mVector;
import GameObjects.MainObject;
import GameObjects.ObjectParty;
import GameScreen.GameScreen;

public class MainParty {
    public String nameMain;
    public mVector vecPartys = new mVector("MainParty vecPartys");
    public boolean isClear = false;

    public MainParty(String name, short Lv, int idMap, byte idarea) {
        this.nameMain = name;
        this.vecPartys.removeAllElements();
        this.isClear = false;
        this.addObjParty(name, Lv, idMap, idarea);
    }

    public void addObjParty(String name, short Lv, int idMap, byte idarea) {
        ObjectParty objectParty = new ObjectParty(name, Lv, idMap, idarea);
        objectParty.maxhp = 10;
        objectParty.hp = 10;
        this.vecPartys.addElement(objectParty);
    }

    public void removeObj(String name) {
        for (int i = 0; i < this.vecPartys.size(); ++i) {
            ObjectParty objectParty = (ObjectParty)this.vecPartys.elementAt(i);
            if (objectParty.name.compareTo(name) != 0) continue;
            objectParty.isRemove = true;
            break;
        }
    }

    public void setPos(String name, int x, int y, int hp, int maxhp) {
        for (int i = 0; i < this.vecPartys.size(); ++i) {
            ObjectParty objectParty = (ObjectParty)this.vecPartys.elementAt(i);
            if (objectParty.name.compareTo(name) != 0) continue;
            objectParty.x = x;
            objectParty.y = y;
            if (hp > maxhp) {
                hp = maxhp;
            }
            objectParty.hp = hp;
            objectParty.maxhp = maxhp;
            break;
        }
    }

    public boolean update() {
        if (this.isClear) {
            this.vecPartys.removeAllElements();
            return true;
        }
        for (int i = 0; i < this.vecPartys.size(); ++i) {
            ObjectParty objectParty = (ObjectParty)this.vecPartys.elementAt(i);
            if (!objectParty.isRemove) continue;
            this.vecPartys.removeElement(objectParty);
            --i;
        }
        return false;
    }

    public void setIsParty() {
        Object object;
        int n;
        for (n = 0; n < this.vecPartys.size(); ++n) {
            object = (ObjectParty)this.vecPartys.elementAt(n);
            ((ObjectParty)object).ischeck = false;
        }
        block1: for (n = 0; n < GameScreen.Vecplayers.size(); ++n) {
            object = (MainObject)GameScreen.Vecplayers.elementAt(n);
            if (((MainObject)object).typeObject != 0 || object == GameScreen.player) continue;
            ((MainObject)object).isParty = false;
            for (int i = 0; i < this.vecPartys.size(); ++i) {
                ObjectParty objectParty = (ObjectParty)this.vecPartys.elementAt(i);
                if (objectParty.ischeck || objectParty.name.compareTo(((MainObject)object).name) != 0) continue;
                ((MainObject)object).isParty = true;
                objectParty.ischeck = true;
                continue block1;
            }
        }
    }

    public void setObjParty(MainObject obj) {
        for (int i = 0; i < this.vecPartys.size(); ++i) {
            ObjectParty objectParty = (ObjectParty)this.vecPartys.elementAt(i);
            if (objectParty.name.compareTo(obj.name) != 0) continue;
            obj.isParty = true;
            break;
        }
    }
}

