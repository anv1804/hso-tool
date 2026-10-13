package com.silverknight;

import GameScreen.GameScreen;
import GameScreen.PaintInfoGameScreen;
import GameObjects.MainObject;
import GameObjects.MainMonster;
import GameObjects.Player;
import GameObjects.Object_Effect_Skill;
import Main.GameCanvas;
import Thread_More.LoadMap;
import CLib.mVector;
import Skill.ListSkill;
import Skill.Skill;
import Skill.MainListSkill;
import Skill.HotKey;
import netcommand.global.GlobalService;
import CLib.mGraphics;
import CLib.mFont;
import GameScreen.MainScreen;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileInputStream;
import java.io.ByteArrayOutputStream;

public class HsoBridge {
    static {
        try {
            org.microemu.log.Logger.removeAllAppenders();
        } catch (Throwable ignored) {}
        disableAwtIme();
    }
    private static long lastSyncTime = 0;
    private static final String STATE_FILE = "client_state.json";
    private static final String CMD_FILE = "client_command.json";
    private static final String CMD_DIR = "client_commands";

    public static boolean isAutoPickup = false;
    public static boolean lootWhite = true;
    public static boolean lootBlue = true;
    public static boolean lootYellow = true;
    public static boolean lootPurple = true;
    public static boolean lootOrange = true;
    public static boolean lootGreen = true;
    public static boolean lootHp = true;
    public static boolean lootMp = true;
    public static boolean lootGold = true;
    public static boolean lootMaterial = true;
    public static boolean lootQuest = true;
    public static boolean isAutoAttack = false;
    public static boolean pendingMinArea = false;
    public static boolean isSilentRequestArea = false;
    public static volatile int pendingTargetArea = -1;
    private static volatile long pendingAreaTime = 0;
    public static float currentSpeedMultiplier = 1.0f;
    private static int lastMapId = -1;
    private static int huntTargetId = -1;
    private static volatile boolean farmConfigured = false;
    private static volatile int farmMapId = -1;
    private static volatile int farmArea = -1;
    private static volatile int[] farmTargetIds = new int[0];
    private static long lastFarmAttemptTime = 0;
    private static volatile int farmPacketsSent = 0;
    private static volatile int farmDamageResults = 0;
    private static final mVector farmDamagedIds = new mVector("Auto Fam confirmed targets");
    private static int farmTargetCursor = 0;
    public static volatile boolean botCaptchaDetected = false;
    public static volatile int otherPlayersInZone = 0;
    private static final int FARM_BATCH_SIZE = 7;
    private static final long FARM_BURST_INTERVAL_MULTI = 80L;
    private static final long FARM_BURST_INTERVAL_SINGLE = 300L;
    private static final int MONSTER_SCAN_INITIAL_LIMIT = 512;
    private static final int MONSTER_SCAN_MAX_LIMIT = 2048;
    private static int monsterScanLimit = MONSTER_SCAN_INITIAL_LIMIT;
    private static int monsterScanMapId = -1;
    private static int monsterScanArea = -1;
    private static int monsterScanNextId = -1;
    private static boolean monsterScanDone = false;
    private static long lastMonsterScanRequest = 0;
    private static int monsterRefreshIndex = 0;
    private static long lastMonsterRefreshTime = 0;
    private static final mVector mapMonsterRecords = new mVector("Auto Fam map monsters");

    private static class MonsterRecord {
        int id;
        String name;
        int hp;
        int maxHp;
        int x;
        int y;
        int respawnSeconds = -1;
        long respawnIntervalMs = 5000L;
        long deathTime = 0L;
        boolean preemptiveFired = false;
    }
    public static int pinnedMonsterId = -1;
    private static int pinnedMapId = -1;
    private static int pinnedArea = -1;
    public static int pinnedPlayerId = -1;
    private static int pinnedPlayerMapId = -1;
    private static int pinnedPlayerArea = -1;
    public static int pinnedPlayerX = -1;
    public static int pinnedPlayerY = -1;
    public static long lastRemoteMoveTime = 0;
    public static short lastRemoteTargetX = -1;
    public static short lastRemoteTargetY = -1;
    public static int remoteOrigX = -1;
    public static int remoteOrigY = -1;
    public static volatile boolean isStrikeActive = false;
    private static long lastPathTime = 0;
    private static int unreachableMonsterId = -1;
    private static long unreachableTime = 0;
    private static long lastPickupTime = 0;
    private static long lastCaptchaNoticeTime = 0;
    private static long lastOtherPlayerNoticeTime = 0;

    public static void logDebug(String msg) {
        try {
            FileOutputStream fos = new FileOutputStream("hso_debug.log", true);
            fos.write((System.currentTimeMillis() + " " + msg + "\n").getBytes("UTF-8"));
            fos.close();
        } catch (Exception e) {}
    }

    public static void update() {
        long now = System.currentTimeMillis();

        checkCommand();
        ensureMonsterScanContext();
        advanceMonsterScan(now);
        advanceMonsterRefresh(now);
        int otherCount = 0;
        String otherName = null;
        boolean captchaFound = false;
        if (GameScreen.Vecplayers != null) {
            for (int i = 0; i < GameScreen.Vecplayers.size(); ++i) {
                MainObject localObj = (MainObject)GameScreen.Vecplayers.elementAt(i);
                if (localObj == null) continue;
                if (localObj.typeObject == 1) {
                    if (localObj.isDie || localObj.isRemove || localObj.Action == 4 || localObj.hp <= 0) {
                        markMonsterDead(localObj.ID);
                    } else {
                        rememberLocalMonster(localObj);
                    }
                    if (localObj.typeBoss == 2 && !localObj.isDie && !localObj.isRemove) {
                        captchaFound = true;
                    }
                } else if (localObj.typeObject == 0 && localObj != GameScreen.player) {
                    otherCount++;
                    if (otherName == null) otherName = localObj.name;
                }
            }
        }
        otherPlayersInZone = otherCount;
        botCaptchaDetected = captchaFound;

        if (captchaFound && now - lastCaptchaNoticeTime > 6000L) {
            lastCaptchaNoticeTime = now;
            GameCanvas.addInfoCharServer("CANH BAO: BOT CAPTCHA XUAT HIEN!");
        }

        if (farmConfigured && otherCount > 0) {
            farmConfigured = false;
            farmTargetIds = new int[0];
            logDebug("CANH BAO AN TOAN: Phat hien " + otherName + " vao khu! Da tat Auto Fam 1-hit!");
            if (now - lastOtherPlayerNoticeTime > 4000L) {
                lastOtherPlayerNoticeTime = now;
                GameCanvas.addInfoCharServer("CANH BAO: Phat hien " + (otherName != null ? otherName : "nguoi choi") + " vao khu! Da tat Auto Fam!");
            }
        }
        if (GameScreen.player == null) {
            clearPinnedMonster();
            clearPinnedPlayer();
        }
        restorePinnedFocus();
        
        // Thực hiện auto nhặt đồ rơi liên tục trên toàn map nếu bật (tuân thủ bộ lọc đồ)
        if (isAutoPickup && GameScreen.player != null && GameScreen.Vecplayers != null) {
            if (now - lastPickupTime >= 200) {
                lastPickupTime = now;
                Object[] arr = null;
                try {
                    synchronized (GameScreen.Vecplayers) {
                        int sz = GameScreen.Vecplayers.size();
                        arr = new Object[sz];
                        for (int k = 0; k < sz; ++k) arr[k] = GameScreen.Vecplayers.elementAt(k);
                    }
                } catch (Exception ignored) {}
                if (arr != null) {
                    for (int i = 0; i < arr.length; ++i) {
                        MainObject obj = (MainObject)arr[i];
                        if (shouldPickupItem(obj)) {
                            GlobalService.gI().Get_Item_Map((short)obj.ID, obj.typeObject);
                            obj.isSend = false;
                        }
                    }
                }
            }
        }

        // Thực hiện thông minh: Auto Tìm Quái & Đánh (Auto-Hunt) hoặc Tấn công Người chơi được ghim
        if (isAutoAttack && GameCanvas.currentScreen == GameCanvas.game && GameScreen.player != null) {
            // Ngăn hệ thống auto gốc kéo giật nhân vật về vị trí cũ (leashing)
            Player.isAutoFire = (byte)-1;
            Player.isCurAutoFire = false;
            Player.xBeginAutoFire = GameScreen.player.x;
            Player.yBeginAutofire = GameScreen.player.y;

            if (pinnedPlayerId != -1) {
                // Tấn công nhân vật được ghim từ xa liên tục nếu bật Auto Attack
                MainObject targetPl = findPinnedPlayer();
                if (targetPl != null && !targetPl.isDie && !targetPl.isRemove && targetPl.Action != 4) {
                    if (now - lastFarmAttemptTime >= 350L && !isStrikeActive) {
                        attackPlayerRemote(pinnedPlayerId);
                        lastFarmAttemptTime = now;
                    }
                }
            } else if (farmConfigured && farmTargetIds != null && farmTargetIds.length > 0) {
                if (GameCanvas.loadmap != null && farmMapId == GameCanvas.loadmap.idMap) {
                    fireFarmBurst(now);
                }
            } else if (GameScreen.player.Action != 4 && GameScreen.player.hp > 0) {
                MainObject target = pinnedMonsterId >= 0 ? findPinnedMonster() : GameScreen.ObjFocus;
                boolean validTarget = (target != null 
                    && target.typeObject == 1 
                    && target.hp > 0 
                    && target.Action != 4 
                    && !target.isDie 
                    && !target.isRemove 
                    && !target.isLuaThieng());

                // Nếu chưa có quái hoặc quái hiện tại đã chết -> tìm quái sống gần nhất
                if (!validTarget && pinnedMonsterId < 0) {
                    target = findNearestMonster(now);
                    if (target != null) {
                        GameScreen.ObjFocus = target;
                        PaintInfoGameScreen.isPaintInfoFocus = true;
                        huntTargetId = target.ID;
                        validTarget = true;
                    } else {
                        // Không có quái nào trên map -> dừng di chuyển
                        if (GameScreen.player.posTransRoad != null) {
                            GameScreen.player.posTransRoad = null;
                            GameScreen.player.toX = GameScreen.player.x;
                            GameScreen.player.toY = GameScreen.player.y;
                            GameScreen.player.resetMove();
                        }
                    }
                }

                if (pinnedMonsterId >= 0) {
                    // Mục tiêu ghim được đánh bằng ID, không đổi sang quái gần hơn
                    target = findPinnedMonster();
                    MonsterRecord rec = findMonsterRecord(pinnedMonsterId);
                    boolean isAlive = (target != null && target.hp > 0 && !target.isDie && !target.isRemove && target.Action != 4)
                                   || (rec != null && rec.hp > 0);
                    if (isAlive) {
                        if (target != null) {
                            GameScreen.ObjFocus = target;
                            PaintInfoGameScreen.isPaintInfoFocus = true;
                        }
                        if (now - lastFarmAttemptTime >= 350L && !isStrikeActive) {
                            // Dịch chuyển tọa độ sát quái trên server nếu ở ngoài tầm cận chiến (> 25px)
                            // giúp server luôn chấp nhận đòn đánh kể cả quái ở cự ly gần 30-50px hay xa
                            if (target != null && MainObject.getDistance(GameScreen.player.x, GameScreen.player.y, target.x, target.y) > 25) {
                                lastRemoteMoveTime = now;
                                lastRemoteTargetX = (short)target.x;
                                lastRemoteTargetY = (short)target.y;
                                GlobalService.gI().player_move((short)target.x, (short)target.y);
                            }
                            mVector targets = new mVector("Auto attack pinned monster");
                            targets.addElement(new Object_Effect_Skill((short)pinnedMonsterId, (byte)1));
                            GlobalService.gI().fire_monster(targets, (byte)0);
                            lastFarmAttemptTime = now;
                        }
                    }
                } else if (target != null && validTarget) {
                    // Chọn vị trí đứng ngang cạnh quái (bên trái hoặc bên phải quái)
                    int leftStandX = target.x - 24;
                    int rightStandX = target.x + 24;
                    int standY = target.y;

                    // Ưu tiên phía hiện tại nhân vật đang đứng, đồng thời kiểm tra ô có thể đi
                    int standX;
                    if (GameScreen.player.x < target.x) {
                        standX = isWalkable(leftStandX, standY) ? leftStandX : (isWalkable(rightStandX, standY) ? rightStandX : leftStandX);
                    } else {
                        standX = isWalkable(rightStandX, standY) ? rightStandX : (isWalkable(leftStandX, standY) ? leftStandX : rightStandX);
                    }

                    int dx = GameScreen.player.x - target.x;
                    int dy = GameScreen.player.y - target.y;
                    int absDx = (dx < 0) ? -dx : dx;
                    int absDy = (dy < 0) ? -dy : dy;

                    // Kiểm tra vị trí đứng ngang: Y xấp xỉ ngang bằng quái (<= 16px) và X trong cự ly đánh ngang (8px đến 42px)
                    boolean inHorizontalAttackRange = (absDy <= 16 && absDx >= 8 && absDx <= 42);

                    if (inHorizontalAttackRange) {
                        // Đã vào vị trí đứng ngang hoàn hảo -> Dừng bước
                        if (GameScreen.player.posTransRoad != null) {
                            GameScreen.player.posTransRoad = null;
                            GameScreen.player.toX = GameScreen.player.x;
                            GameScreen.player.toY = GameScreen.player.y;
                            GameScreen.player.resetMove();
                        }

                        // Quay ngang: Đứng bên trái quái thì quay PHẢI (3), đứng bên phải quái thì quay TRÁI (2)
                        GameScreen.player.Direction = (GameScreen.player.x < target.x) ? (byte)3 : (byte)2;

                        // Kích hoạt đánh bằng cơ chế native của game và phím đánh
                        if (GameScreen.player.Action != 2 && GameScreen.player.Action != 4) {
                            GameScreen.player.setAutoFire();
                        }
                        GameCanvas.keyMyPressed[25] = true;
                        GameCanvas.keyMyPressed[5] = true;
                    } else {
                        // Chưa ở vị trí đứng ngang -> Tìm đường di chuyển tới vị trí đứng ngang
                        boolean needNewPath = (GameScreen.player.posTransRoad == null 
                            || huntTargetId != target.ID 
                            || now - lastPathTime > 1500);

                        if (needNewPath) {
                            huntTargetId = target.ID;
                            lastPathTime = now;
                            short[] road = findPathTo(standX, standY);
                            if (road == null) {
                                road = findPathTo(target.x, target.y);
                            }
                            if (road != null && road.length > 0) {
                                GameScreen.player.toX = GameScreen.player.x;
                                GameScreen.player.toY = GameScreen.player.y;
                                GameScreen.player.xStopMove = 0;
                                GameScreen.player.yStopMove = 0;
                                GameScreen.player.countAutoMove = 0;
                                GameScreen.player.resetMove();
                                GameScreen.player.posTransRoad = road;
                            } else {
                                unreachableMonsterId = target.ID;
                                unreachableTime = now;
                                GameScreen.ObjFocus = null;
                            }
                        }
                    }
                } else if (isAutoPickup) {
                    // Nếu không có quái sống, tìm vật phẩm gần nhất trên map để tự động chạy tới nhặt sạch map
                    MainObject nearestItem = findNearestItem();
                    if (nearestItem != null && GameScreen.player.posTransRoad == null) {
                        short[] road = findPathTo(nearestItem.x, nearestItem.y);
                        if (road != null && road.length > 0) {
                            GameScreen.player.toX = GameScreen.player.x;
                            GameScreen.player.toY = GameScreen.player.y;
                            GameScreen.player.xStopMove = 0;
                            GameScreen.player.yStopMove = 0;
                            GameScreen.player.countAutoMove = 0;
                            GameScreen.player.resetMove();
                            GameScreen.player.posTransRoad = road;
                        }
                    }
                }
            }
        } else if (!isAutoAttack) {
            Player.isAutoFire = (byte)-1;
            Player.isCurAutoFire = false;
            GameCanvas.keyMyPressed[25] = false;
            GameCanvas.keyMyPressed[5] = false;
            if (GameScreen.player != null) {
                GameScreen.player.KillFire = -1;
                GameScreen.player.IDAttack = -1;
            }
        }

        // Bỏ trạng thái khu của map cũ trước khi xử lý yêu cầu chọn khu.
        if (GameCanvas.currentScreen == GameCanvas.game && GameCanvas.loadmap != null
                && GameCanvas.loadmap.idMap != lastMapId) {
            lastMapId = GameCanvas.loadmap.idMap;
            clearPinnedMonster();
            clearPinnedPlayer();
            pendingTargetArea = -1;
            LoadMap.mStatusArea = null;
            isSilentRequestArea = true;
            GlobalService.gI().Request_Area();
        }

        if (pendingTargetArea >= 0 && now - pendingAreaTime > 5000L) {
            logDebug("quickChangeArea: area list request timed out");
            pendingTargetArea = -1;
            isSilentRequestArea = false;
        }

        // Tự động chuyển khu ít người nhất khi có dữ liệu area
        if (pendingMinArea && LoadMap.mStatusArea != null && LoadMap.mStatusArea.length > 0) {
            int bestArea = -1;
            int bestStatus = Integer.MAX_VALUE;
            for (int i = 0; i < LoadMap.mStatusArea.length; ++i) {
                if (i != LoadMap.Area && LoadMap.mStatusArea[i] < bestStatus && LoadMap.mStatusArea[i] < 2) {
                    bestStatus = LoadMap.mStatusArea[i];
                    bestArea = i;
                }
            }
            if (bestArea >= 0) {
                quickChangeArea(bestArea);
            }
            pendingMinArea = false;
        }

        // Fallback an toàn hoàn trả vị trí thực tế của nhân vật về server sau khi đánh từ xa (1200ms)
        if (lastRemoteMoveTime > 0 && now - lastRemoteMoveTime >= 1200) {
            if (GameScreen.player != null && remoteOrigX > 0) {
                GameScreen.player.x = remoteOrigX;
                GameScreen.player.y = remoteOrigY;
                GameScreen.player.toX = remoteOrigX;
                GameScreen.player.toY = remoteOrigY;
                GameScreen.player.xStand = (short)remoteOrigX;
                GameScreen.player.yStand = (short)remoteOrigY;
                GameScreen.player.timeSendmove = now;
                GlobalService.gI().player_move((short)remoteOrigX, (short)remoteOrigY);
                logDebug("FALLBACK RESTORED position to orig=(" + remoteOrigX + "," + remoteOrigY + ")");
            }
            lastRemoteMoveTime = 0;
            remoteOrigX = -1;
            remoteOrigY = -1;
        }

        // Duy trì tốc độ di chuyển đã cấu hình
        if (GameScreen.player != null && currentSpeedMultiplier >= 1.0f) {
            GameScreen.player.vMax = (int)Math.round(6 * currentSpeedMultiplier);
        }

        if (now - lastSyncTime < 350) {
            return;
        }
        lastSyncTime = now;

        writeState();
    }

    private static MainObject findNearestMonster(long now) {
        if (GameScreen.Vecplayers == null || GameScreen.player == null) return null;
        int minDistance = Integer.MAX_VALUE;
        MainObject nearest = null;
        for (int i = 0; i < GameScreen.Vecplayers.size(); ++i) {
            MainObject obj = (MainObject)GameScreen.Vecplayers.elementAt(i);
            if (obj != null && obj.typeObject == 1 && obj.hp > 0 && obj.Action != 4 && !obj.isDie && !obj.isRemove && !obj.isLuaThieng()) {
                if (obj.ID == unreachableMonsterId && now - unreachableTime < 4000) {
                    continue;
                }
                int d = MainObject.getDistance(GameScreen.player.x, GameScreen.player.y, obj.x, obj.y);
                if (d < minDistance) {
                    minDistance = d;
                    nearest = obj;
                }
            }
        }
        return nearest;
    }

    private static synchronized void ensureMonsterScanContext() {
        if (GameCanvas.loadmap == null) return;
        int mapId = GameCanvas.loadmap.idMap;
        int area = LoadMap.Area;
        if (monsterScanMapId == mapId && monsterScanArea == area) return;
        monsterScanMapId = mapId;
        monsterScanArea = area;
        monsterScanNextId = 0;
        monsterScanDone = false;
        monsterScanLimit = MONSTER_SCAN_INITIAL_LIMIT;
        lastMonsterScanRequest = 0;
        monsterRefreshIndex = 0;
        lastMonsterRefreshTime = 0;
        mapMonsterRecords.removeAllElements();
        logDebug("Auto Fam scan started map=" + monsterScanMapId + " area=" + monsterScanArea);
    }

    private static synchronized MonsterRecord findMonsterRecord(int id) {
        for (int i = 0; i < mapMonsterRecords.size(); ++i) {
            MonsterRecord record = (MonsterRecord)mapMonsterRecords.elementAt(i);
            if (record.id == id) return record;
        }
        return null;
    }

    private static synchronized void rememberLocalMonster(MainObject obj) {
        if (obj == null || obj.typeObject != 1 || obj.isRemove) return;
        MonsterRecord record = findMonsterRecord(obj.ID);
        if (record == null) {
            record = new MonsterRecord();
            record.id = obj.ID;
            mapMonsterRecords.addElement(record);
        }
        record.name = obj.name;
        record.maxHp = obj.maxHp;
        record.x = obj.x;
        record.y = obj.y;
        if (obj instanceof MainMonster) {
            int t = ((MainMonster)obj).timeReveice;
            if (t > 0) {
                record.respawnSeconds = t;
                record.respawnIntervalMs = (long)t * 1000L;
            }
        }
        if (obj.isDie || obj.Action == 4 || obj.hp <= 0) {
            record.hp = 0;
            if (record.deathTime <= 0L) {
                record.deathTime = System.currentTimeMillis();
                record.preemptiveFired = false;
            }
        } else {
            if (record.hp <= 0 && record.deathTime > 0L) {
                long measured = System.currentTimeMillis() - record.deathTime;
                if (measured > 1500L && measured < 60000L) {
                    record.respawnIntervalMs = measured;
                }
                record.deathTime = 0L;
                record.preemptiveFired = false;
            }
            record.hp = obj.hp;
        }
    }

    public static synchronized void updateMonsterPosition(int id, int x, int y) {
        MonsterRecord record = findMonsterRecord(id);
        if (record != null) {
            record.x = x;
            record.y = y;
        }
    }

    public static void stopAllAutoAttack() {
        isAutoAttack = false;
        huntTargetId = -1;
        Player.isAutoFire = (byte)-1;
        Player.isCurAutoFire = false;
        GameCanvas.keyMyPressed[25] = false;
        GameCanvas.keyMyPressed[5] = false;
        if (GameScreen.player != null) {
            GameScreen.player.posTransRoad = null;
            GameScreen.player.toX = GameScreen.player.x;
            GameScreen.player.toY = GameScreen.player.y;
            GameScreen.player.resetMove();
            GameScreen.player.KillFire = -1;
            GameScreen.player.IDAttack = -1;
            GameScreen.player.vecObjFocusSkill = null;
        }
    }

    public static synchronized void rememberMonsterInfo(int id, int lv, int x, int y, int hp, int maxHp) {
        rememberMonsterInfo(id, lv, x, y, hp, maxHp, -1);
    }

    public static synchronized void rememberMonsterInfo(int id, int lv, int x, int y, int hp, int maxHp, int timeReveice) {
        ensureMonsterScanContext();
        if (monsterScanMapId < 0) return;
        MonsterRecord record = findMonsterRecord(id);
        if (record == null) {
            record = new MonsterRecord();
            record.id = id;
            mapMonsterRecords.addElement(record);
        }
        MainObject local = GameScreen.Vecplayers == null ? null : MainObject.get_Object(id, (byte)1);
        String name = local != null ? local.name : MainMonster.getCatalogyNameByStats(lv, maxHp);
        if (name == null && GameScreen.Vecplayers != null) {
            for (int i = 0; i < GameScreen.Vecplayers.size(); ++i) {
                MainObject candidate = (MainObject)GameScreen.Vecplayers.elementAt(i);
                if (candidate == null || candidate.typeObject != 1
                        || candidate.Lv != lv || candidate.maxHp != maxHp) continue;
                if (name != null && !name.equals(candidate.name)) {
                    name = null;
                    break;
                }
                name = candidate.name;
            }
        }
        if (name != null && name.length() > 0) record.name = name;
        if (record.name == null) record.name = "Chưa rõ loại";
        record.maxHp = maxHp;
        record.x = x;
        record.y = y;

        if (timeReveice > 0) {
            record.respawnSeconds = timeReveice;
            record.respawnIntervalMs = (long)timeReveice * 1000L;
        }

        if (record.hp <= 0 && hp > 0) {
            if (record.deathTime > 0L) {
                long measured = System.currentTimeMillis() - record.deathTime;
                if (measured > 1500L && measured < 60000L) {
                    record.respawnIntervalMs = measured;
                }
            }
            record.deathTime = 0L;
            record.preemptiveFired = false;
        } else if (hp <= 0) {
            if (record.deathTime <= 0L) {
                record.deathTime = System.currentTimeMillis();
                record.preemptiveFired = false;
            }
        }
        record.hp = hp;
    }

    public static synchronized void markMonsterDead(int id) {
        MonsterRecord record = findMonsterRecord(id);
        if (record != null) {
            record.hp = 0;
            if (record.deathTime <= 0L) {
                record.deathTime = System.currentTimeMillis();
                record.preemptiveFired = false;
            }
        }
    }

    public static synchronized void updateMonsterHealth(int id, int hp) {
        MonsterRecord record = findMonsterRecord(id);
        if (record != null) {
            record.hp = Math.max(0, hp);
            if (record.hp == 0) {
                if (record.deathTime <= 0L) {
                    record.deathTime = System.currentTimeMillis();
                    record.preemptiveFired = false;
                }
            }
        }
    }

    public static synchronized void noteFarmDamageResult(int attackerId, int monsterId, int damage) {
        if (!isFarmAttackActive() || GameScreen.player == null
                || attackerId != GameScreen.player.ID || damage <= 0) return;
        ++farmDamageResults;
        Integer id = Integer.valueOf(monsterId);
        if (!farmDamagedIds.contains(id)) farmDamagedIds.addElement(id);
    }

    public static void noteFarmPacketSent() {
        if (isFarmAttackActive()) ++farmPacketsSent;
    }

    public static boolean isFarmAttackActive() {
        return isAutoAttack && farmConfigured && farmTargetIds != null && farmTargetIds.length > 0
                && GameCanvas.loadmap != null && farmMapId == GameCanvas.loadmap.idMap;
    }

    public static boolean isFarmConfigured() {
        return farmConfigured && farmTargetIds != null && farmTargetIds.length > 0
                && GameCanvas.loadmap != null && farmMapId == GameCanvas.loadmap.idMap;
    }

    public static boolean isConfiguredFarmTarget(int id) {
        if (farmTargetIds == null) return false;
        for (int i = 0; i < farmTargetIds.length; ++i) {
            if (farmTargetIds[i] == id) return true;
        }
        return false;
    }

    public static boolean isFarmTargetAllowed(int id) {
        return !isFarmAttackActive() || isConfiguredFarmTarget(id);
    }

    public static MainObject getLiveMonster(int id) {
        if (GameScreen.Vecplayers == null) return null;
        MainObject obj = MainObject.get_Object(id, (byte)1);
        if (obj != null && !obj.isRemove && !obj.isDie && obj.Action != 4 && obj.hp > 0) {
            return obj;
        }
        return null;
    }

    public static synchronized void onMonsterRespawned(int id, int x, int y) {
        MonsterRecord rec = findMonsterRecord(id);
        if (rec != null) {
            rec.hp = rec.maxHp > 0 ? rec.maxHp : 1;
            rec.x = x;
            rec.y = y;
            rec.deathTime = 0L;
            rec.preemptiveFired = false;
        }
    }

    public static boolean isMonsterAlive(int id) {
        if (GameScreen.Vecplayers != null) {
            MainObject obj = MainObject.get_Object(id, (byte)1);
            if (obj != null) {
                if (obj.isRemove || obj.isDie || obj.Action == 4 || obj.hp <= 0) {
                    return false;
                }
                MonsterRecord rec = findMonsterRecord(id);
                if (rec != null) {
                    rec.hp = obj.hp;
                    rec.x = obj.x;
                    rec.y = obj.y;
                    if (rec.deathTime > 0L) {
                        rec.deathTime = 0L;
                        rec.preemptiveFired = false;
                    }
                }
                return true;
            }
        }
        MonsterRecord rec = findMonsterRecord(id);
        return rec != null && rec.hp > 0;
    }

    public static synchronized void startMonsterScan() {
        ensureMonsterScanContext();
        if (monsterScanMapId < 0) return;
        monsterScanNextId = 0;
        monsterScanDone = false;
        lastMonsterScanRequest = 0;
        logDebug("Auto Fam scan start/restart map=" + monsterScanMapId + " area=" + monsterScanArea);
    }

    private static synchronized void advanceMonsterScan(long now) {
        if (GameCanvas.currentScreen != GameCanvas.game || GameCanvas.loadmap == null
                || GameCanvas.loadmap.idMap != monsterScanMapId || LoadMap.Area != monsterScanArea
                || monsterScanNextId < 0 || now - lastMonsterScanRequest < 25L) return;
        if (monsterScanNextId >= monsterScanLimit) {
            for (int i = 0; i < mapMonsterRecords.size(); ++i) {
                MonsterRecord record = (MonsterRecord)mapMonsterRecords.elementAt(i);
                if (record.id >= monsterScanLimit - 64 && monsterScanLimit < MONSTER_SCAN_MAX_LIMIT) {
                    monsterScanLimit = Math.min(MONSTER_SCAN_MAX_LIMIT, monsterScanLimit + 256);
                    return;
                }
            }
            monsterScanNextId = -1;
            monsterScanDone = true;
            logDebug("Auto Fam scan finished records=" + mapMonsterRecords.size());
            return;
        }
        int batch = 6;
        while (batch-- > 0 && monsterScanNextId < monsterScanLimit) {
            int id = monsterScanNextId++;
            if (findMonsterRecord(id) == null) {
                GlobalService.gI().monster_info((short)id);
            }
        }
        lastMonsterScanRequest = now;
    }

    private static synchronized void advanceMonsterRefresh(long now) {
        if (!monsterScanDone) return;
        if (GameCanvas.currentScreen != GameCanvas.game || GameCanvas.loadmap == null
                || GameCanvas.loadmap.idMap != monsterScanMapId || LoadMap.Area != monsterScanArea
                || mapMonsterRecords.size() == 0) return;

        // 1. Quét các quái đã chết: nếu đã đến hoặc quá thời gian hồi sinh thì khôi phục và truy vấn server ngay
        for (int i = 0; i < mapMonsterRecords.size(); ++i) {
            MonsterRecord rec = (MonsterRecord)mapMonsterRecords.elementAt(i);
            if (rec != null && rec.hp <= 0 && rec.deathTime > 0L) {
                long interval = rec.respawnIntervalMs > 0 ? rec.respawnIntervalMs : 5000L;
                if (now - rec.deathTime >= interval) {
                    if (rec.maxHp > 0) rec.hp = rec.maxHp;
                    rec.deathTime = 0L;
                    GlobalService.gI().monster_info((short)rec.id);
                }
            }
        }

        // 2. Định kỳ round-robin gửi truy vấn monster_info cho toàn bộ quái trên map để đồng bộ tọa độ và máu thời gian thực
        if (now - lastMonsterRefreshTime < 60L) return;
        lastMonsterRefreshTime = now;

        int size = mapMonsterRecords.size();
        if (size == 0) return;

        int batch = 4;
        while (batch-- > 0) {
            if (monsterRefreshIndex >= size) monsterRefreshIndex = 0;
            MonsterRecord rec = (MonsterRecord)mapMonsterRecords.elementAt(monsterRefreshIndex++);
            if (rec != null) {
                GlobalService.gI().monster_info((short)rec.id);
            }
        }
    }

    private static synchronized String mapMonstersJson() {
        if (GameScreen.Vecplayers != null) {
            Object[] array = null;
            try {
                synchronized (GameScreen.Vecplayers) {
                    int sz = GameScreen.Vecplayers.size();
                    array = new Object[sz];
                    for (int k = 0; k < sz; ++k) {
                        array[k] = GameScreen.Vecplayers.elementAt(k);
                    }
                }
            } catch (Exception ignored) {}
            if (array != null) {
                for (int i = 0; i < array.length; ++i) {
                    MainObject obj = (MainObject)array[i];
                    rememberLocalMonster(obj);
                }
            }
        }
        StringBuffer result = new StringBuffer("[");
        for (int i = 0; i < mapMonsterRecords.size(); ++i) {
            MonsterRecord record = (MonsterRecord)mapMonsterRecords.elementAt(i);
            if (i > 0) result.append(",");
            result.append("{\"id\":").append(record.id)
                  .append(",\"name\":\"").append(escape(record.name == null ? "Chưa rõ loại" : record.name))
                  .append("\",\"hp\":").append(record.hp)
                  .append(",\"maxHp\":").append(record.maxHp)
                  .append(",\"x\":").append(record.x)
                  .append(",\"y\":").append(record.y)
                  .append(",\"respawn\":").append(record.respawnSeconds)
                  .append(",\"respawn_ms\":").append(record.respawnIntervalMs)
                  .append(",\"death_time\":").append(record.deathTime)
                  .append("}");
        }
        return result.append("]").toString();
    }

    private static final long PREEMPTIVE_LEAD_MS = 500L;

    public static int getNormalAttackCooldownMs() {
        try {
            Player player = GameScreen.player;
            if (player != null && Player.mKillPlayer != null && Player.mKillPlayer.length > 0) {
                int skillId = Player.mKillPlayer[0];
                Skill skill = MainListSkill.getSkillFormId(skillId);
                if (skill != null && skill.mLvSkill != null) {
                    int lvIdx = 0;
                    if (Player.mCurentLvSkill != null && skillId < Player.mCurentLvSkill.length) {
                        lvIdx = Player.mCurentLvSkill[skillId] + (Player.mPlusLvSkill != null && skillId < Player.mPlusLvSkill.length ? Player.mPlusLvSkill[skillId] : 0) - 1;
                    }
                    if (lvIdx >= 0 && lvIdx < skill.mLvSkill.length) {
                        int d = skill.mLvSkill[lvIdx].getdelay();
                        if (d > 0) return d;
                    }
                }
            }
        } catch (Exception ignored) {}
        return 1000; // Mặc định chuẩn 1s (1000ms) theo kỹ năng đánh thường
    }

    private static void fireFarmBurst(long now) {
        if (!isAutoAttack || !farmConfigured || farmTargetIds == null || farmTargetIds.length == 0) return;
        Player player = GameScreen.player;
        if (player == null || player.hp <= 0 || player.Action == 4
                || player.currentQuest != null || player.isTanHinh) return;

        // Chu kỳ tấn công giữa các đòn đánh liên tiếp: Đồng bộ chuẩn xác theo thời gian hồi chiêu đánh thường (1s)
        // Trừ 50ms bù trừ độ trễ mạng TCP (latency) để gói tin tới server đúng lúc vừa hết hồi chiêu 1s
        long attackCooldown = Math.max(800L, (long)getNormalAttackCooldownMs() - 50L);
        if (now - lastFarmAttemptTime < attackCooldown) return;

        int[] targets = getLiveFarmTargets(player, now);
        if (targets == null || targets.length == 0) return;

        player.IDAttack = targets[0];

        // 1. Dồn gom toàn bộ quái trên map vào 1 danh sách để tấn công 1 hit cả map
        mVector allTargets = new mVector("Auto Farm All Map Targets");
        for (int i = 0; i < targets.length; ++i) {
            allTargets.addElement(new Object_Effect_Skill((short)targets[i], (byte)1));
        }

        // 2. Tìm chiêu tấn công sẵn sàng trên thanh phím tắt (nếu có chiêu đánh lan)
        int readySkillId = -1;
        int hotkeyIdx = -1;
        if (player.mhotkey != null && Player.levelTab < player.mhotkey.length) {
            HotKey[] keys = player.mhotkey[Player.levelTab];
            if (keys != null) {
                for (int k = 0; k < keys.length; ++k) {
                    if (keys[k] != null && keys[k].type == HotKey.SKILL) {
                        int sid = keys[k].id;
                        Skill s = MainListSkill.getSkillFormId(sid);
                        if (s != null && s.typeSkill == 0 && player.setDelaySkill(sid, -1)) {
                            readySkillId = sid;
                            hotkeyIdx = k;
                            break;
                        }
                    }
                }
            }
        }

        // 3. Phóng gói tin 1 hit chứa toàn bộ quái trên map (nếu có chiêu lan)
        if (readySkillId >= 0) {
            GlobalService.gI().fire_monster(allTargets, (byte)readySkillId);
            player.setAddDelaySkill(readySkillId, hotkeyIdx);
        } else {
            GlobalService.gI().fire_monster(allTargets, (byte)0);
        }

        // 4. ĐỒNG THỜI GỬI KÈM GÓI ĐÁNH CHO TỪNG QUÁI TRONG CÙNG 1 TICK (BURST 1 HIT)
        // Cơ chế gốc học từ Git: Chiêu 0 (đánh thường) trên server HSO chỉ xử lý đơn mục tiêu.
        // Khi gom nhiều quái vào 1 gói tin skill 0, server chỉ trừ máu quái đầu tiên và bỏ qua quái sau.
        // Vòng lặp bắn toàn bộ N gói tin single trong < 1ms giúp TCP socket đẩy toàn bộ gói trong cùng 1 frame,
        // server xử lý và trừ máu TOÀN BỘ quái cùng lúc, tiêu diệt 100% quái trong 1 hit duy nhất!
        for (int i = 0; i < targets.length; ++i) {
            mVector single = new mVector("Auto Farm Single Target");
            single.addElement(new Object_Effect_Skill((short)targets[i], (byte)1));
            GlobalService.gI().fire_monster(single, (byte)0);
        }

        lastFarmAttemptTime = now;
    }

    private static int[] getLiveFarmTargets(Player player, long now) {
        int[] idsToCheck = farmTargetIds;
        if (idsToCheck == null || idsToCheck.length == 0) return new int[0];

        int aliveCount = 0;
        for (int i = 0; i < idsToCheck.length; ++i) {
            if (isMonsterAlive(idsToCheck[i])) {
                ++aliveCount;
            }
        }

        // TÌNH HUỐNG 1: CÓ QUÁI ĐANG SỐNG (kể cả 1 con hay toàn map)
        // -> LẬP TỨC ĐÁNH NGAY TOÀN BỘ CÁC CON ĐANG SỐNG!
        // Quái cần 2-3 hit mới chết sẽ được dồn đòn liên tục theo đúng nhịp hồi chiêu đánh thường (mỗi 250ms),
        // tuyệt đối KHÔNG ĐƯỢC nhịn đánh hay chờ đợi bất kỳ con quái nào đã chết!
        if (aliveCount > 0) {
            int[] liveIds = new int[aliveCount];
            int idx = 0;
            for (int i = 0; i < idsToCheck.length; ++i) {
                if (isMonsterAlive(idsToCheck[i])) {
                    liveIds[idx++] = idsToCheck[i];
                }
            }
            return liveIds;
        }

        // TÌNH HUỐNG 2: TOÀN BỘ QUÁI ĐỀU ĐÃ CHẾT (aliveCount == 0)
        // -> Chờ đón đầu thời điểm quái hồi sinh sớm 500ms
        long earliestRespawnDueTime = 0L;
        for (int i = 0; i < idsToCheck.length; ++i) {
            MonsterRecord rec = findMonsterRecord(idsToCheck[i]);
            if (rec != null && rec.deathTime > 0L) {
                long interval = rec.respawnIntervalMs > 0 ? rec.respawnIntervalMs : 5000L;
                // Bỏ qua các boss / quái đặc biệt có thời gian hồi sinh cực dài (> 60s)
                if (interval > 60000L) continue;
                long due = rec.deathTime + interval;
                if (earliestRespawnDueTime == 0L || due < earliestRespawnDueTime) {
                    earliestRespawnDueTime = due;
                }
            }
        }

        long threshold = earliestRespawnDueTime > 0L ? earliestRespawnDueTime : (now + 5000L);
        if (now >= threshold - PREEMPTIVE_LEAD_MS) {
            return idsToCheck;
        }

        return new int[0];
    }

    private static long farmTargetScore(Player player, int id) {
        if (GameScreen.Vecplayers != null) {
            MainObject obj = MainObject.get_Object(id, (byte)1);
            if (obj != null && !obj.isRemove && !obj.isDie && obj.Action != 4 && obj.hp > 0) {
                long dx = obj.x - player.x;
                long dy = obj.y - player.y;
                return dx * dx + dy * dy;
            }
        }
        MonsterRecord record = findMonsterRecord(id);
        if (record == null) return Long.MAX_VALUE / 2 + id;
        long dx = record.x - player.x;
        long dy = record.y - player.y;
        return (record.hp <= 0 ? 1000000000L : 0L) + dx * dx + dy * dy;
    }

    private static void stopFarmAutoMovement() {
        Player player = GameScreen.player;
        if (player == null) return;
        Player.isAutoFire = (byte)-1;
        Player.isCurAutoFire = false;
        player.KillFire = -1;
        player.vecObjFocusSkill = null;
        player.posTransRoad = null;
        player.toX = player.x;
        player.toY = player.y;
        player.xStopMove = 0;
        player.yStopMove = 0;
        player.countAutoMove = 0;
        GameCanvas.keyMyPressed[25] = false;
        GameCanvas.keyMyPressed[5] = false;
    }

    private static void configureFarmTargets(String cmd) {
        String map = extractJsonValue(cmd, "map_id");
        String area = extractJsonValue(cmd, "area");
        String configured = extractJsonValue(cmd, "configured");
        String ids = extractJsonValue(cmd, "ids");
        if (map == null || area == null || configured == null || ids == null) return;
        farmMapId = Integer.parseInt(map);
        try {
            farmArea = Integer.parseInt(area);
        } catch (Exception e) {
            farmArea = -1;
        }
        ids = ids.replace("\"", "").trim();
        boolean isConf = Integer.parseInt(configured) != 0;
        if (ids.length() == 0 || !isConf) {
            farmConfigured = false;
            farmTargetIds = new int[0];
        } else {
            String[] parts = ids.split(";");
            int[] parsedIds = new int[parts.length];
            for (int i = 0; i < parts.length; ++i) parsedIds[i] = Integer.parseInt(parts[i]);
            farmTargetIds = parsedIds;
            farmConfigured = (farmTargetIds.length > 0);
        }
        farmPacketsSent = 0;
        farmDamageResults = 0;
        farmTargetCursor = 0;
        farmDamagedIds.removeAllElements();
        if (isFarmAttackActive()) stopFarmAutoMovement();
        logDebug("Auto Fam configured=" + farmConfigured + " targets=" + farmTargetIds.length + " map=" + farmMapId + " area=" + farmArea);
    }

    private static short[] findPathTo(int targetX, int targetY) {
        if (GameCanvas.game == null || GameScreen.player == null || LoadMap.wTile <= 0) return null;
        int tx = targetX / LoadMap.wTile;
        int ty = targetY / LoadMap.wTile;
        int px = GameScreen.player.x / LoadMap.wTile;
        int py = GameScreen.player.y / LoadMap.wTile;

        // Thử đường đi trực tiếp
        short[] road = GameCanvas.game.updateFindRoad(tx, ty, px, py, 120);
        if (road != null && road.length > 0 && road.length <= 120) return road;

        // Nếu ô quái đứng không thể đi (hoặc mép vật cản), thử 8 ô lân cận
        int[][] offsets = new int[][]{{0, 1}, {0, -1}, {1, 0}, {-1, 0}, {1, 1}, {-1, -1}, {1, -1}, {-1, 1}};
        for (int i = 0; i < offsets.length; ++i) {
            road = GameCanvas.game.updateFindRoad(tx + offsets[i][0], ty + offsets[i][1], px, py, 120);
            if (road != null && road.length > 0 && road.length <= 120) return road;
        }
        return null;
    }

    private static boolean isWalkable(int x, int y) {
        if (GameCanvas.loadmap == null) return true;
        int t = GameCanvas.loadmap.getTile(x, y);
        return t != 1 && t != -1;
    }

    public static boolean shouldPickupItem(MainObject obj) {
        if (obj == null || obj.isRemove) return false;
        if (obj.typeObject == 3) {
            switch (obj.colorName) {
                case 0: return lootWhite;
                case 1: return lootBlue;
                case 2: return lootYellow;
                case 3: return lootPurple;
                case 4: return lootOrange;
                case 5: return lootGreen;
                default: return lootWhite;
            }
        } else if (obj.typeObject == 4) {
            String n = (obj.name != null) ? obj.name.toLowerCase() : "";
            if (n.indexOf("vàng") != -1 || n.indexOf("vang") != -1 || n.indexOf("xu") != -1 || n.indexOf("coin") != -1 || obj.colorName == 2) {
                return lootGold;
            }
            if (n.indexOf("mana") != -1 || n.indexOf("mp") != -1 || obj.colorName == 1) {
                return lootMp;
            }
            if (n.indexOf("máu") != -1 || n.indexOf("mau") != -1 || n.indexOf("hp") != -1 || obj.colorName == 0) {
                return lootHp;
            }
            return lootHp || lootMp || lootGold;
        } else if (obj.typeObject == 5) {
            return lootQuest;
        } else if (obj.typeObject == 7) {
            return lootMaterial;
        }
        return false;
    }

    private static MainObject findNearestItem() {
        if (GameScreen.Vecplayers == null || GameScreen.player == null) return null;
        int minDistance = Integer.MAX_VALUE;
        MainObject nearest = null;
        for (int i = 0; i < GameScreen.Vecplayers.size(); ++i) {
            MainObject obj = (MainObject)GameScreen.Vecplayers.elementAt(i);
            if (shouldPickupItem(obj)) {
                int d = MainObject.getDistance(GameScreen.player.x, GameScreen.player.y, obj.x, obj.y);
                if (d < minDistance) {
                    minDistance = d;
                    nearest = obj;
                }
            }
        }
        return nearest;
    }

    public static void onAreaChanged() {
        clearPinnedMonster();
        huntTargetId = -1;
        unreachableMonsterId = -1;
        GameScreen.ObjFocus = null;
        if (GameScreen.player != null) {
            GameScreen.player.xStand = GameScreen.player.x;
            GameScreen.player.yStand = GameScreen.player.y;
            GameScreen.player.toX = GameScreen.player.x;
            GameScreen.player.toY = GameScreen.player.y;
            GameScreen.player.xStopMove = 0;
            GameScreen.player.yStopMove = 0;
            GameScreen.player.countAutoMove = 0;
            GameScreen.player.timeSendmove = System.currentTimeMillis();
            GameScreen.player.posTransRoad = null;
            GameScreen.player.resetMove();
            GameScreen.player.resetAction();
        }
    }

    public static MainObject findPinnedMonster() {
        if (pinnedMonsterId < 0 || GameScreen.Vecplayers == null || GameCanvas.loadmap == null
                || pinnedMapId != GameCanvas.loadmap.idMap || pinnedArea != LoadMap.Area) {
            return null;
        }
        for (int i = 0; i < GameScreen.Vecplayers.size(); ++i) {
            MainObject obj = (MainObject)GameScreen.Vecplayers.elementAt(i);
            if (obj != null && obj.typeObject == 1 && obj.ID == pinnedMonsterId
                    && !obj.isRemove && !obj.isDie && obj.hp > 0 && obj.Action != 4) {
                return obj;
            }
        }
        return null;
    }

    public static void clearPinnedMonster() {
        pinnedMonsterId = -1;
        pinnedMapId = -1;
        pinnedArea = -1;
    }

    public static void unpinMonster() {
        int oldId = pinnedMonsterId;
        clearPinnedMonster();
        if (oldId >= 0 && GameScreen.ObjFocus != null
                && GameScreen.ObjFocus.typeObject == 1 && GameScreen.ObjFocus.ID == oldId) {
            GameScreen.ObjFocus = null;
            PaintInfoGameScreen.isPaintInfoFocus = false;
        }
    }

    public static boolean restorePinnedFocus() {
        if (pinnedPlayerId != -1) {
            MainObject target = findPinnedPlayer();
            if (target != null) {
                GameScreen.ObjFocus = target;
                PaintInfoGameScreen.isPaintInfoFocus = true;
                if (GameScreen.player != null) GameScreen.player.IDAttack = target.ID;
                return true;
            }
            return false;
        }
        if (pinnedMonsterId != -1) {
            MainObject target = findPinnedMonster();
            if (target != null) {
                GameScreen.ObjFocus = target;
                PaintInfoGameScreen.isPaintInfoFocus = true;
                if (GameScreen.player != null) GameScreen.player.IDAttack = target.ID;
                return true;
            }
            return false;
        }
        return false;
    }

    public static void pinMonster(int id, int mapId, int area) {
        if (GameCanvas.currentScreen != GameCanvas.game || GameCanvas.loadmap == null
                || GameCanvas.loadmap.idMap != mapId || LoadMap.Area != area) {
            logDebug("pinMonster: stale map or area for ID " + id);
            return;
        }
        MonsterRecord rec = findMonsterRecord(id);
        if (rec != null) {
            clearPinnedPlayer();
            pinnedMonsterId = id;
            pinnedMapId = mapId;
            pinnedArea = area;
            restorePinnedFocus();
            logDebug("Pinned monster ID " + id + " (" + rec.name + ")");
            return;
        }
        if (GameScreen.Vecplayers != null) {
            for (int i = 0; i < GameScreen.Vecplayers.size(); ++i) {
                MainObject obj = (MainObject)GameScreen.Vecplayers.elementAt(i);
                if (obj != null && obj.typeObject == 1 && obj.ID == id
                        && !obj.isRemove && !obj.isDie && obj.hp > 0 && obj.Action != 4) {
                    clearPinnedPlayer();
                    pinnedMonsterId = id;
                    pinnedMapId = mapId;
                    pinnedArea = area;
                    restorePinnedFocus();
                    logDebug("Pinned monster ID " + id);
                    return;
                }
            }
        }
        logDebug("pinMonster: monster ID " + id + " is unavailable");
    }

    public static MainObject findPinnedPlayer() {
        if (pinnedPlayerId == -1 || GameScreen.Vecplayers == null) {
            return null;
        }
        for (int i = 0; i < GameScreen.Vecplayers.size(); ++i) {
            MainObject obj = (MainObject)GameScreen.Vecplayers.elementAt(i);
            if (obj != null && obj.typeObject == 0 && obj.ID == pinnedPlayerId
                    && !obj.isRemove && !obj.isDie && obj.Action != 4) {
                pinnedPlayerX = obj.x;
                pinnedPlayerY = obj.y;
                return obj;
            }
        }
        return null;
    }

    public static void clearPinnedPlayer() {
        pinnedPlayerId = -1;
        pinnedPlayerMapId = -1;
        pinnedPlayerArea = -1;
        pinnedPlayerX = -1;
        pinnedPlayerY = -1;
    }

    public static void unpinPlayer() {
        int oldId = pinnedPlayerId;
        clearPinnedPlayer();
        if (oldId != -1 && GameScreen.ObjFocus != null
                && GameScreen.ObjFocus.typeObject == 0 && GameScreen.ObjFocus.ID == oldId) {
            GameScreen.ObjFocus = null;
            PaintInfoGameScreen.isPaintInfoFocus = false;
        }
    }

    public static void pinPlayer(int id, int mapId, int area) {
        clearPinnedMonster();
        pinnedPlayerId = id;
        pinnedPlayerMapId = GameCanvas.loadmap != null ? GameCanvas.loadmap.idMap : mapId;
        pinnedPlayerArea = LoadMap.Area;
        MainObject obj = findPinnedPlayer();
        if (obj != null) {
            pinnedPlayerX = obj.x;
            pinnedPlayerY = obj.y;
        }
        restorePinnedFocus();
        logDebug("Pinned player ID " + id);
    }

    public static void disableAwtIme() {
        try {
            java.awt.Frame[] frames = java.awt.Frame.getFrames();
            if (frames != null) {
                for (int i = 0; i < frames.length; i++) {
                    frames[i].enableInputMethods(false);
                }
            }
        } catch (Throwable ignored) {}
    }

    public static void paintPinnedTargetHud(mGraphics g) {
        if (GameScreen.player == null) return;
        if (pinnedPlayerId == -1) return; // Chỉ vẽ HUD banner cho người chơi PK được ghim

        MainObject target = findPinnedPlayer();
        boolean isPlayer = true;
        if (target == null) return;

        GameCanvas.resetTrans(g);

        int hudW = 210;
        int hudH = 36;
        int hudX = GameCanvas.w / 2 - hudW / 2;
        int hudY = 3;

        // Vẽ khung gỗ viền vàng HSO
        g.setColor(0x1a0f08);
        g.fillRect(hudX, hudY, hudW, hudH, false);
        g.setColor(0x35190e);
        g.fillRect(hudX + 2, hudY + 2, hudW - 4, hudH - 4, false);
        g.setColor(isPlayer ? 0xff5533 : 0xc8963e);
        g.drawRect(hudX, hudY, hudW, hudH, false);
        g.setColor(0x5a2d12);
        g.drawRect(hudX + 1, hudY + 1, hudW - 2, hudH - 2, false);

        // Dòng 1: Tiêu đề ghim
        String title = (isPlayer ? "[GHIM PK] " : "[GHIM QUAI] ") + target.name + " Lv." + target.Lv;
        mFont.tahoma_7b_white.drawString(g, title, hudX + 6, hudY + 2, 0, false);

        // Trạng thái Đồ Sát (nếu ghim player)
        if (isPlayer) {
            if (GameScreen.player.typePk == 0) {
                mFont.tahoma_7_yellow.drawString(g, "DO SAT: BAT", hudX + hudW - 6, hudY + 2, 1, false);
            } else {
                mFont.tahoma_7_red.drawString(g, "!CHUA DO SAT!", hudX + hudW - 6, hudY + 2, 1, false);
            }
        }

        // Dòng 2: Thanh máu
        int barX = hudX + 6;
        int barY = hudY + 14;
        int barW = hudW - 12;
        int barH = 8;
        g.setColor(0x100804);
        g.fillRect(barX, barY, barW, barH, false);
        g.setColor(0x5a3118);
        g.drawRect(barX, barY, barW, barH, false);

        if (target.maxHp > 0 && target.hp > 0) {
            int fillW = (int)((long)target.hp * (barW - 2) / target.maxHp);
            if (fillW < 1) fillW = 1;
            if (fillW > barW - 2) fillW = barW - 2;
            g.setColor(0xd42a2a);
            g.fillRect(barX + 1, barY + 1, fillW, barH - 2, false);
            g.setColor(0xff7a7a);
            g.fillRect(barX + 1, barY + 1, fillW, 1, false);
        }
        String hpText = target.hp + "/" + target.maxHp;
        mFont.tahoma_7_white.drawString(g, hpText, barX + barW / 2, barY - 1, 2, false);

        // Dòng 3: Khoảng cách & Hướng
        int dist = MainObject.getDistance(GameScreen.player.x, GameScreen.player.y, target.x, target.y);
        int dx = target.x - GameScreen.player.x;
        String dirText = (dx < -60 ? "<- " : "") + "Cu ly: " + dist + "px (" + target.x + "," + target.y + ")" + (dx > 60 ? " ->" : "");
        mFont.tahoma_7_white.drawString(g, dirText, hudX + 6, hudY + 24, 0, false);

        // Mũi tên định vị ngoài màn hình
        if (MainScreen.cameraMain != null) {
            int screenX = target.x - MainScreen.cameraMain.xCam;
            int screenY = target.y - MainScreen.cameraMain.yCam;
            if (screenX < 20 || screenX > GameCanvas.w - 20 || screenY < 20 || screenY > GameCanvas.h - 20) {
                int arrowX = screenX < 20 ? 16 : (screenX > GameCanvas.w - 20 ? GameCanvas.w - 16 : screenX);
                int arrowY = screenY < 45 ? 50 : (screenY > GameCanvas.h - 30 ? GameCanvas.h - 30 : screenY);
                String arrowStr = (screenX < 20 ? "<- " : "") + target.name + " (" + dist + "p)" + (screenX > GameCanvas.w - 20 ? " ->" : "");
                g.setColor(0x1a0f08);
                int strW = mFont.tahoma_7b_white.getWidth(arrowStr) + 8;
                g.fillRect(arrowX - strW / 2, arrowY - 2, strW, 14, false);
                g.setColor(0xffd700);
                g.drawRect(arrowX - strW / 2, arrowY - 2, strW, 14, false);
                mFont.tahoma_7b_white.drawString(g, arrowStr, arrowX, arrowY, 2, false);
            }
        }
    }

    public static void executeRemotePkStrike(final MainObject target, final int skillId, final mVector targets) {
        if (target == null || GameScreen.player == null) return;
        // Chặn race condition: không spawn thread mới khi đang có strike đang chạy
        if (isStrikeActive) return;
        isStrikeActive = true;
        new Thread(new Runnable() {
            public void run() {
                try {
                    final int startX = GameScreen.player.x;
                    final int startY = GameScreen.player.y;
                    final int targetX = target.x;
                    final int targetY = target.y;
                    logDebug("executeRemotePkStrike START: from (" + startX + "," + startY
                            + ") to (" + targetX + "," + targetY + ") skill=" + skillId);

                    remoteOrigX = startX;
                    remoteOrigY = startY;
                    lastRemoteMoveTime = System.currentTimeMillis();

                    // Vị trí ảo trước mặt mục tiêu 24px để lọt vào tầm đánh của cả đòn đánh thường và skill xa
                    int dx = targetX - startX;
                    int dy = targetY - startY;
                    int dist = MainObject.getDistance(dx, dy);
                    int strikeX = targetX;
                    int strikeY = targetY;
                    if (dist > 30) {
                        strikeX = targetX - (dx * 24 / dist);
                        strikeY = targetY - (dy * 24 / dist);
                    }

                    // Bước 1: Bước nhảy ảo tới mục tiêu (tối đa 2 bước siêu nhanh để server không giật lag)
                    if (dist > 300) {
                        int midX = (startX + strikeX) / 2;
                        int midY = (startY + strikeY) / 2;
                        GlobalService.gI().player_move((short)midX, (short)midY);
                        try { Thread.sleep(12); } catch (Exception ignored) {}
                    }
                    GlobalService.gI().player_move((short)strikeX, (short)strikeY);

                    // Chờ nhẹ 35ms để server nhận tọa độ
                    try { Thread.sleep(35); } catch (Exception ignored) {}

                    // Bước 2: Gửi gói tấn công PK
                    GlobalService.gI().fire_Pk(targets, (byte)skillId);
                    logDebug("executeRemotePkStrike FIRED at (" + strikeX + "," + strikeY + ")");

                    // Chờ server xử lý đòn đánh 35ms
                    try { Thread.sleep(35); } catch (Exception ignored) {}

                    // Bước 3: Rút vị trí server-side lập tức về vị trí gốc
                    if (dist > 300) {
                        int midX = (startX + strikeX) / 2;
                        int midY = (startY + strikeY) / 2;
                        GlobalService.gI().player_move((short)midX, (short)midY);
                        try { Thread.sleep(10); } catch (Exception ignored) {}
                    }
                    GlobalService.gI().player_move((short)startX, (short)startY);

                    // Đồng bộ giữ vững vị trí local tại startX, startY
                    if (GameScreen.player != null) {
                        GameScreen.player.x = startX;
                        GameScreen.player.y = startY;
                        GameScreen.player.toX = startX;
                        GameScreen.player.toY = startY;
                        GameScreen.player.xStand = (short)startX;
                        GameScreen.player.yStand = (short)startY;
                        GameScreen.player.vx = 0;
                        GameScreen.player.vy = 0;
                        GameScreen.player.posTransRoad = null;
                        GameScreen.player.timeSendmove = System.currentTimeMillis();
                    }

                    // Cập nhật timestamp bảo vệ để ReadMessenge.java nuốt toàn bộ các gói objectMove dội về trong 3 giây tiếp theo
                    lastRemoteMoveTime = System.currentTimeMillis();
                    logDebug("executeRemotePkStrike RESTORED cleanly to (" + startX + "," + startY + ")");
                } catch (Exception e) {
                    logDebug("executeRemotePkStrike error: " + e);
                } finally {
                    isStrikeActive = false;
                }
            }
        }).start();
    }

    public static void attackPlayerRemote(int targetPlayerId) {
        Player player = GameScreen.player;
        if (player == null || player.hp <= 0 || player.Action == 4 || player.Action == 2) return;

        MainObject target = findPinnedPlayer();
        if (target == null && GameScreen.Vecplayers != null) {
            for (int i = 0; i < GameScreen.Vecplayers.size(); ++i) {
                MainObject obj = (MainObject)GameScreen.Vecplayers.elementAt(i);
                if (obj != null && obj.typeObject == 0 && obj.ID == targetPlayerId
                        && !obj.isRemove && !obj.isDie && obj.Action != 4) {
                    target = obj;
                    break;
                }
            }
        }
        if (target == null) return;

        pinnedPlayerX = target.x;
        pinnedPlayerY = target.y;

        int readySkillId = 0;
        if (player.mhotkey != null && Player.levelTab < player.mhotkey.length) {
            HotKey[] keys = player.mhotkey[Player.levelTab];
            if (keys != null) {
                for (int k = 0; k < keys.length; ++k) {
                    if (keys[k] != null && keys[k].type == HotKey.SKILL) {
                        int sid = keys[k].id;
                        Skill s = MainListSkill.getSkillFormId(sid);
                        if (s != null && s.typeSkill == 0 && player.setDelaySkill(sid, -1)) {
                            readySkillId = sid;
                            break;
                        }
                    }
                }
            }
        }

        GameScreen.ObjFocus = target;
        PaintInfoGameScreen.isPaintInfoFocus = true;
        if (player.typePk != 0 && target.typePk == -1) {
            player.typePk = 0;
            GlobalService.gI().set_Pk((byte)0);
            GameCanvas.addInfoChar("Tự động bật Đồ Sát!");
        }
        player.setStartSkill(readySkillId, false);
        logDebug("attackPlayerRemote: fired via setStartSkill at player ID=" + targetPlayerId + " (" + target.name + ") skill=" + readySkillId + " player.typePk=" + player.typePk + " target.typePk=" + target.typePk);
    }

    public static boolean firePinnedSkill(int skillId, int hotkeyIndex) {
        Player player = GameScreen.player;
        if (player == null || player.Action == 4 || player.isDie
                || player.currentQuest != null || player.isTanHinh) {
            return (pinnedPlayerId != -1 || pinnedMonsterId != -1);
        }

        // 1. Trường hợp có ghim Người chơi -> Tấn công PK từ xa trực tiếp
        if (pinnedPlayerId != -1) {
            MainObject targetPl = findPinnedPlayer();
            if (targetPl != null && (targetPl.Action == 4 || targetPl.isDie || targetPl.isRemove)) {
                return true;
            }
            if (targetPl == null) {
                return true;
            }
            if (player.Action == 2) {
                return true;
            }
            // Đang thực hiện strike từ xa -> nuốt phím, không spawn thêm thread
            if (isStrikeActive) {
                return true;
            }
            if (skillId > 0 && !player.setDelaySkill(skillId, hotkeyIndex)) {
                return true;
            }
            if (skillId == 0 && !ListSkill.canAttack()) {
                return true;
            }
            if (skillId == 0) {
                ListSkill.doSetTimeAtt();
            }

            pinnedPlayerX = targetPl.x;
            pinnedPlayerY = targetPl.y;
            GameScreen.ObjFocus = targetPl;
            PaintInfoGameScreen.isPaintInfoFocus = true;

            if (player.typePk != 0 && targetPl.typePk == -1) {
                player.typePk = 0;
                GlobalService.gI().set_Pk((byte)0);
                GameCanvas.addInfoChar("Tự động bật Đồ Sát!");
            }

            player.setStartSkill(skillId, false);
            logDebug("firePinnedSkill: fired via setStartSkill at player ID=" + pinnedPlayerId + " (" + targetPl.name + ") skill=" + skillId + " player.typePk=" + player.typePk + " target.typePk=" + targetPl.typePk);
            return true;
        }

        // 2. Trường hợp có ghim Quái vật -> Tấn công quái vật được ghim
        if (pinnedMonsterId != -1) {
            MainObject target = findPinnedMonster();
            MonsterRecord rec = findMonsterRecord(pinnedMonsterId);
            boolean isAlive = (target != null && target.Action != 4 && !target.isDie && !target.isRemove && target.hp > 0)
                           || (rec != null && rec.hp > 0);
            if (!isAlive) {
                return true;
            }

            if (player.Action == 2 || player.Action == 4 || player.isDie) {
                return true;
            }
            if (skillId > 0 && !player.setDelaySkill(skillId, hotkeyIndex)) {
                return true;
            }
            if (skillId == 0 && !ListSkill.canAttack()) {
                return true;
            }
            if (skillId == 0) {
                ListSkill.doSetTimeAtt();
            }

            if (target != null) {
                GameScreen.ObjFocus = target;
                PaintInfoGameScreen.isPaintInfoFocus = true;
            }

            mVector targets = new mVector("Pinned monster target");
            targets.addElement(new Object_Effect_Skill((short)pinnedMonsterId, (byte)1));
            GlobalService.gI().fire_monster(targets, (byte)skillId);
            logDebug("firePinnedSkill: fired at pinned monster ID=" + pinnedMonsterId + " skill=" + skillId);
            return true;
        }

        return false;
    }

    public static void quickChangeArea(int targetArea) {
        if (GameCanvas.currentScreen != GameCanvas.game || GameScreen.player == null
                || targetArea < 0 || targetArea >= LoadMap.MaxArea) {
            logDebug("quickChangeArea: invalid request " + targetArea);
            return;
        }
        if (targetArea == LoadMap.Area) {
            logDebug("Already in Area " + targetArea);
            return;
        }
        if (LoadMap.mStatusArea != null && targetArea < LoadMap.mStatusArea.length
                && LoadMap.mStatusArea[targetArea] >= 2) {
            logDebug("quickChangeArea: area " + targetArea + " is full");
            return;
        }

        // Menu gốc lấy danh sách khu trước khi gửi packet 51, không cần tới NPC.
        if (GameCanvas.menu2 != null) {
            GameCanvas.menu2.doCloseMenu();
        }
        GameCanvas.end_Dialog();
        pendingTargetArea = targetArea;
        pendingAreaTime = System.currentTimeMillis();
        isSilentRequestArea = true;
        logDebug("quickChangeArea: requesting areas before Change_Area(" + targetArea + ")");
        GlobalService.gI().Request_Area();
    }

    public static void onChangeAreaSuccess() {
        logDebug("Change area SUCCESS confirmed!");
    }

    private static String extractJsonValue(String json, String key) {
        int idx = json.indexOf("\"" + key + "\":");
        if (idx == -1) return null;
        String sub = json.substring(idx + key.length() + 3).trim();
        int comma = sub.indexOf(",");
        int brace = sub.indexOf("}");
        int end = -1;
        if (comma != -1 && brace != -1) end = Math.min(comma, brace);
        else if (comma != -1) end = comma;
        else if (brace != -1) end = brace;
        if (end != -1) sub = sub.substring(0, end).trim();
        return sub;
    }

    private static boolean isJsonBoolTrue(String json, String key) {
        String val = extractJsonValue(json, key);
        if (val == null) return false;
        val = val.trim().toLowerCase();
        return val.equals("true") || val.equals("1");
    }

    public static void configureLootFilter(String cmd) {
        if (cmd == null) return;
        if (cmd.indexOf("\"white\":") != -1) lootWhite = isJsonBoolTrue(cmd, "white");
        if (cmd.indexOf("\"blue\":") != -1) lootBlue = isJsonBoolTrue(cmd, "blue");
        if (cmd.indexOf("\"yellow\":") != -1) lootYellow = isJsonBoolTrue(cmd, "yellow");
        if (cmd.indexOf("\"purple\":") != -1) lootPurple = isJsonBoolTrue(cmd, "purple");
        if (cmd.indexOf("\"orange\":") != -1) lootOrange = isJsonBoolTrue(cmd, "orange");
        if (cmd.indexOf("\"green\":") != -1) lootGreen = isJsonBoolTrue(cmd, "green");
        if (cmd.indexOf("\"hp\":") != -1) lootHp = isJsonBoolTrue(cmd, "hp");
        if (cmd.indexOf("\"mp\":") != -1) lootMp = isJsonBoolTrue(cmd, "mp");
        if (cmd.indexOf("\"gold\":") != -1) lootGold = isJsonBoolTrue(cmd, "gold");
        if (cmd.indexOf("\"material\":") != -1) lootMaterial = isJsonBoolTrue(cmd, "material");
        if (cmd.indexOf("\"quest\":") != -1) lootQuest = isJsonBoolTrue(cmd, "quest");
    }

    private static void checkCommand() {
        try {
            File claimed = null;
            File[] queued = new File(CMD_DIR).listFiles();
            if (queued != null) {
                for (int i = 0; i < queued.length; ++i) {
                    File candidate = queued[i];
                    if (!candidate.getName().endsWith(".json")
                            || claimed != null && candidate.getName().compareTo(claimed.getName()) >= 0) continue;
                    claimed = candidate;
                }
            }
            if (claimed == null) {
                File f = new File(CMD_FILE);
                claimed = new File(CMD_FILE + ".processing");
                if (claimed.exists()) claimed.delete();
                if (!f.exists() || !f.renameTo(claimed)) return;
            }
            FileInputStream fis = new FileInputStream(claimed);
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            byte[] buf = new byte[1024];
            int n;
            while ((n = fis.read(buf)) != -1) {
                baos.write(buf, 0, n);
            }
            fis.close();
            claimed.delete();

            String cmd = new String(baos.toByteArray(), "UTF-8");

            if (cmd.indexOf("\"unpin_monster\"") != -1 || cmd.indexOf("\"unpin_target\"") != -1 || cmd.indexOf("\"unpin_player\"") != -1) {
                unpinMonster();
                unpinPlayer();
            }

            if (cmd.indexOf("\"pin_player\"") != -1) {
                String id = extractJsonValue(cmd, "id");
                String mapId = extractJsonValue(cmd, "map_id");
                String area = extractJsonValue(cmd, "area");
                if (id != null && mapId != null && area != null) {
                    pinPlayer(Integer.parseInt(id), Integer.parseInt(mapId), Integer.parseInt(area));
                }
            }

            if (cmd.indexOf("\"attack_player\"") != -1) {
                String id = extractJsonValue(cmd, "id");
                if (id != null) {
                    attackPlayerRemote(Integer.parseInt(id));
                } else if (pinnedPlayerId != -1) {
                    attackPlayerRemote(pinnedPlayerId);
                }
            }

            if (cmd.indexOf("\"attack_monster\"") != -1) {
                String id = extractJsonValue(cmd, "id");
                int monId = id != null ? Integer.parseInt(id) : pinnedMonsterId;
                if (monId >= 0 && GameScreen.player != null) {
                    MainObject target = findPinnedMonster();
                    if (target != null) {
                        GameScreen.ObjFocus = target;
                        PaintInfoGameScreen.isPaintInfoFocus = true;
                    }
                    mVector targets = new mVector("Attack pinned monster");
                    targets.addElement(new Object_Effect_Skill((short)monId, (byte)1));
                    GlobalService.gI().fire_monster(targets, (byte)0);
                    logDebug("attack_monster command executed: ID=" + monId);
                }
            }

            if (cmd.indexOf("\"set_pk\"") != -1) {
                String pk = extractJsonValue(cmd, "type_pk");
                byte pkType = pk != null ? Byte.parseByte(pk) : (byte)0;
                GlobalService.gI().set_Pk(pkType);
                logDebug("set_Pk command executed: typePk=" + pkType + " (current was " + (GameScreen.player != null ? GameScreen.player.typePk : "null") + ")");
            }

            if (cmd.indexOf("\"farm_targets\"") != -1) {
                configureFarmTargets(cmd);
            }

            if (cmd.indexOf("\"scan_monsters\"") != -1) {
                startMonsterScan();
            }

            if (cmd.indexOf("\"pin_monster\"") != -1) {
                String id = extractJsonValue(cmd, "id");
                String mapId = extractJsonValue(cmd, "map_id");
                String area = extractJsonValue(cmd, "area");
                if (id != null && mapId != null && area != null) {
                    pinMonster(Integer.parseInt(id), Integer.parseInt(mapId), Integer.parseInt(area));
                }
            }

            // Lệnh yêu cầu danh sách khu
            if (cmd.indexOf("\"request_areas\"") != -1) {
                isSilentRequestArea = true;
                GlobalService.gI().Request_Area();
            }

            // Lệnh đổi khu
            if (cmd.indexOf("\"change_area\"") != -1) {
                String sub = extractJsonValue(cmd, "area");
                if (sub != null) {
                    int targetArea = Integer.parseInt(sub);
                    if (targetArea >= 0) {
                        quickChangeArea(targetArea);
                    }
                }
            }

            // Lệnh auto vào khu ít người nhất
            if (cmd.indexOf("\"auto_min_area\"") != -1) {
                if (LoadMap.mStatusArea != null && LoadMap.mStatusArea.length > 0) {
                    int bestArea = -1;
                    int bestStatus = Integer.MAX_VALUE;
                    for (int i = 0; i < LoadMap.mStatusArea.length; ++i) {
                        if (i != LoadMap.Area && LoadMap.mStatusArea[i] < bestStatus && LoadMap.mStatusArea[i] < 2) {
                            bestStatus = LoadMap.mStatusArea[i];
                            bestArea = i;
                        }
                    }
                    if (bestArea >= 0) {
                        quickChangeArea(bestArea);
                    }
                    pendingMinArea = false;
                } else {
                    pendingMinArea = true;
                    isSilentRequestArea = true;
                    GlobalService.gI().Request_Area();
                }
            }

            // Lệnh auto đánh
            if (cmd.indexOf("\"auto_attack\"") != -1) {
                if (cmd.indexOf("\"enabled\": true") != -1 || cmd.indexOf("\"enabled\":true") != -1 || cmd.indexOf("\"enabled\": 1") != -1) {
                    isAutoAttack = true;
                    if (isFarmAttackActive()) stopFarmAutoMovement();
                    GameScreen.ObjFocus = null;
                    PaintInfoGameScreen.isPaintInfoFocus = false;
                    if (GameScreen.player != null) {
                        GameScreen.player.KillFire = -1;
                        GameScreen.player.IDAttack = -1;
                        GameScreen.player.posTransRoad = null;
                    }
                    GameCanvas.keyMyPressed[25] = false;
                    GameCanvas.keyMyPressed[5] = false;
                } else if (cmd.indexOf("\"enabled\": false") != -1 || cmd.indexOf("\"enabled\":false") != -1 || cmd.indexOf("\"enabled\": 0") != -1) {
                    stopAllAutoAttack();
                }
            }

            // Lệnh auto nhặt đồ
            if (cmd.indexOf("\"auto_pickup\"") != -1) {
                if (cmd.indexOf("\"enabled\": true") != -1 || cmd.indexOf("\"enabled\":true") != -1 || cmd.indexOf("\"enabled\": 1") != -1) {
                    isAutoPickup = true;
                } else if (cmd.indexOf("\"enabled\": false") != -1 || cmd.indexOf("\"enabled\":false") != -1 || cmd.indexOf("\"enabled\": 0") != -1) {
                    isAutoPickup = false;
                }
                configureLootFilter(cmd);
            }

            // Lệnh cấu hình bộ lọc đồ rơi
            if (cmd.indexOf("\"loot_filter\"") != -1) {
                configureLootFilter(cmd);
            }

            // Lệnh tốc độ
            if (cmd.indexOf("\"speed\"") != -1) {
                String sub = extractJsonValue(cmd, "speed");
                if (sub != null) {
                    currentSpeedMultiplier = Float.parseFloat(sub);
                    if (GameScreen.player != null && currentSpeedMultiplier >= 1.0f) {
                        GameScreen.player.vMax = (int)Math.round(6 * currentSpeedMultiplier);
                    }
                }
            }

            // Lệnh di chuyển tới tọa độ
            if (cmd.indexOf("\"move_to\"") != -1) {
                int idxX = cmd.indexOf("\"x\":");
                int idxY = cmd.indexOf("\"y\":");
                if (idxX != -1 && idxY != -1 && GameScreen.player != null) {
                    String subX = cmd.substring(idxX + 4).trim();
                    int endX = subX.indexOf(",");
                    if (endX == -1) endX = subX.indexOf("}");
                    if (endX != -1) subX = subX.substring(0, endX).trim();

                    String subY = cmd.substring(idxY + 4).trim();
                    int endY = subY.indexOf(",");
                    if (endY == -1) endY = subY.indexOf("}");
                    if (endY != -1) subY = subY.substring(0, endY).trim();

                    int toX = Integer.parseInt(subX);
                    int toY = Integer.parseInt(subY);
                    short[] road = findPathTo(toX, toY);
                    if (road != null && road.length > 0) {
                        GameScreen.player.toX = GameScreen.player.x;
                        GameScreen.player.toY = GameScreen.player.y;
                        GameScreen.player.xStopMove = 0;
                        GameScreen.player.yStopMove = 0;
                        GameScreen.player.countAutoMove = 0;
                        GameScreen.player.resetMove();
                        GameScreen.player.posTransRoad = road;
                    }
                }
            }
        }
        catch (Exception exception) {
        }
    }

    private static void writeState() {
        try {
            StringBuffer sb = new StringBuffer();
            sb.append("{");

            boolean inGame = (GameCanvas.loadmap != null && GameScreen.player != null
                    && GameScreen.player.name != null && !GameScreen.player.name.equals("unname")
                    && GameScreen.player.name.length() > 0);
            sb.append("\"in_game\":").append(inGame).append(",");

            if (inGame && GameCanvas.loadmap != null) {
                sb.append("\"map_id\":").append(GameCanvas.loadmap.idMap).append(",");
                sb.append("\"map_name\":\"").append(escape(GameCanvas.loadmap.nameMap)).append("\",");
                sb.append("\"map_width\":").append(GameCanvas.loadmap.mapW * LoadMap.wTile).append(",");
                sb.append("\"map_height\":").append(GameCanvas.loadmap.mapH * LoadMap.wTile).append(",");
                sb.append("\"area\":").append((int)LoadMap.Area).append(",");
                MainObject focused = GameScreen.ObjFocus;
                sb.append("\"focus_monster_id\":")
                  .append(focused != null && focused.typeObject == 1 ? focused.ID : -1).append(",");
                sb.append("\"focus_target_id\":")
                  .append(focused != null ? focused.ID : -1).append(",");
                sb.append("\"pinned_monster_id\":").append(pinnedMonsterId).append(",");
                sb.append("\"pinned_player_id\":").append(pinnedPlayerId).append(",");
                sb.append("\"max_area\":").append((int)LoadMap.MaxArea).append(",");

                // Danh sách các khu trong map
                if (LoadMap.mStatusArea != null && LoadMap.mStatusArea.length > 0) {
                    sb.append("\"areas\":[");
                    for (int i = 0; i < LoadMap.mStatusArea.length; ++i) {
                        if (i > 0) sb.append(",");
                        sb.append("{\"area\":").append(i + 1)
                          .append(",\"status\":").append(LoadMap.mStatusArea[i])
                          .append("}");
                    }
                    sb.append("],");
                } else if (LoadMap.MaxArea > 0) {
                    sb.append("\"areas\":[");
                    for (int i = 0; i < LoadMap.MaxArea; ++i) {
                        if (i > 0) sb.append(",");
                        sb.append("{\"area\":").append(i + 1)
                          .append(",\"status\":-1}");
                    }
                    sb.append("],");
                } else {
                    sb.append("\"areas\":[],");
                }

                sb.append("\"player\":{");
                sb.append("\"name\":\"").append(escape(GameScreen.player.name)).append("\",");
                sb.append("\"lv\":").append(GameScreen.player.Lv).append(",");
                sb.append("\"hp\":").append(GameScreen.player.hp).append(",");
                sb.append("\"maxHp\":").append(GameScreen.player.maxHp).append(",");
                sb.append("\"mp\":").append(GameScreen.player.mp).append(",");
                sb.append("\"maxMp\":").append(GameScreen.player.maxMp).append(",");
                sb.append("\"maxmp\":").append(GameScreen.player.maxMp).append(",");
                sb.append("\"exp\":").append((int)GameScreen.player.phantramLv).append(",");
                sb.append("\"x\":").append(GameScreen.player.x).append(",");
                sb.append("\"y\":").append(GameScreen.player.y).append(",");
                sb.append("\"typePk\":").append(GameScreen.player.typePk);
                sb.append("},");

                // Snapshot an toàn danh sách đối tượng trong map
                Object[] localPlayers = null;
                if (GameScreen.Vecplayers != null) {
                    try {
                        synchronized (GameScreen.Vecplayers) {
                            int sz = GameScreen.Vecplayers.size();
                            localPlayers = new Object[sz];
                            for (int k = 0; k < sz; ++k) {
                                localPlayers[k] = GameScreen.Vecplayers.elementAt(k);
                            }
                        }
                    } catch (Exception ignored) {}
                }

                // Quét danh sách quái vật và người chơi trong map
                int playerCount = 0;
                StringBuffer sbPlayers = new StringBuffer();
                sbPlayers.append("[");

                StringBuffer sbMonsters = new StringBuffer();
                sbMonsters.append("[");
                int monsterCount = 0;

                StringBuffer sbNpcs = new StringBuffer();
                sbNpcs.append("[");
                int npcCount = 0;

                if (localPlayers != null) {
                    for (int i = 0; i < localPlayers.length; ++i) {
                        MainObject obj = (MainObject)localPlayers[i];
                        if (obj == null) continue;

                        if (obj.typeObject == 0 && obj != GameScreen.player) {
                            // Người chơi khác
                            if (playerCount > 0) sbPlayers.append(",");
                            sbPlayers.append("{\"id\":").append(obj.ID)
                                     .append(",\"name\":\"").append(escape(obj.name))
                                     .append("\",\"lv\":").append(obj.Lv)
                                     .append(",\"hp\":").append(obj.hp)
                                     .append(",\"maxHp\":").append(obj.maxHp)
                                     .append(",\"x\":").append(obj.x)
                                     .append(",\"y\":").append(obj.y)
                                     .append(",\"typePk\":").append(obj.typePk)
                                     .append("}");
                            playerCount++;
                        } else if (obj.typeObject == 1) {
                            // Quái vật
                            if (monsterCount > 0) sbMonsters.append(",");
                            sbMonsters.append("{\"id\":").append(obj.ID)
                                      .append(",\"name\":\"").append(escape(obj.name))
                                      .append("\",\"hp\":").append(obj.hp)
                                      .append(",\"maxHp\":").append(obj.maxHp)
                                      .append(",\"x\":").append(obj.x)
                                      .append(",\"y\":").append(obj.y)
                                      .append("}");
                            monsterCount++;
                        } else if (obj.typeObject == 2) {
                            // NPC
                            if (npcCount > 0) sbNpcs.append(",");
                            sbNpcs.append("{\"id\":").append(obj.ID)
                                  .append(",\"name\":\"").append(escape(obj.name))
                                  .append("\",\"x\":").append(obj.x)
                                  .append(",\"y\":").append(obj.y)
                                  .append("}");
                            npcCount++;
                        }
                    }
                }

                sbPlayers.append("]");
                sbMonsters.append("]");
                sbNpcs.append("]");

                // Waypoints list
                StringBuffer sbWaypoints = new StringBuffer();
                sbWaypoints.append("[");
                if (LoadMap.vecPointChange != null) {
                    for (int i = 0; i < LoadMap.vecPointChange.size(); ++i) {
                        Model.Point pt = (Model.Point)LoadMap.vecPointChange.elementAt(i);
                        if (pt != null) {
                            if (i > 0) sbWaypoints.append(",");
                            sbWaypoints.append("{\"name\":\"").append(escape(pt.name))
                                       .append("\",\"x\":").append(pt.x)
                                       .append(",\"y\":").append(pt.y)
                                       .append("}");
                        }
                    }
                }
                sbWaypoints.append("]");

                sb.append("\"players_count\":").append(playerCount).append(",");
                sb.append("\"players\":").append(sbPlayers.toString()).append(",");
                sb.append("\"npcs\":").append(sbNpcs.toString()).append(",");
                sb.append("\"waypoints\":").append(sbWaypoints.toString()).append(",");
                sb.append("\"monsters\":").append(sbMonsters.toString()).append(",");
                sb.append("\"map_monsters\":").append(mapMonstersJson()).append(",");
                sb.append("\"monster_scan_active\":").append(monsterScanNextId >= 0).append(",");
                sb.append("\"monster_scan_done\":").append(monsterScanDone).append(",");
                sb.append("\"monster_scan_progress\":")
                  .append(monsterScanNextId >= 0 ? monsterScanNextId : monsterScanDone ? monsterScanLimit : 0)
                  .append(",");
                sb.append("\"monster_scan_limit\":").append(monsterScanLimit);
            } else {
                sb.append("\"map_id\":0,");
                sb.append("\"map_name\":\"Chưa vào thế giới\",");
                sb.append("\"map_width\":0,\"map_height\":0,");
                sb.append("\"area\":0,");
                sb.append("\"focus_monster_id\":-1,");
                sb.append("\"focus_target_id\":-1,");
                sb.append("\"pinned_monster_id\":").append(pinnedMonsterId).append(",");
                sb.append("\"pinned_player_id\":").append(pinnedPlayerId).append(",");
                sb.append("\"max_area\":0,");
                sb.append("\"areas\":[],");
                sb.append("\"players_count\":0,");
                sb.append("\"players\":[],");
                sb.append("\"monsters\":[],\"map_monsters\":[],");
                sb.append("\"monster_scan_active\":false,\"monster_scan_done\":false,");
                sb.append("\"monster_scan_progress\":0,\"monster_scan_limit\":")
                  .append(monsterScanLimit);
            }

            sb.append(",\"auto_attack\":").append(isAutoAttack);
            sb.append(",\"farm_mode\":\"")
              .append(isFarmAttackActive() ? "normal" : "off")
              .append("\"");
            sb.append(",\"farm_packets_sent\":").append(farmPacketsSent);
            sb.append(",\"farm_damage_results\":").append(farmDamageResults);
            sb.append(",\"farm_distinct_hits\":").append(farmDamagedIds.size());
            sb.append(",\"auto_pickup\":").append(isAutoPickup);
            sb.append(",\"speed\":").append(currentSpeedMultiplier);
            sb.append(",\"bot_detected\":").append(botCaptchaDetected);
            sb.append(",\"other_players_count\":").append(otherPlayersInZone);
            sb.append("}");

            byte[] stateBytes = sb.toString().getBytes("UTF-8");
            File target = new File(STATE_FILE);
            File tmpTarget = new File(STATE_FILE + ".tmp");
            try {
                FileOutputStream fos = new FileOutputStream(tmpTarget);
                fos.write(stateBytes);
                fos.flush();
                fos.close();
                tmpTarget.renameTo(target);
            } catch (Exception ignored) {}
        }
        catch (Exception exception) {
        }
    }

    private static String escape(String s) {
        if (s == null) return "";
        return s.replace("\"", "\\\"").replace("\n", " ").replace("\r", "");
    }
}
