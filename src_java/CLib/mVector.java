/*
 * Decompiled with CFR 0.152.
 */
package CLib;

import java.util.Vector;

public class mVector {
    private Vector a;
    private String name;

    public mVector() {
        this.a = new Vector();
        this.name = "EffectManager";
    }

    public mVector(String a) {
        this.a = new Vector();
        this.name = a;
    }

    public mVector(Vector a) {
        this.a = a;
        this.name = "No Name";
    }

    public void addElement(Object o) {
        this.a.addElement(o);
    }

    public boolean contains(Object o) {
        return this.a.contains(o);
    }

    public int size() {
        if (this.a == null) {
            return 0;
        }
        return this.a.size();
    }

    public Object elementAt(int index) {
        if (index > -1 && index < this.a.size()) {
            return this.a.elementAt(index);
        }
        return null;
    }

    public void setElementAt(Object obj, int index) {
        if (index > -1 && index < this.a.size()) {
            this.a.setElementAt(obj, index);
        }
    }

    public int indexOf(Object o) {
        return this.a.indexOf(o);
    }

    public void removeElementAt(int index) {
        if (index > -1 && index < this.a.size()) {
            this.a.removeElementAt(index);
        }
    }

    public void removeElement(Object o) {
        this.a.removeElement(o);
    }

    public void removeAllElements() {
        this.a.removeAllElements();
    }

    public void insertElementAt(Object o, int i) {
        this.a.insertElementAt(o, i);
    }

    public Object firstElement() {
        return this.a.firstElement();
    }

    public Object lastElement() {
        return this.a.lastElement();
    }
}

