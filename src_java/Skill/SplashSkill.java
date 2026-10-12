/*
 * Decompiled with CFR 0.152.
 */
package Skill;

import GameObjects.MainObject;

public class SplashSkill {
    public int typePlash = 0;
    public int min;
    boolean isSung = false;
    public static final byte PLASH_MIN_4 = 0;
    public static final byte PLASH_MIN_6 = 1;
    public static final byte PLASH_MIN_12 = 2;
    public static final byte PLASH_SUNG_0 = 3;
    public static final byte PLASH_2KIEM_LV2_LV4 = 4;
    public static final byte PLASH_MIN_8 = 5;
    public static final byte PLASH_SUNG_LASER = 6;
    public static final byte PLASH_MIN_16 = 7;
    public static final byte PLASH_MIN_10 = 8;
    public static final byte PLASH_BUFF = 9;
    public static final byte PLASH_SUNG_LV4 = 10;
    public static final byte PLASH_SUNG_MIN_8 = 11;
    public static final byte PLASH_KIEM_LV5 = 12;
    public static final byte PLASH_SUNG_BAODAN = 13;
    public static final byte PLASH_NULL = 14;
    public static final byte PLASH_SUNG_DAYDAN = 15;
    public static final byte PLASH_SUNG_MIN_6 = 16;

    public void setPlash(int typeP) {
        this.typePlash = typeP;
        this.isSung = false;
        switch (this.typePlash) {
            case 0: {
                this.min = 4;
                break;
            }
            case 1: {
                this.min = 6;
                break;
            }
            case 6: {
                this.min = 30;
                this.isSung = true;
                break;
            }
            case 2: {
                this.min = 12;
                break;
            }
            case 5: {
                this.min = 8;
                break;
            }
            case 10: {
                this.min = 22;
                this.isSung = true;
                break;
            }
            case 7: {
                this.min = 16;
                break;
            }
            case 8: {
                this.min = 10;
                break;
            }
            case 11: {
                this.min = 11;
                this.isSung = true;
                break;
            }
            case 16: {
                this.min = 6;
                this.isSung = true;
            }
        }
    }

    public void update(MainObject obj) {
        switch (this.typePlash) {
            case 3: {
                if (obj.fplash < 2) {
                    obj.frame = 5;
                    obj.weapon_frame = 4;
                    break;
                }
                obj.setEye(1);
                obj.weapon_frame = obj.fplash + 2;
                if (obj.fplash < 6) {
                    obj.frame = 4;
                    break;
                }
                obj.Action = 0;
                obj.frame = 0;
                obj.f = 0;
                obj.weapon_frame = 0;
                break;
            }
            case 13: {
                if (obj.fplash < 12) {
                    obj.frame = 5;
                    obj.weapon_frame = 4;
                    break;
                }
                if (obj.fplash < 30) {
                    if (obj.fplash < 14) {
                        obj.weapon_frame = obj.fplash - 8;
                        obj.frame = 4;
                        break;
                    }
                    if (obj.fplash % 2 == 0) {
                        obj.setEye(1);
                        obj.weapon_frame = 4;
                        obj.frame = 5;
                        break;
                    }
                    obj.weapon_frame = 7;
                    obj.frame = 4;
                    break;
                }
                obj.Action = 0;
                obj.frame = 0;
                obj.f = 0;
                obj.weapon_frame = 0;
                break;
            }
            case 15: {
                if (obj.fplash < 26) {
                    obj.frame = 5;
                    obj.weapon_frame = 4;
                    if (obj.fplash < 12) break;
                    obj.setEye(1);
                    break;
                }
                if (obj.fplash < 30) {
                    obj.weapon_frame = obj.fplash - 22;
                    obj.frame = 4;
                    if (obj.weapon_frame <= 7) break;
                    obj.weapon_frame = 7;
                    break;
                }
                obj.Action = 0;
                obj.frame = 0;
                obj.f = 0;
                obj.weapon_frame = 0;
                break;
            }
            case 4: {
                if (obj.fplash < 4) {
                    obj.frame = 4;
                    obj.weapon_frame = 4;
                    break;
                }
                obj.setEye(1);
                obj.weapon_frame = obj.fplash;
                if (obj.fplash < 10) {
                    obj.frame = 5;
                    break;
                }
                obj.Action = 0;
                obj.frame = 0;
                obj.f = 0;
                obj.weapon_frame = 0;
                break;
            }
            case 12: {
                if (obj.fplash < 30) break;
                obj.Action = 0;
                obj.frame = 0;
                obj.f = 0;
                obj.weapon_frame = 0;
                break;
            }
            case 14: {
                if (obj.clazz == 3) {
                    if (obj.fplash < 2) {
                        obj.frame = 5;
                        obj.weapon_frame = 4;
                        break;
                    }
                    obj.setEye(1);
                    obj.weapon_frame = obj.fplash + 2;
                    if (obj.fplash < 6) {
                        obj.frame = 4;
                        break;
                    }
                    obj.Action = 0;
                    obj.frame = 0;
                    obj.f = 0;
                    obj.weapon_frame = 0;
                    break;
                }
                if (obj.fplash < 4) {
                    obj.frame = 4;
                    obj.weapon_frame = 4;
                    break;
                }
                obj.setEye(1);
                obj.weapon_frame = obj.fplash;
                if (obj.fplash < 8) {
                    obj.frame = 5;
                    break;
                }
                obj.Action = 0;
                obj.frame = 0;
                obj.f = 0;
                obj.weapon_frame = 0;
                break;
            }
            case 9: {
                if (obj.fplash < 30) {
                    obj.frame = 4;
                    if (obj.clazz == 3) {
                        obj.frame = 5;
                    }
                    obj.weapon_frame = 4;
                    break;
                }
                obj.Action = 0;
                obj.frame = 0;
                obj.f = 0;
                obj.weapon_frame = 0;
                break;
            }
            default: {
                if (obj.fplash < this.min) {
                    obj.frame = 4;
                    if (this.isSung) {
                        obj.frame = 5;
                    }
                    obj.weapon_frame = 4;
                    if (obj.weapon_frame <= 4) break;
                    obj.weapon_frame = 4;
                    break;
                }
                obj.setEye(1);
                obj.weapon_frame = obj.fplash - (this.min - 4);
                if (obj.fplash < this.min + 4) {
                    obj.frame = 5;
                    if (!this.isSung) break;
                    obj.frame = 4;
                    break;
                }
                obj.Action = 0;
                obj.frame = 0;
                obj.f = 0;
                obj.weapon_frame = 0;
            }
        }
        ++obj.fplash;
    }
}

