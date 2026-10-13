package com.silverknight;

import GameScreen.GameScreen;
import GameScreen.PaintInfoGameScreen;
import GameObjects.MainObject;
import GameObjects.MainMonster;
import GameObjects.Player;
import GameObjects.Item;
import GameObjects.Object_Effect_Skill;
import GameObjects.NPC;
import GameObjects.NPCserver;
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
    public static boolean isAutoSell = false;
    public static boolean sellWhite = true;
    public static boolean sellBlue = true;
    public static boolean sellYellow = false;
    public static boolean sellPurple = false;
    public static boolean sellOrange = false;
    public static boolean sellGreen = false;
    public static boolean sellMaterial = false;
    public static boolean sellTier0Only = true;
    public static boolean sellUnlockedOnly = true;
    public static volatile boolean isAutoOpeningShop = false;
    public static volatile boolean isSelling = false;
    public static volatile int vibeSoldItems = 0;
    public static volatile int vibeTotalItems = 0;
    private static long lastAutoSellCheckTime = 0L;
    private static volatile long remoteSellInventoryRevision = 0L;
    private static final RemoteSellController remoteSell = new RemoteSellController(new RemoteSellController.Transport() {
        public boolean connected() { return CLib.Session_ME.gI().isConnected(); }
        public int playerId() { return GameScreen.player == null ? -1 : GameScreen.player.ID; }
        public int mapId() { return GameCanvas.loadmap == null ? -1 : GameCanvas.loadmap.idMap; }
        public int area() { return LoadMap.Area; }
        public int x() { return GameScreen.player == null ? 0 : GameScreen.player.x; }
        public int y() { return GameScreen.player == null ? 0 : GameScreen.player.y; }
        public boolean alive() {
            return GameScreen.player != null && GameScreen.player.hp > 0 && GameScreen.player.Action != 4;
        }
        public int quantity(RemoteSellController.Target target) {
            if (Item.VecInvetoryPlayer == null) return -1;
            Item item = findInventoryItem(target.id, target.category);
            if (item == null) return 0;
            if (!target.name.equals(item.itemName)) return -1;
            return item.ItemCatagory == 3 ? 1 : Math.max(1, item.numPotion);
        }
        public boolean allowed(RemoteSellController.Target target) {
            Item item = findInventoryItem(target.id, target.category);
            if (item == null || item.setNameNull()) return false;
            if (!target.explicit) return shouldSellItem(item);
            return (item.ItemCatagory == 3 || item.ItemCatagory == 7)
                    && (target.allowLocked || !isLockedItem(item));
        }
        public long inventoryRevision() { return remoteSellInventoryRevision; }
        public boolean safeVirtualPosition(int x, int y) {
            if (GameCanvas.loadmap == null || x < 0 || y < 0
                    || x >= GameCanvas.loadmap.mapW * LoadMap.wTile
                    || y >= GameCanvas.loadmap.mapH * LoadMap.wTile) return false;
            // Virtual packets must not touch a gate that could actually change the map.
            if (LoadMap.vecPointChange != null) {
                for (int i = 0; i < LoadMap.vecPointChange.size(); ++i) {
                    Model.Point point = (Model.Point)LoadMap.vecPointChange.elementAt(i);
                    if (point != null && MainObject.getDistance(x, y, point.x, point.y) <= 48) return false;
                }
            }
            // Do not apply local pathfinding to packet-only positions. NPC bodies are
            // blocked tiles in the renderer; the server decides whether a move is valid.
            return true;
        }
        public void move(int x, int y) { GlobalService.gI().remoteSellMove(x, y); }
        public void menu(int npcId) { GlobalService.gI().remoteSellMenu(npcId); }
        public void selectMenu(int npcId, int menuId, int option) {
            GlobalService.gI().remoteSellShop(npcId, menuId, option);
        }
        public void sell(RemoteSellController.Target target) {
            GlobalService.gI().remoteSellItem(target.id, target.category);
        }
        public void confirm(int id, int type) { GlobalService.gI().remoteSellConfirm(id, type); }
        public void log(String text) { logDebug(text); }
    });
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
    private static volatile boolean farmSyncMode = true;
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
        remoteSell.tick(now);
        isAutoOpeningShop = remoteSell.isActive() || isSelling;
        ensureMonsterScanContext();
        if (!isRemoteSellBusy()) {
            advanceMonsterScan(now);
            advanceMonsterRefresh(now);
        }
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
        if (!isRemoteSellBusy() && isAutoPickup && GameScreen.player != null && GameScreen.Vecplayers != null) {
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

        // Tự động bán đồ rác nếu bật Auto Sell (chỉ khi ở màn hình game và không mở dialog)
        if (isAutoSell && GameCanvas.currentScreen == GameCanvas.game && GameScreen.player != null) {
            long retryDelay = "failed".equals(remoteSell.phase()) ? 60000L : 3000L;
            if (now - lastAutoSellCheckTime >= retryDelay && !isRemoteSellBusy()) {
                lastAutoSellCheckTime = now;
                if (GameCanvas.currentDialog == null) {
                    sellMatchingItems(true);
                }
            }
        }

        // Thực hiện thông minh: Auto Tìm Quái & Đánh (Auto-Hunt) hoặc Tấn công Người chơi được ghim
        if (!isRemoteSellBusy() && isAutoAttack && GameCanvas.currentScreen == GameCanvas.game && GameScreen.player != null) {
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
            if (t > 0 && t < 120) {
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
            record.deathTime = 0L;
            record.preemptiveFired = false;
            // Đồng bộ 2 chiều: Nếu record đã nhận sát thương mới hơn từ server (record.hp < obj.hp)
            // thì cập nhật cho obj.hp của client, tuyệt đối không ghi đè tăng máu record lên!
            if (record.hp > 0 && obj.hp > record.hp) {
                obj.hp = record.hp;
            } else {
                record.hp = obj.hp;
            }
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

        if (timeReveice > 0 && timeReveice < 120) {
            record.respawnSeconds = timeReveice;
            record.respawnIntervalMs = (long)timeReveice * 1000L;
        }

        if (hp <= 0) {
            if (record.deathTime <= 0L) {
                record.deathTime = System.currentTimeMillis();
                record.preemptiveFired = false;
            }
            record.hp = 0;
        } else {
            // Quái có hp > 0
            // Nếu quái vừa mới chết (dưới 2.5s), gói monster_info này là gói cũ bay trễ từ server,
            // tuyệt đối không hồi sinh quái bằng gói trễ này!
            long now = System.currentTimeMillis();
            if (record.hp <= 0 && record.deathTime > 0L && (now - record.deathTime < 2500L)) {
                // Giữ nguyên trạng thái chết
            } else {
                if (record.hp <= 0 && record.deathTime > 0L) {
                    record.deathTime = 0L;
                    record.preemptiveFired = false;
                }
                // Nếu quái đang sống và máu từ monster_info lại CAO HƠN máu hiện tại
                // (gói monster_info bị delay bay về sau khi quái đã nhận sát thương):
                // Giữ nguyên máu đã bị trừ sát thương, không ghi đè tăng máu!
                if (record.hp > 0 && hp > record.hp && record.deathTime <= 0L) {
                    // Giữ nguyên record.hp
                } else {
                    record.hp = hp;
                }
            }
        }
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
            } else {
                record.deathTime = 0L;
                record.preemptiveFired = false;
            }
        }
        // Cập nhật tức thời cả đối tượng cục bộ trong tầm nhìn (nếu có)
        // đảm bảo thanh máu client và Tool đồng bộ tức thời tại cùng 1 mili-giây
        if (GameScreen.Vecplayers != null) {
            MainObject obj = MainObject.get_Object(id, (byte)1);
            if (obj != null) {
                obj.hp = Math.max(0, hp);
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
            if (rec.deathTime > 0L) {
                long measured = System.currentTimeMillis() - rec.deathTime;
                // Chỉ học thời gian hồi sinh nếu nằm trong khoảng hợp lý của game (3.5s - 30s)
                if (measured >= 3500L && measured <= 30000L) {
                    rec.respawnIntervalMs = measured;
                    rec.respawnSeconds = (int)((measured + 500L) / 1000L);
                }
            }
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

        // 1. Quét các quái đã chết: nếu đã đến hoặc quá thời gian hồi sinh thì truy vấn server
        // TUYỆT ĐỐI KHÔNG tự tiện set rec.hp = rec.maxHp hoặc reset rec.deathTime = 0L ở đây!
        // Chỉ khi server thực sự phản hồi quái hồi sinh thì mới cập nhật trạng thái sống!
        for (int i = 0; i < mapMonsterRecords.size(); ++i) {
            MonsterRecord rec = (MonsterRecord)mapMonsterRecords.elementAt(i);
            if (rec != null && rec.hp <= 0 && rec.deathTime > 0L) {
                long interval = rec.respawnIntervalMs > 0 ? rec.respawnIntervalMs : 5000L;
                if (now - rec.deathTime >= interval) {
                    GlobalService.gI().monster_info((short)rec.id);
                }
            }
        }

        // 2. Định kỳ round-robin gửi truy vấn monster_info cho toàn bộ quái trên map:
        // KHI ĐANG AUTO FARM: TẠM DỪNG round-robin cho quái đang sống để tránh làm nghẽn socket TCP
        // và ngăn race condition các gói tin monster_info trễ ghi đè làm sai lệch thanh máu quái!
        if (isFarmAttackActive()) return;

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

        // Chu kỳ tấn công giữa các đòn đánh liên tiếp: Định thời chuẩn xác theo hồi chiêu đánh thường của server (1s)
        // Nới giãn cách thêm 80ms (1080ms) làm khoảng đệm an toàn tuyệt đối chống ping biến thiên (jitter),
        // đảm bảo 100% khi gói tin tới server thì server ĐÃ HẾT 1.000ms hồi chiêu, triệt tiêu hoàn toàn việc rớt gói tin!
        long attackCooldown = Math.max(1080L, (long)getNormalAttackCooldownMs() + 80L);
        if (now - lastFarmAttemptTime < attackCooldown) return;

        int[] targets = getLiveFarmTargets(player, now);
        if (targets == null || targets.length == 0) return;

        // Triệt tiêu hoàn toàn việc chọn/khóa mục tiêu riêng vào 1 con quái
        // Giúp nhân vật không quay mặt hay tung animation vào riêng 1 con nào trước
        player.IDAttack = -1;
        player.KillFire = -1;
        GameScreen.ObjFocus = null;

        // GỬI CHÍNH XÁC ĐÚNG 1 HIT DUY NHẤT CHO MỖI QUÁI TRONG DANH SÁCH:
        // Tuyệt đối không gửi 2 vòng lặp liên tiếp trong cùng 1 tick mạng,
        // vì server HSO sẽ drop gói tin thứ 2 của hầu hết các con quái do cooldown đòn đánh,
        // dẫn đến việc chỉ có 1-2 con lọt qua bị trừ 2 hit (732) trong khi các con khác chỉ trừ 1 hit (1.2k)!
        // Khi gửi đúng 1 gói cho mỗi con: 100% quái cùng nhận 1 hit, máu cùng tụt đều 100% không lệch 1 giọt!
        for (int i = 0; i < targets.length; ++i) {
            mVector single = new mVector("Auto Farm Target Single");
            single.addElement(new Object_Effect_Skill((short)targets[i], (byte)1));
            GlobalService.gI().fire_monster(single, (byte)0);
        }

        lastFarmAttemptTime = now;
    }

    private static int[] getLiveFarmTargets(Player player, long now) {
        int[] idsToCheck = farmTargetIds;
        if (idsToCheck == null || idsToCheck.length == 0) return new int[0];

        int aliveCount = 0;
        int deadCount = 0;
        long latestDeadRespawnDue = 0L;
        boolean hasDamagedAlive = false; // Có quái sống đang bị thương (đang đánh dở)

        for (int i = 0; i < idsToCheck.length; ++i) {
            int mid = idsToCheck[i];
            if (isMonsterAlive(mid)) {
                ++aliveCount;
                MonsterRecord rec = findMonsterRecord(mid);
                if (rec != null && rec.maxHp > 0 && rec.hp < rec.maxHp) {
                    hasDamagedAlive = true;
                }
            } else {
                ++deadCount;
                MonsterRecord rec = findMonsterRecord(mid);
                if (rec != null && rec.deathTime > 0L) {
                    long interval = rec.respawnIntervalMs > 0 ? rec.respawnIntervalMs : 5000L;
                    if (interval <= 60000L) {
                        long due = rec.deathTime + interval;
                        if (due > latestDeadRespawnDue) {
                            latestDeadRespawnDue = due;
                        }
                    }
                }
            }
        }

        // CHẾ ĐỘ TỰ DO (farmSyncMode == false): Cứ con nào ra trước là HIT ngay lập tức, không đợi!
        if (!farmSyncMode) {
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
            return new int[0];
        }

        // CHẾ ĐỘ ĐỒNG BỘ (farmSyncMode == true): Đợi cả đàn hồi sinh để giữ nhịp chết và hồi sinh cùng 1 giây
        // TÌNH HUỐNG 1: 100% QUÁI ĐỀU ĐANG SỐNG (aliveCount == idsToCheck.length)
        // -> Trạng thái đồng bộ hoàn hảo: Tấn công toàn bộ 100% quái!
        if (aliveCount == idsToCheck.length) {
            return idsToCheck;
        }

        // TÌNH HUỐNG 2: 100% QUÁI ĐỀU ĐÃ CHẾT (aliveCount == 0)
        // -> Chờ đến khi con chết MUỘN NHẤT đã hồi sinh hoàn toàn trên server (+100ms đệm an toàn)
        if (aliveCount == 0) {
            long threshold = latestDeadRespawnDue > 0L ? latestDeadRespawnDue : (now + 5000L);
            if (now >= threshold + 100L) {
                // Đã hồi sinh xong trên server: trả về toàn bộ quái để tung đòn Hit 1 đồng loạt!
                return idsToCheck;
            }
            return new int[0]; // Chưa tới giờ -> Đợi (Hold Fire)
        }

        // TÌNH HUỐNG 3: ĐÀN QUÁI ĐANG BỊ LỆCH PHA (Vừa có quái sống, vừa có quái chết)
        // A. Nếu con chết muộn nhất cũng đã chạm mốc hồi sinh (+100ms đệm an toàn):
        //    -> Gom toàn bộ (cả quái sống lẫn quái vừa hồi sinh) vào đúng 1 nhịp đánh!
        if (latestDeadRespawnDue > 0L && now >= latestDeadRespawnDue + 100L) {
            return idsToCheck;
        }

        // B. Nếu có các quái sống ĐANG BỊ THƯƠNG (đang đánh dở từ đợt trước):
        //    -> Đánh tiếp các con sống đang bị thương này để tiêu diệt dứt điểm,
        //       đưa toàn bộ đàn về trạng thái CHẾT để chờ hội tụ chung 1 mốc hồi sinh!
        if (hasDamagedAlive) {
            int[] liveIds = new int[aliveCount];
            int idx = 0;
            for (int i = 0; i < idsToCheck.length; ++i) {
                if (isMonsterAlive(idsToCheck[i])) {
                    liveIds[idx++] = idsToCheck[i];
                }
            }
            return liveIds;
        }

        // C. Ngược lại: Các quái sống đều full máu (mới hồi sinh sớm) trong khi các quái khác vẫn đang chết:
        //    -> BẮT BUỘC HOLD FIRE (GIỮ NHỊP)! Tuyệt đối KHÔNG đánh lẻ tẻ quái vừa hồi sinh sớm,
        //       chờ cho các con chết còn lại hồi sinh đầy đủ (chạm mốc đón đầu) thì gom đánh chung 1 thể!
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
        String sync = extractJsonValue(cmd, "sync");
        String ids = extractJsonValue(cmd, "ids");
        if (sync != null) {
            farmSyncMode = !"0".equals(sync) && !"false".equalsIgnoreCase(sync);
        }
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

    public static boolean shouldSellItem(Item item) {
        if (item == null || item.setNameNull()) return false;
        // Bảo vệ an toàn: Không bán đồ có khóa bảo hiểm nếu bật unlocked_only
        if (sellUnlockedOnly && item.isLock > 0) return false;
        // Thêm: Kiểm tra tên đồ chứa "[Khoá]" hoặc "[Khóa]" → cũng coi là có khóa
        if (sellUnlockedOnly && item.itemName != null
                && (item.itemName.indexOf("[Kho") >= 0 || item.itemName.indexOf("[kho") >= 0)) {
            return false;
        }
        // Ghi chú: Không kiểm tra canSell == 0 vì server có thể gửi giá trị này
        // không chính xác khi nhặt đồ từ quái. Server sẽ tự reject nếu không thể bán.

        if (item.ItemCatagory == 3) {
            // Trang bị: Chỉ bán đồ +0 nếu bật tier0_only (bảo vệ đồ cường hóa)
            if (sellTier0Only && item.tier != 0) return false;
            switch (item.colorNameItem) {
                case 0: return sellWhite;
                case 1: return sellBlue;
                case 2: return sellYellow;
                case 3: return sellPurple;
                case 4: return sellOrange;
                case 5: return sellGreen;
                default: return false;
            }
        } else if (item.ItemCatagory == 7) {
            // Nguyên liệu
            return sellMaterial;
        }
        return false;
    }

    public static boolean hasItemInInventory(short id, int cat) {
        if (Item.VecInvetoryPlayer == null) return false;
        for (int i = 0; i < Item.VecInvetoryPlayer.size(); ++i) {
            Object obj = Item.VecInvetoryPlayer.elementAt(i);
            if (obj instanceof Item) {
                Item it = (Item)obj;
                if (it.Id == id && it.ItemCatagory == cat) {
                    return true;
                }
            }
        }
        return false;
    }

    public static boolean isShopNpc(String name, String nameGt) {
        if (name == null) return false;
        String n = name.trim().toLowerCase();
        if (n.equals("lisa") || n.equals("emma") || n.equals("doubar") || n.equals("hammer")) {
            return true;
        }
        if (nameGt != null) {
            String gt = nameGt.trim().toLowerCase();
            if (gt.indexOf("cửa hàng") != -1 || gt.indexOf("cua hang") != -1 || gt.indexOf("shop") != -1 || gt.indexOf("mua bán") != -1 || gt.indexOf("mua ban") != -1) {
                return true;
            }
        }
        return false;
    }

    public static MainObject findMapNpc() {
        if (GameScreen.Vecplayers == null) return null;
        for (int i = 0; i < GameScreen.Vecplayers.size(); ++i) {
            Object obj = GameScreen.Vecplayers.elementAt(i);
            if (obj instanceof NPC) {
                NPC npc = (NPC)obj;
                if (isShopNpc(npc.name, npc.nameGiaotiep)) {
                    return npc;
                }
            } else if (obj instanceof NPCserver) {
                NPCserver npc = (NPCserver)obj;
                if (isShopNpc(npc.name, npc.nameGiaotiep)) {
                    return npc;
                }
            }
        }
        return null;
    }

    public static void spoofMoveTo(int targetX, int targetY) {
        if (GameScreen.player == null) return;
        int curX = GameScreen.player.x;
        int curY = GameScreen.player.y;
        int dist = MainObject.getDistance(curX, curY, targetX, targetY);
        if (dist > 300) {
            int midX = (curX + targetX) / 2;
            int midY = (curY + targetY) / 2;
            GlobalService.gI().player_move((short)midX, (short)midY);
            try { Thread.sleep(15L); } catch (Exception ignored) {}
        }
        GlobalService.gI().player_move((short)targetX, (short)targetY);
        try { Thread.sleep(35L); } catch (Exception ignored) {}
    }

    public static void restoreVirtualMove(int startX, int startY) {
        if (GameScreen.player == null) return;
        GlobalService.gI().player_move((short)startX, (short)startY);
        GameScreen.player.x = startX;
        GameScreen.player.y = startY;
        GameScreen.player.toX = startX;
        GameScreen.player.toY = startY;
        GameScreen.player.xStand = (short)startX;
        GameScreen.player.yStand = (short)startY;
    }

    private static Item findInventoryItem(short id, int category) {
        if (Item.VecInvetoryPlayer == null) return null;
        for (int i = 0; i < Item.VecInvetoryPlayer.size(); ++i) {
            Object value = Item.VecInvetoryPlayer.elementAt(i);
            if (!(value instanceof Item)) continue;
            Item item = (Item)value;
            if (item.Id == id && item.ItemCatagory == category) return item;
        }
        return null;
    }

    private static boolean isLockedItem(Item item) {
        return item.isLock > 0 || (item.itemName != null
                && (item.itemName.indexOf("[Kho") >= 0 || item.itemName.indexOf("[kho") >= 0));
    }

    private static RemoteSellController.Target saleTarget(Item item, boolean explicit, boolean allowLocked) {
        return new RemoteSellController.Target((short)item.Id, (byte)item.ItemCatagory, item.itemName,
                item.ItemCatagory == 3 ? 1 : Math.max(1, item.numPotion), explicit, allowLocked);
    }

    private static RemoteSellController.Merchant remoteMerchant(int id, int x, int y, String name) {
        // Stand beside the NPC instead of inside its collision rectangle.
        int[][] offsets = {{0, 24}, {-24, 0}, {24, 0}, {0, -24}, {0, 48}, {-48, 0}, {48, 0}, {0, -48}};
        for (int i = 0; i < offsets.length; ++i) {
            int sx = x + offsets[i][0], sy = y + offsets[i][1];
            if (GameCanvas.loadmap != null && sx >= 0 && sy >= 0
                    && sx < GameCanvas.loadmap.mapW * LoadMap.wTile
                    && sy < GameCanvas.loadmap.mapH * LoadMap.wTile && isWalkable(sx, sy)) {
                return new RemoteSellController.Merchant(id, sx, sy, name);
            }
        }
        return new RemoteSellController.Merchant(id, x, y, name);
    }

    private static RemoteSellController.Merchant[] remoteMerchants() {
        java.util.ArrayList<RemoteSellController.Merchant> result = new java.util.ArrayList<RemoteSellController.Merchant>();
        if (GameScreen.Vecplayers != null) {
            for (int i = 0; i < GameScreen.Vecplayers.size(); ++i) {
                Object value = GameScreen.Vecplayers.elementAt(i);
                if (!(value instanceof MainObject)) continue;
                MainObject npc = (MainObject)value;
                String greeting = npc instanceof NPC ? ((NPC)npc).nameGiaotiep
                        : npc instanceof NPCserver ? ((NPCserver)npc).nameGiaotiep : null;
                if (!(npc instanceof NPC) && !(npc instanceof NPCserver) || !isShopNpc(npc.name, greeting)) continue;
                int interactionId = npc instanceof NPCserver ? ((NPCserver)npc).getIDnpc() : npc.ID;
                result.add(remoteMerchant(interactionId, npc.x, npc.y, npc.name));
            }
        }
        RemoteSellController.Merchant[] known = {
            remoteMerchant(-3, 756, 120, "Lisa"),
            remoteMerchant(-5, 180, 192, "Hammer"),
            remoteMerchant(-4, 204, 576, "Doubar")
        };
        for (int k = 0; k < known.length; ++k) {
            boolean found = false;
            for (int i = 0; i < result.size(); ++i) if (result.get(i).id == known[k].id) found = true;
            if (!found) result.add(known[k]);
        }
        return result.toArray(new RemoteSellController.Merchant[result.size()]);
    }

    private static boolean startRemoteSale(RemoteSellController.Target[] targets) {
        if (isRemoteSellBusy()) {
            logDebug("Remote Sell: giao dich dang chay, bo qua yeu cau trung.");
            return false;
        }
        if (isStrikeActive || lastRemoteMoveTime > 0) {
            remoteSell.reject("strike_busy", "Đang xử lý đòn đánh từ xa; hãy bán lại khi đòn đánh kết thúc.");
            return false;
        }
        boolean started = remoteSell.start(targets, remoteMerchants(), System.currentTimeMillis());
        if (started) {
            stopFarmAutoMovement();
            isAutoOpeningShop = true;
            lastAutoSellCheckTime = System.currentTimeMillis();
        } else {
            remoteSell.reject("not_ready", "Chưa kết nối vào game hoặc không có vật phẩm phù hợp để bán.");
        }
        return started;
    }

    public static boolean sellSingleItem(short id, int category) {
        return sellSingleItem(id, category, false);
    }

    public static synchronized boolean sellSingleItem(final short id, final int category, final boolean allowLocked) {
        if (Item.VecInvetoryPlayer == null) return false;
        Item item = findInventoryItem(id, category);
        if (item == null || item.setNameNull() || (category != 3 && category != 7)) {
            logDebug("sellSingleItem: Không tìm thấy vật phẩm id=" + id + " cat=" + category);
            return false;
        }
        if (isLockedItem(item) && !allowLocked) {
            logDebug("sellSingleItem: Vật phẩm có khóa, cần cho phép bán đồ khóa.");
            return false;
        }

        final Item itemToSell = item;
        new Thread(new Runnable() {
            public void run() {
                int startX = GameScreen.player != null ? GameScreen.player.x : 0;
                int startY = GameScreen.player != null ? GameScreen.player.y : 0;
                int npcId = -3;
                int npcX = 756;
                int npcY = 120;
                String npcName = "Lisa (Từ xa)";
                isSelling = true;
                isAutoOpeningShop = true;
                vibeSoldItems = 0;
                vibeTotalItems = 1;
                try {
                    String name = itemToSell.itemName != null ? itemToSell.itemName : ("id=" + id);
                    logDebug("Bán từng cái (Vị trí ảo): Di chuyển ảo tới " + npcName + " (id=" + npcId + ") tại (" + npcX + "," + npcY + ")...");
                    spoofMoveTo(npcX, npcY);
                    logDebug("Bán từng cái: Gửi getlist_from_npc id=" + npcId);
                    GlobalService.gI().getlist_from_npc((byte)npcId);
                    try { Thread.sleep(300L); } catch (InterruptedException ignored) {}

                    logDebug("Bán từng cái: Đang gửi lệnh bán '" + name + "' (id=" + id + ", cat=" + category + ")...");
                    GlobalService.gI().delete_Item((byte)category, id, (byte)1);

                    // Đợi server xác nhận xóa item khỏi hành trang (tối đa 2500ms)
                    long startWait = System.currentTimeMillis();
                    boolean removed = false;
                    while (System.currentTimeMillis() - startWait < 2500L) {
                        try { Thread.sleep(80L); } catch (InterruptedException ignored) {}
                        if (!hasItemInInventory(id, category)) {
                            removed = true;
                            break;
                        }
                    }

                    if (removed) {
                        vibeSoldItems = 1;
                        logDebug("Bán từng cái: Đã bán thành công '" + name + "'.");
                    } else {
                        logDebug("Bán từng cái: Server chưa phản hồi xóa '" + name + "' sau 2.5s.");
                    }
                } catch (Exception e) {
                    logDebug("Lỗi sellSingleItem: " + e.getMessage());
                } finally {
                    isSelling = false;
                    isAutoOpeningShop = false;
                    restoreVirtualMove(startX, startY);
                    try {
                        Main.GameCanvas.end_Dialog();
                        if (Main.GameCanvas.menu2 != null) Main.GameCanvas.menu2.doCloseMenu();
                    } catch (Exception ignored) {}
                    logDebug("Bán từng cái (Vị trí ảo): Đã hoàn trả tọa độ về (" + startX + "," + startY + ").");
                }
            }
        }, "HsoVibeSellSingle").start();
        return true;
    }

    public static void sellMatchingItems() {
        sellMatchingItems(false);
    }

    private static synchronized void sellMatchingItems(final boolean automatic) {
        if (isSelling) {
            logDebug("Auto Sell: Đang có tiến trình bán chạy ngầm, bỏ qua đợt mới.");
            return;
        }
        if (Item.VecInvetoryPlayer == null || Item.VecInvetoryPlayer.size() == 0) {
            logDebug("Auto Sell: Hành trang trống.");
            return;
        }

        isSelling = true;
        new Thread(new Runnable() {
            public void run() {
                int totalSold = 0;
                int startX = GameScreen.player != null ? GameScreen.player.x : 0;
                int startY = GameScreen.player != null ? GameScreen.player.y : 0;
                int npcId = -3;
                int npcX = 756;
                int npcY = 120;
                String npcName = "Lisa (Từ xa)";
                isAutoOpeningShop = true;
                vibeSoldItems = 0;
                vibeTotalItems = 0;
                try {
                    logDebug("Auto Sell (bán từng cái vị trí ảo): Bắt đầu quét và bán tuần tự...");
                    logDebug("Auto Sell: Di chuyển ảo tới " + npcName + " (id=" + npcId + ") tại (" + npcX + "," + npcY + ")...");
                    spoofMoveTo(npcX, npcY);
                    logDebug("Auto Sell: Gửi getlist_from_npc id=" + npcId);
                    GlobalService.gI().getlist_from_npc((byte)npcId);
                    try { Thread.sleep(300L); } catch (InterruptedException ignored) {}

                    while (true) {
                        // Mỗi lượt quét tìm 1 món đầu tiên thỏa điều kiện bán
                        Item target = null;
                        if (Item.VecInvetoryPlayer != null) {
                            for (int i = 0; i < Item.VecInvetoryPlayer.size(); ++i) {
                                Object obj = Item.VecInvetoryPlayer.elementAt(i);
                                if (obj instanceof Item) {
                                    Item it = (Item)obj;
                                    if (shouldSellItem(it)) {
                                        target = it;
                                        break;
                                    }
                                }
                            }
                        }

                        if (target == null) {
                            logDebug("Auto Sell: Không còn món nào thỏa điều kiện bán.");
                            break;
                        }

                        final short targetId = (short)target.Id;
                        final byte targetCat = (byte)target.ItemCatagory;
                        final String targetName = target.itemName != null ? target.itemName : ("Item#" + targetId);

                        logDebug("Auto Sell: Đang gửi lệnh bán lẻ '" + targetName + "' (id=" + targetId + ", cat=" + targetCat + ")...");
                        GlobalService.gI().delete_Item(targetCat, targetId, (byte)1);

                        // Chờ server xác nhận xóa item khỏi hành trang (tối đa 2500ms, kiểm tra mỗi 80ms)
                        long startWait = System.currentTimeMillis();
                        boolean removed = false;
                        while (System.currentTimeMillis() - startWait < 2500L) {
                            try { Thread.sleep(80L); } catch (InterruptedException ignored) {}
                            if (!hasItemInInventory(targetId, (int)targetCat)) {
                                removed = true;
                                break;
                            }
                        }

                        if (removed) {
                            totalSold++;
                            vibeSoldItems = totalSold;
                            logDebug("Auto Sell: Đã bán xong '" + targetName + "' (Tổng: " + totalSold + "). Nghỉ 150ms...");
                            try { Thread.sleep(150L); } catch (InterruptedException ignored) {}
                        } else {
                            logDebug("Auto Sell: Quá thời gian chờ server xác nhận bán '" + targetName + "', dừng chuỗi bán để an toàn.");
                            break;
                        }
                    }
                } catch (Exception e) {
                    logDebug("Lỗi Auto Sell: " + e.getMessage());
                } finally {
                    isSelling = false;
                    isAutoOpeningShop = false;
                    restoreVirtualMove(startX, startY);
                    try {
                        Main.GameCanvas.end_Dialog();
                        if (Main.GameCanvas.menu2 != null) Main.GameCanvas.menu2.doCloseMenu();
                    } catch (Exception ignored) {}
                    logDebug("Auto Sell: Hoàn tất đợt bán. Tổng số món đã bán: " + totalSold + ". Đã trả tọa độ về (" + startX + "," + startY + ").");
                }
            }
        }, "HsoVibeSellMatching").start();
    }

    public static boolean isRemoteSellBusy() { return remoteSell.isActive() || isSelling; }
    public static boolean isRemoteSellPositionGuarded() {
        return remoteSell.guardPosition(System.currentTimeMillis());
    }
    public static void noteRemoteSellInventory() { ++remoteSellInventoryRevision; }
    public static boolean onRemoteSellMenu(int npcId, int menuId, int option) {
        return remoteSell.onMenu(npcId, menuId, option);
    }

    public static int remoteShopOption(int npcId, int clazz, String[] options) {
        int classOption = -1;
        String[] classes = {"chiến binh", "sát thủ", "pháp sư", "xạ thủ"};
        for (int i = 0; i < options.length; ++i) {
            String label = options[i].trim().toLowerCase(java.util.Locale.ROOT);
            if (label.equals("mua bán") || label.equals("mua ban") || label.equals("cửa hàng")
                    || label.equals("cua hang") || label.equals("shop")) return i;
            // Hammer/Doubar open equipment shops through the character-class menu.
            if ((npcId == -5 || npcId == -4) && clazz >= 0 && clazz < classes.length
                    && label.equals(classes[clazz])) classOption = i;
        }
        return classOption;
    }
    public static boolean onRemoteSellShop(int type) { return remoteSell.onShop(type); }
    public static boolean onRemoteSellDialog(int id, int type, String text) {
        return remoteSell.onDialog(id, type, text);
    }
    public static void noteRemoteSellNotice(String text) { remoteSell.onNotice(text); }

    private static String remoteSellJson() {
        if (isSelling) {
            return "{\"active\":true,\"phase\":\"vibe_selling\",\"message\":\"Đang bán trang bị rác từ xa...\",\"error\":\"\",\"sold_items\":" + vibeSoldItems + ",\"sold_units\":" + vibeSoldItems + ",\"total_items\":" + vibeTotalItems + "}";
        }
        synchronized (remoteSell) {
            return "{\"active\":" + remoteSell.isActive()
                    + ",\"phase\":\"" + escape(remoteSell.phase())
                    + "\",\"message\":\"" + escape(remoteSell.message())
                    + "\",\"error\":\"" + escape(remoteSell.error())
                    + "\",\"sold_items\":" + remoteSell.soldItems()
                    + ",\"sold_units\":" + remoteSell.soldUnits()
                    + ",\"total_items\":" + remoteSell.totalItems() + "}";
        }
    }

    public static String inventoryJson() {
        StringBuffer sb = new StringBuffer();
        sb.append("[");
        if (Item.VecInvetoryPlayer != null) {
            int count = 0;
            for (int i = 0; i < Item.VecInvetoryPlayer.size(); ++i) {
                Object obj = Item.VecInvetoryPlayer.elementAt(i);
                if (obj instanceof Item) {
                    Item it = (Item)obj;
                    if (it != null && !it.setNameNull()) {
                        if (count > 0) sb.append(",");
                        boolean isLocked = (it.isLock > 0) || (it.itemName != null && (it.itemName.indexOf("[Kho") >= 0 || it.itemName.indexOf("[kho") >= 0));
                        sb.append("{\"id\":").append(it.Id)
                          .append(",\"name\":\"").append(escape(it.itemName != null ? it.itemName : ""))
                          .append("\",\"cat\":").append(it.ItemCatagory)
                          .append(",\"color\":").append(it.colorNameItem)
                          .append(",\"tier\":").append(it.tier)
                          .append(",\"lock\":").append(isLocked ? 1 : 0)
                          .append(",\"level\":").append(it.LvItem)
                          .append(",\"can_sell\":").append(it.canSell)
                          .append(",\"num\":").append(it.numPotion)
                          .append(",\"price\":").append(it.priceItem)
                          .append("}");
                        count++;
                    }
                }
            }
        }
        sb.append("]");
        return sb.toString();
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
        int keyStart = json.indexOf("\"" + key + "\"");
        if (keyStart < 0) return null;
        int idx = json.indexOf(':', keyStart + key.length() + 2);
        if (idx < 0) return null;
        ++idx;
        while (idx < json.length() && Character.isWhitespace(json.charAt(idx))) ++idx;
        if (idx >= json.length()) return null;
        if (json.charAt(idx) == '"') {
            StringBuilder value = new StringBuilder();
            for (++idx; idx < json.length(); ++idx) {
                char c = json.charAt(idx);
                if (c == '"') return value.toString();
                if (c != '\\') { value.append(c); continue; }
                if (++idx >= json.length()) return null;
                c = json.charAt(idx);
                switch (c) {
                    case 'u':
                        if (idx + 4 >= json.length()) return null;
                        try { value.append((char)Integer.parseInt(json.substring(idx + 1, idx + 5), 16)); }
                        catch (NumberFormatException ex) { return null; }
                        idx += 4;
                        break;
                    case 'n': value.append('\n'); break;
                    case 'r': value.append('\r'); break;
                    case 't': value.append('\t'); break;
                    case 'b': value.append('\b'); break;
                    case 'f': value.append('\f'); break;
                    case '"': case '\\': case '/': value.append(c); break;
                    default: return null;
                }
            }
            return null;
        }
        int end = idx;
        while (end < json.length() && json.charAt(end) != ',' && json.charAt(end) != '}') ++end;
        return json.substring(idx, end).trim();
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

    public static void configureSellFilter(String cmd) {
        if (cmd == null) return;
        if (cmd.indexOf("\"white\":") != -1) sellWhite = isJsonBoolTrue(cmd, "white");
        if (cmd.indexOf("\"blue\":") != -1) sellBlue = isJsonBoolTrue(cmd, "blue");
        if (cmd.indexOf("\"yellow\":") != -1) sellYellow = isJsonBoolTrue(cmd, "yellow");
        if (cmd.indexOf("\"purple\":") != -1) sellPurple = isJsonBoolTrue(cmd, "purple");
        if (cmd.indexOf("\"orange\":") != -1) sellOrange = isJsonBoolTrue(cmd, "orange");
        if (cmd.indexOf("\"green\":") != -1) sellGreen = isJsonBoolTrue(cmd, "green");
        if (cmd.indexOf("\"material\":") != -1) sellMaterial = isJsonBoolTrue(cmd, "material");
        if (cmd.indexOf("\"tier0_only\":") != -1) sellTier0Only = isJsonBoolTrue(cmd, "tier0_only");
        if (cmd.indexOf("\"unlocked_only\":") != -1) sellUnlockedOnly = isJsonBoolTrue(cmd, "unlocked_only");
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

            if (cmd.indexOf("\"farm_sync\"") != -1) {
                String sync = extractJsonValue(cmd, "enabled");
                if (sync == null) sync = extractJsonValue(cmd, "sync");
                if (sync != null) {
                    farmSyncMode = !"0".equals(sync) && !"false".equalsIgnoreCase(sync);
                    logDebug("Auto Fam sync mode updated: " + farmSyncMode);
                }
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

            // Lệnh auto bán đồ
            if (cmd.indexOf("\"auto_sell\"") != -1 || cmd.indexOf("\"set_auto_sell\"") != -1) {
                if (cmd.indexOf("\"enabled\": true") != -1 || cmd.indexOf("\"enabled\":true") != -1 || cmd.indexOf("\"enabled\": 1") != -1) {
                    isAutoSell = true;
                } else if (cmd.indexOf("\"enabled\": false") != -1 || cmd.indexOf("\"enabled\":false") != -1 || cmd.indexOf("\"enabled\": 0") != -1) {
                    isAutoSell = false;
                }
                configureSellFilter(cmd);
                logDebug("auto_sell command: enabled=" + isAutoSell);
            }

            // Lệnh cấu hình bộ lọc bán đồ
            if (cmd.indexOf("\"sell_filter\"") != -1) {
                configureSellFilter(cmd);
                logDebug("sell_filter command updated");
            }

            // Lệnh bán đồ ngay lập tức (tuần tự từng món)
            if (cmd.indexOf("\"sell_now\"") != -1 || cmd.indexOf("\"sell_items\"") != -1) {
                configureSellFilter(cmd);
                sellMatchingItems();
                logDebug("sell_now command triggered");
            }

            // Lệnh bán đích danh 1 món đồ lẻ (bán từng cái)
            if (cmd.indexOf("\"sell_single_item\"") != -1) {
                String subId = extractJsonValue(cmd, "item_id");
                if (subId == null) subId = extractJsonValue(cmd, "id");
                String subCat = extractJsonValue(cmd, "cat");
                if (subId != null) {
                    try {
                        short itId = Short.parseShort(subId);
                        int itCat = (subCat != null) ? Integer.parseInt(subCat) : 3;
                        String expectedName = extractJsonValue(cmd, "item_name");
                        Item currentItem = findInventoryItem(itId, itCat);
                        if (expectedName != null && expectedName.length() > 0
                                && currentItem != null && !expectedName.equals(currentItem.itemName)) {
                            remoteSell.reject("item_changed", "Danh sách đồ đã thay đổi; hãy chọn lại món cần bán.");
                        } else {
                            sellSingleItem(itId, itCat, isJsonBoolTrue(cmd, "allow_locked"));
                        }
                        logDebug("sell_single_item command triggered: id=" + itId + " cat=" + itCat);
                    } catch (Exception ex) {
                        logDebug("Lỗi parse sell_single_item: " + ex.getMessage());
                    }
                }
            }

            // Lệnh kiểm tra gói tin NPC
            if (cmd.indexOf("\"cancel_sell\"") != -1) {
                isAutoSell = false;
                remoteSell.cancel(System.currentTimeMillis());
            }

            if (cmd.indexOf("\"test_npc_packet\"") != -1) {
                String subNpc = extractJsonValue(cmd, "npc_id");
                String subX = extractJsonValue(cmd, "x");
                String subY = extractJsonValue(cmd, "y");
                byte targetNpcId = (subNpc != null) ? Byte.parseByte(subNpc) : (byte)-90;
                int tx = (subX != null) ? Integer.parseInt(subX) : 0;
                int ty = (subY != null) ? Integer.parseInt(subY) : 0;
                logDebug("TEST NPC PACKET: Gửi getlist_from_npc(" + targetNpcId + ") tại tọa độ (" + tx + "," + ty + ")");
                int sx = GameScreen.player != null ? GameScreen.player.x : 0;
                int sy = GameScreen.player != null ? GameScreen.player.y : 0;
                if (targetNpcId == GrahamNpcMenu.NPC_ID && tx == 0 && ty == 0) {
                    if (GrahamNpcMenu.show()) GlobalService.gI().remoteSellMenu(targetNpcId);
                    return;
                }
                if (tx > 0 && ty > 0) {
                    spoofMoveTo(tx, ty);
                }
                GlobalService.gI().getlist_from_npc(targetNpcId);
                try { Thread.sleep(200L); } catch (Exception ignored) {}
                if (tx > 0 && ty > 0) {
                    restoreVirtualMove(sx, sy);
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
                sb.append("\"typePk\":").append(GameScreen.player.typePk).append(",");
                sb.append("\"coin\":").append(GameScreen.player.coin).append(",");
                sb.append("\"gold\":").append(GameScreen.player.gold).append(",");
                sb.append("\"inven_count\":").append(Item.VecInvetoryPlayer != null ? Item.VecInvetoryPlayer.size() : 0).append(",");
                sb.append("\"inven_max\":").append(Player.maxInven);
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
                sb.append("\"inventory\":").append(inventoryJson()).append(",");
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
                sb.append("\"inventory\":[],");
                sb.append("\"monsters\":[],\"map_monsters\":[],");
                sb.append("\"monster_scan_active\":false,\"monster_scan_done\":false,");
                sb.append("\"monster_scan_progress\":0,\"monster_scan_limit\":")
                  .append(monsterScanLimit);
            }

            sb.append(",\"auto_attack\":").append(isAutoAttack);
            sb.append(",\"farm_mode\":\"")
              .append(isFarmAttackActive() ? "normal" : "off")
              .append("\"");
            sb.append(",\"farm_sync\":").append(farmSyncMode);
            sb.append(",\"farm_packets_sent\":").append(farmPacketsSent);
            sb.append(",\"farm_damage_results\":").append(farmDamageResults);
            sb.append(",\"farm_distinct_hits\":").append(farmDamagedIds.size());
            sb.append(",\"auto_pickup\":").append(isAutoPickup);
            sb.append(",\"auto_sell\":").append(isAutoSell);
            sb.append(",\"remote_sell\":").append(remoteSellJson());
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
        return s.replace("\\", "\\\\").replace("\"", "\\\"")
                .replace("\t", "\\t").replace("\n", " ").replace("\r", "");
    }
}
