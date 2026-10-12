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
import netcommand.global.GlobalService;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileInputStream;
import java.io.ByteArrayOutputStream;

public class HsoBridge {
    private static long lastSyncTime = 0;
    private static final String STATE_FILE = "client_state.json";
    private static final String CMD_FILE = "client_command.json";
    private static final String CMD_DIR = "client_commands";

    public static boolean isAutoPickup = false;
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
    private static final int MONSTER_SCAN_INITIAL_LIMIT = 512;
    private static final int MONSTER_SCAN_MAX_LIMIT = 2048;
    private static int monsterScanLimit = MONSTER_SCAN_INITIAL_LIMIT;
    private static int monsterScanMapId = -1;
    private static int monsterScanArea = -1;
    private static int monsterScanNextId = -1;
    private static boolean monsterScanDone = false;
    private static long lastMonsterScanRequest = 0;
    private static final mVector mapMonsterRecords = new mVector("Auto Fam map monsters");

    private static class MonsterRecord {
        int id;
        String name;
        int hp;
        int maxHp;
        int x;
        int y;
    }
    public static int pinnedMonsterId = -1;
    private static int pinnedMapId = -1;
    private static int pinnedArea = -1;
    private static long lastPathTime = 0;
    private static int unreachableMonsterId = -1;
    private static long unreachableTime = 0;
    private static long lastPickupTime = 0;

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
        if (pinnedMonsterId >= 0 && (GameCanvas.currentScreen != GameCanvas.game || GameScreen.player == null)) {
            clearPinnedMonster();
        }
        restorePinnedFocus();
        
        // Thực hiện auto nhặt đồ rơi liên tục trên toàn map nếu bật
        if (isAutoPickup && GameScreen.player != null && GameScreen.Vecplayers != null) {
            if (now - lastPickupTime >= 200) {
                lastPickupTime = now;
                for (int i = 0; i < GameScreen.Vecplayers.size(); ++i) {
                    MainObject obj = (MainObject)GameScreen.Vecplayers.elementAt(i);
                    if (obj != null && !obj.isRemove && (obj.typeObject == 3 || obj.typeObject == 4 || obj.typeObject == 5 || obj.typeObject == 7)) {
                        GlobalService.gI().Get_Item_Map((short)obj.ID, obj.typeObject);
                        obj.isSend = false;
                    }
                }
            }
        }

        // Thực hiện thông minh: Auto Tìm Quái & Đánh (Auto-Hunt)
        if (isAutoAttack && GameCanvas.currentScreen == GameCanvas.game && GameScreen.player != null) {
            // Ngăn hệ thống auto gốc kéo giật nhân vật về vị trí cũ (leashing)
            Player.isAutoFire = (byte)-1;
            Player.isCurAutoFire = false;
            Player.xBeginAutoFire = GameScreen.player.x;
            Player.yBeginAutofire = GameScreen.player.y;

            if (farmConfigured) {
                if (GameCanvas.loadmap != null && farmMapId == GameCanvas.loadmap.idMap
                        && farmArea == LoadMap.Area) {
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
                    // Mục tiêu ghim được đánh bằng ID, không đổi sang quái gần hơn.
                    if (validTarget && ListSkill.canAttack()) {
                        GameScreen.player.setAutoFire();
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
        }

        // Bỏ trạng thái khu của map cũ trước khi xử lý yêu cầu chọn khu.
        if (GameCanvas.currentScreen == GameCanvas.game && GameCanvas.loadmap != null
                && GameCanvas.loadmap.idMap != lastMapId) {
            lastMapId = GameCanvas.loadmap.idMap;
            clearPinnedMonster();
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
        monsterScanNextId = -1;
        monsterScanDone = false;
        monsterScanLimit = MONSTER_SCAN_INITIAL_LIMIT;
        mapMonsterRecords.removeAllElements();
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
        record.hp = obj.hp;
        record.maxHp = obj.maxHp;
        record.x = obj.x;
        record.y = obj.y;
    }

    public static synchronized void rememberMonsterInfo(int id, int lv, int x, int y, int hp, int maxHp) {
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
        record.hp = hp;
        record.maxHp = maxHp;
        record.x = x;
        record.y = y;
    }

    public static synchronized void markMonsterDead(int id) {
        MonsterRecord record = findMonsterRecord(id);
        if (record != null) record.hp = 0;
    }

    public static synchronized void updateMonsterHealth(int id, int hp) {
        MonsterRecord record = findMonsterRecord(id);
        if (record != null) record.hp = Math.max(0, hp);
    }

    public static synchronized void noteFarmDamageResult(int attackerId, int monsterId, int damage) {
        if (!isFarmAttackActive() || GameScreen.player == null
                || attackerId != GameScreen.player.ID || damage <= 0
                || !isConfiguredFarmTarget(monsterId)) return;
        ++farmDamageResults;
        Integer id = Integer.valueOf(monsterId);
        if (!farmDamagedIds.contains(id)) farmDamagedIds.addElement(id);
    }

    public static void noteFarmPacketSent() {
        if (isFarmAttackActive()) ++farmPacketsSent;
    }

    public static boolean isFarmAttackActive() {
        return isAutoAttack && farmConfigured && GameCanvas.loadmap != null
                && farmMapId == GameCanvas.loadmap.idMap && farmArea == LoadMap.Area;
    }

    private static boolean isConfiguredFarmTarget(int id) {
        for (int i = 0; i < farmTargetIds.length; ++i) {
            if (farmTargetIds[i] == id) return true;
        }
        return false;
    }

    public static boolean isFarmTargetAllowed(int id) {
        return !isFarmAttackActive() || isConfiguredFarmTarget(id);
    }

    private static synchronized void startMonsterScan() {
        ensureMonsterScanContext();
        if (monsterScanMapId < 0 || monsterScanNextId >= 0 || monsterScanDone) return;
        for (int i = 0; i < mapMonsterRecords.size(); ++i) {
            MonsterRecord record = (MonsterRecord)mapMonsterRecords.elementAt(i);
            if (record.id >= monsterScanLimit - 64) {
                monsterScanLimit = Math.min(MONSTER_SCAN_MAX_LIMIT, record.id + 128);
            }
        }
        monsterScanNextId = 0;
        lastMonsterScanRequest = 0;
        logDebug("Auto Fam scan started map=" + monsterScanMapId + " area=" + monsterScanArea);
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
        int id = monsterScanNextId++;
        lastMonsterScanRequest = now;
        if (findMonsterRecord(id) == null) GlobalService.gI().monster_info((short)id);
    }

    private static synchronized String mapMonstersJson() {
        if (GameScreen.Vecplayers != null) {
            for (int i = 0; i < GameScreen.Vecplayers.size(); ++i) {
                MainObject obj = (MainObject)GameScreen.Vecplayers.elementAt(i);
                rememberLocalMonster(obj);
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
                  .append("}");
        }
        return result.append("]").toString();
    }

    private static void fireFarmBurst(long now) {
        Player player = GameScreen.player;
        if (player == null || player.hp <= 0 || player.Action == 2 || player.Action == 4
                || player.currentQuest != null || player.isTanHinh || farmTargetIds.length == 0
                || now - lastFarmAttemptTime < 250L || !ListSkill.canAttack()) return;
        if (!player.setDelaySkill(0, -1)) return;
        int[] orderedIds = orderedFarmTargetIds(player);
        if (orderedIds.length == 0) return;
        player.IDAttack = orderedIds[0];
        mVector[] groups = farmNormalAttackGroups(orderedIds);
        if (groups.length == 0) return;
        for (int i = 0; i < groups.length; ++i) {
            GlobalService.gI().fire_monster(groups[i], (byte)0);
        }
        if (Player.mKillPlayer != null && Player.mKillPlayer.length > 0) {
            player.setAddDelaySkill(Player.mKillPlayer[0], 0);
        }
        lastFarmAttemptTime = now;
    }

    private static mVector[] farmNormalAttackGroups(int[] ids) {
        // Chiêu index 0 (đánh thường) trên server HSO là đơn mục tiêu.
        // Khi gom nhiều quái vào 1 gói tin, server chỉ xử lý và gây sát thương lên quái đầu tiên.
        // Để 1 hit toàn bộ quái cùng chịu sát thương từ chiêu 0, mỗi quái cần 1 gói đánh riêng biệt.
        int count = 0;
        for (int i = 0; i < ids.length; ++i) {
            MonsterRecord rec = findMonsterRecord(ids[i]);
            if (rec == null || rec.hp > 0) {
                ++count;
            }
        }
        if (count == 0) return new mVector[0];
        mVector[] groups = new mVector[count];
        int idx = 0;
        for (int i = 0; i < ids.length; ++i) {
            MonsterRecord rec = findMonsterRecord(ids[i]);
            if (rec != null && rec.hp <= 0) continue;
            mVector targets = new mVector("Auto Fam normal attack target");
            targets.addElement(new Object_Effect_Skill((short)ids[i], (byte)1));
            groups[idx++] = targets;
        }
        return groups;
    }

    private static int[] orderedFarmTargetIds(Player player) {
        int[] ids = new int[farmTargetIds.length];
        System.arraycopy(farmTargetIds, 0, ids, 0, ids.length);
        for (int i = 0; i < ids.length; ++i) {
            int best = i;
            long bestScore = farmTargetScore(player, ids[i]);
            for (int j = i + 1; j < ids.length; ++j) {
                long score = farmTargetScore(player, ids[j]);
                if (score >= bestScore) continue;
                best = j;
                bestScore = score;
            }
            int swap = ids[i];
            ids[i] = ids[best];
            ids[best] = swap;
        }
        return ids;
    }

    private static long farmTargetScore(Player player, int id) {
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
        farmArea = Integer.parseInt(area);
        farmConfigured = Integer.parseInt(configured) != 0;
        ids = ids.replace("\"", "").trim();
        if (ids.length() == 0 || !farmConfigured) {
            farmTargetIds = new int[0];
        } else {
            String[] parts = ids.split(";");
            int[] parsedIds = new int[parts.length];
            for (int i = 0; i < parts.length; ++i) parsedIds[i] = Integer.parseInt(parts[i]);
            farmTargetIds = parsedIds;
        }
        farmPacketsSent = 0;
        farmDamageResults = 0;
        farmDamagedIds.removeAllElements();
        if (isFarmAttackActive()) stopFarmAutoMovement();
        logDebug("Auto Fam configured=" + farmConfigured + " targets=" + farmTargetIds.length);
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

    private static MainObject findNearestItem() {
        if (GameScreen.Vecplayers == null || GameScreen.player == null) return null;
        int minDistance = Integer.MAX_VALUE;
        MainObject nearest = null;
        for (int i = 0; i < GameScreen.Vecplayers.size(); ++i) {
            MainObject obj = (MainObject)GameScreen.Vecplayers.elementAt(i);
            if (obj != null && !obj.isRemove && (obj.typeObject == 3 || obj.typeObject == 4 || obj.typeObject == 5 || obj.typeObject == 7)) {
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

    private static MainObject findPinnedMonster() {
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
        if (pinnedMonsterId < 0) return false;
        MainObject target = findPinnedMonster();
        if (target != null) {
            GameScreen.ObjFocus = target;
            PaintInfoGameScreen.isPaintInfoFocus = true;
            if (GameScreen.player != null) GameScreen.player.IDAttack = target.ID;
        } else if (GameScreen.ObjFocus != null) {
            GameScreen.ObjFocus = null;
            PaintInfoGameScreen.isPaintInfoFocus = false;
        }
        return true;
    }

    public static void pinMonster(int id, int mapId, int area) {
        if (GameCanvas.currentScreen != GameCanvas.game || GameCanvas.loadmap == null
                || GameCanvas.loadmap.idMap != mapId || LoadMap.Area != area || GameScreen.Vecplayers == null) {
            logDebug("pinMonster: stale map or area for ID " + id);
            return;
        }
        for (int i = 0; i < GameScreen.Vecplayers.size(); ++i) {
            MainObject obj = (MainObject)GameScreen.Vecplayers.elementAt(i);
            if (obj != null && obj.typeObject == 1 && obj.ID == id
                    && !obj.isRemove && !obj.isDie && obj.hp > 0 && obj.Action != 4) {
                pinnedMonsterId = id;
                pinnedMapId = mapId;
                pinnedArea = area;
                restorePinnedFocus();
                logDebug("Pinned monster ID " + id);
                return;
            }
        }
        logDebug("pinMonster: monster ID " + id + " is unavailable");
    }

    public static boolean firePinnedSkill(int skillId, int hotkeyIndex) {
        if (pinnedMonsterId < 0) return false;
        MainObject target = findPinnedMonster();
        Player player = GameScreen.player;
        if (target == null || player == null || player.Action == 2 || player.Action == 4
                || player.currentQuest != null || player.isTanHinh || !ListSkill.canAttack()
                || !player.setDelaySkill(skillId, hotkeyIndex) || !player.setFirePlayer(target)) {
            return true;
        }
        mVector targets = new mVector("Pinned monster skill");
        targets.addElement(new Object_Effect_Skill((short)target.ID, target.typeObject));
        player.IDAttack = target.ID;
        GlobalService.gI().fire_monster(targets, (byte)skillId);
        return true;
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

            if (cmd.indexOf("\"unpin_monster\"") != -1) {
                unpinMonster();
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
                } else if (cmd.indexOf("\"enabled\": false") != -1 || cmd.indexOf("\"enabled\":false") != -1 || cmd.indexOf("\"enabled\": 0") != -1) {
                    isAutoAttack = false;
                    huntTargetId = -1;
                    if (GameScreen.player != null) {
                        GameScreen.player.posTransRoad = null;
                        GameScreen.player.toX = GameScreen.player.x;
                        GameScreen.player.toY = GameScreen.player.y;
                        GameScreen.player.resetMove();
                    }
                }
            }

            // Lệnh auto nhặt đồ
            if (cmd.indexOf("\"auto_pickup\"") != -1) {
                if (cmd.indexOf("\"enabled\": true") != -1 || cmd.indexOf("\"enabled\":true") != -1 || cmd.indexOf("\"enabled\": 1") != -1) {
                    isAutoPickup = true;
                } else if (cmd.indexOf("\"enabled\": false") != -1 || cmd.indexOf("\"enabled\":false") != -1 || cmd.indexOf("\"enabled\": 0") != -1) {
                    isAutoPickup = false;
                }
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

            boolean inGame = (GameCanvas.currentScreen == GameCanvas.game && GameScreen.player != null);
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
                sb.append("\"pinned_monster_id\":").append(pinnedMonsterId).append(",");
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
                sb.append("\"maxmp\":").append(GameScreen.player.maxMp).append(",");
                sb.append("\"exp\":").append((int)GameScreen.player.phantramLv).append(",");
                sb.append("\"x\":").append(GameScreen.player.x).append(",");
                sb.append("\"y\":").append(GameScreen.player.y);
                sb.append("},");

                // Quét danh sách quái vật và người chơi trong map
                int playerCount = 0;
                StringBuffer sbPlayers = new StringBuffer();
                sbPlayers.append("[");

                StringBuffer sbMonsters = new StringBuffer();
                sbMonsters.append("[");
                int monsterCount = 0;

                if (GameScreen.Vecplayers != null) {
                    for (int i = 0; i < GameScreen.Vecplayers.size(); ++i) {
                        MainObject obj = (MainObject)GameScreen.Vecplayers.elementAt(i);
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
                        }
                    }
                }

                sbPlayers.append("]");
                sbMonsters.append("]");

                // NPC list
                StringBuffer sbNpcs = new StringBuffer();
                sbNpcs.append("[");
                int npcCount = 0;
                if (GameScreen.Vecplayers != null) {
                    for (int i = 0; i < GameScreen.Vecplayers.size(); ++i) {
                        MainObject obj = (MainObject)GameScreen.Vecplayers.elementAt(i);
                        if (obj != null && obj.typeObject == 2) {
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
                sb.append("\"pinned_monster_id\":-1,");
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
            sb.append("}");

            File tmp = new File(STATE_FILE + ".tmp");
            FileOutputStream fos = new FileOutputStream(tmp);
            fos.write(sb.toString().getBytes("UTF-8"));
            fos.close();

            File target = new File(STATE_FILE);
            tmp.renameTo(target);
        }
        catch (Exception exception) {
        }
    }

    private static String escape(String s) {
        if (s == null) return "";
        return s.replace("\"", "\\\"").replace("\n", " ").replace("\r", "");
    }
}
