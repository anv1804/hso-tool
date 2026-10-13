package com.silverknight;

/** One remote transaction at a time. Only server inventory changes acknowledge a sale. */
public final class RemoteSellController {
    public interface Transport {
        boolean connected();
        int playerId();
        int mapId();
        int area();
        int x();
        int y();
        boolean alive();
        int quantity(Target item);
        boolean allowed(Target item);
        long inventoryRevision();
        boolean safeVirtualPosition(int x, int y);
        void move(int x, int y);
        void menu(int npcId);
        void selectMenu(int npcId, int menuId, int option);
        void sell(Target item);
        void confirm(int dialogId, int dialogType);
        void log(String message);
    }

    public static final class Target {
        public final short id;
        public final byte category;
        public final String name;
        public final boolean explicit;
        public final boolean allowLocked;
        int remaining;
        boolean counted;

        public Target(short id, byte category, String name, int quantity,
                      boolean explicit, boolean allowLocked) {
            this.id = id;
            this.category = category;
            this.name = name;
            this.remaining = quantity;
            this.explicit = explicit;
            this.allowLocked = allowLocked;
        }
    }

    public static final class Merchant {
        public final int id, x, y;
        public final String name;
        public Merchant(int id, int x, int y, String name) {
            this.id = id;
            this.x = x;
            this.y = y;
            this.name = name;
        }
    }

    private static final long RESULT_WAIT_MS = 4000L;
    private static final long MENU_WAIT_MS = 10000L;
    private static final long SHOP_WAIT_MS = 6500L;
    private final Transport io;
    private Target[] targets = new Target[0];
    private Merchant[] merchants = new Merchant[0];
    private int playerId, mapId, area, startX, startY, virtualX, virtualY;
    private int targetIndex, merchantIndex, baselineQuantity, soldItems, soldUnits;
    private long started, deadline, nextMove, nextKeepAlive, baselineRevision, guardUntil, restoreDeadline;
    private boolean active, positionUsed, pendingSale, shopReady, remoteTried;
    private int menuId = -1, menuOption = -1, dialogId = -1, dialogType;
    private int confirmedDialogId = -1;
    private boolean[] requestedMerchants = new boolean[0];
    private boolean shopRequested;
    private String phase = "idle", error = "", serverNotice = "";
    private String message = "Chưa có giao dịch bán đồ.";
    private String finalPhase;

    public RemoteSellController(Transport io) { this.io = io; }

    public synchronized boolean start(Target[] items, Merchant[] npcs, long now) {
        if (active) return false;
        if (!io.connected() || !io.alive() || items == null || items.length == 0) return false;
        targets = items;
        merchants = npcs == null ? new Merchant[0] : npcs;
        playerId = io.playerId();
        mapId = io.mapId();
        area = io.area();
        startX = virtualX = io.x();
        startY = virtualY = io.y();
        targetIndex = merchantIndex = soldItems = soldUnits = 0;
        started = now;
        deadline = now;
        guardUntil = 0;
        error = serverNotice = "";
        pendingSale = positionUsed = shopReady = remoteTried = false;
        menuId = menuOption = dialogId = confirmedDialogId = -1;
        requestedMerchants = new boolean[merchants.length];
        shopRequested = false;
        active = true;
        setPhase("send", "Đang thử bán trực tiếp tại bãi treo.");
        return true;
    }

    public synchronized boolean isActive() { return active; }
    public synchronized void reject(String code, String text) {
        if (active) return;
        error = code;
        setPhase("failed", text);
    }
    public synchronized boolean guardPosition(long now) {
        return sameContext() && (active || now < guardUntil);
    }
    public synchronized String phase() { return phase; }
    public synchronized String message() { return message; }
    public synchronized String error() { return error; }
    public synchronized int soldItems() { return soldItems; }
    public synchronized int soldUnits() { return soldUnits; }
    public synchronized int totalItems() { return targets.length; }

    private boolean sameContext() {
        return io.connected() && io.playerId() == playerId && io.mapId() == mapId && io.area() == area;
    }

    private void setPhase(String value, String text) {
        phase = value;
        message = text;
        io.log("Remote Sell [" + value + "]: " + text);
    }

    public synchronized boolean onMenu(int npcId, int id, int option) {
        if (!active || !sameContext()) return false;
        boolean requested = false;
        for (int i = 0; i < merchants.length; ++i)
            if (requestedMerchants[i] && merchants[i].id == npcId) requested = true;
        if (!requested) return false;
        if (merchantIndex >= merchants.length || merchants[merchantIndex].id != npcId
                || !"wait_menu".equals(phase)) return true; // Consume our late replies, without acting on them.
        menuId = id;
        menuOption = option; // -1 is a real menu with no buying/selling option.
        return true;
    }

    public synchronized boolean onShop(int type) {
        // These types describe NPC shops, never a chest, salon or another player's store.
        if (!active || !sameContext() || !shopRequested
                || (type != 0 && type != 1 && type != 4 && type != 100)) return false;
        if ("wait_shop".equals(phase)) shopReady = true;
        return true;
    }

    public synchronized boolean onDialog(int id, int type, String text) {
        if (!active || !sameContext() || !pendingSale || targetIndex >= targets.length || text == null)
            return false;
        String lower = text.toLowerCase(java.util.Locale.ROOT);
        String name = targets[targetIndex].name.toLowerCase(java.util.Locale.ROOT)
                .replace("[khoá]", "").replace("[khóa]", "").trim();
        if (name.length() == 0 || (lower.indexOf("bạn có muốn bán") < 0
                && lower.indexOf("ban co muon ban") < 0) || lower.indexOf(name) < 0) return false;
        if (targets[targetIndex].allowLocked || !isLockName(targets[targetIndex].name)) {
            if (id != confirmedDialogId) {
                dialogId = id;
                dialogType = type;
            }
            return true;
        }
        return false;
    }

    private static boolean isLockName(String name) {
        return name.indexOf("[Kho") >= 0 || name.indexOf("[kho") >= 0;
    }

    public synchronized void onNotice(String text) {
        if (!active || text == null) return;
        String lower = text.toLowerCase(java.util.Locale.ROOT);
        if (lower.indexOf("bán") >= 0 || lower.indexOf("mua bán") >= 0
                || lower.indexOf("cửa hàng") >= 0 || lower.indexOf("khoảng cách") >= 0
                || lower.indexOf("quá xa") >= 0) serverNotice = text;
    }

    public synchronized void cancel(long now) {
        if (active) finish("cancelled", "cancelled", "Đã dừng bán đồ theo yêu cầu.", now);
    }

    public synchronized void tick(long now) {
        if (!active) return;
        try {
            if (!sameContext()) {
                // Never restore coordinates into a different map, zone or login session.
                active = false;
                guardUntil = 0;
                pendingSale = false;
                error = "context_changed";
                setPhase("failed", "Dừng bán: đã mất kết nối hoặc đổi nhân vật/map/khu.");
                return;
            }
            if (!"restore".equals(phase) && (!io.alive() || io.x() != startX || io.y() != startY)) {
                finish("cancelled", "player_changed", "Dừng bán: nhân vật di chuyển hoặc đã chết.", now);
            }
            if (!"restore".equals(phase) && now - started > 120000L) {
                finish("failed", "session_timeout", "Hết thời gian giao dịch bán đồ.", now);
            }

            // A late response to the first direct sale can arrive during shop negotiation.
            // Consume it before considering a retry, so the item is not sold twice.
            if (pendingSale && acknowledgeSale(now)) return;
            if (dialogId >= 0 && pendingSale) {
                io.confirm(dialogId, dialogType);
                confirmedDialogId = dialogId;
                dialogId = -1;
                deadline = Math.max(deadline, now + RESULT_WAIT_MS);
            }

            if ("restore".equals(phase)) {
                // Restore only the server position. The local player/camera were never moved.
                int restoreX = io.x(), restoreY = io.y();
                if (now >= restoreDeadline) {
                    io.move(restoreX, restoreY);
                    virtualX = restoreX;
                    virtualY = restoreY;
                }
                if (now >= nextMove) {
                    if (stepTo(restoreX, restoreY, now, true)) {
                        active = false;
                        pendingSale = false;
                        guardUntil = now + 1200L;
                        phase = finalPhase;
                        io.log("Remote Sell [" + phase + "]: " + message);
                    }
                }
                return;
            }
            if (positionUsed && !"move".equals(phase) && now >= nextKeepAlive) {
                io.move(virtualX, virtualY);
                nextKeepAlive = now + 750L;
            }

            if ("send".equals(phase)) {
                if (now < deadline) return;
                while (targetIndex < targets.length) {
                    Target target = targets[targetIndex];
                    int count = io.quantity(target);
                    if (count < 0) {
                        finish("failed", "item_changed", "Vật phẩm đã thay đổi; dừng để tránh bán nhầm.", now);
                        return;
                    }
                    if (count == 0 || target.remaining <= 0 || !io.allowed(target)) {
                        ++targetIndex;
                        continue;
                    }
                    baselineQuantity = count;
                    baselineRevision = io.inventoryRevision();
                    pendingSale = true;
                    dialogId = confirmedDialogId = -1;
                    io.sell(target);
                    deadline = now + RESULT_WAIT_MS;
                    setPhase("wait_result", "Chờ server xác nhận bán " + target.name + ".");
                    return;
                }
                finish("done", "", "Đã bán " + soldItems + " món (" + soldUnits + " đơn vị), server đã xác nhận.", now);
            } else if ("wait_result".equals(phase) && now >= deadline) {
                if (!remoteTried && !shopReady && merchants.length > 0) {
                    remoteTried = true;
                    beginMerchant(now);
                } else {
                    finish("failed", "sale_not_confirmed", "Server chưa xác nhận bán đồ; không gửi lặp vô hạn." + notice(), now);
                }
            } else if ("move".equals(phase) && now >= nextMove) {
                Merchant npc = merchants[merchantIndex];
                if (!io.safeVirtualPosition(npc.x, npc.y)) {
                    nextMerchant(now, "Tọa độ NPC không dùng được trên map hiện tại.");
                } else if (stepTo(npc.x, npc.y, now, false)) {
                    menuId = menuOption = -1;
                    requestedMerchants[merchantIndex] = true;
                    io.menu(npc.id);
                    deadline = now + MENU_WAIT_MS;
                    setPhase("wait_menu", "Đang gọi menu " + npc.name + " bằng vị trí ảo.");
                }
            } else if ("wait_menu".equals(phase)) {
                if (menuId >= 0) {
                    if (menuOption < 0) {
                        nextMerchant(now, "NPC không có lựa chọn mua bán.");
                    } else {
                        Merchant npc = merchants[merchantIndex];
                        shopRequested = true;
                        io.selectMenu(npc.id, menuId, menuOption);
                        deadline = now + SHOP_WAIT_MS;
                        setPhase("wait_shop", "Chờ server mở cửa hàng " + npc.name + ".");
                    }
                } else if (now >= deadline) nextMerchant(now, "Server không trả menu NPC.");
            } else if ("wait_shop".equals(phase)) {
                if (shopReady) {
                    // Retry once, only after a real shop reply. Check inventory again on the next tick.
                    pendingSale = false;
                    deadline = now + 200L;
                    setPhase("send", "Server đã mở shop; đang bán lần lượt.");
                } else if (now >= deadline) nextMerchant(now, "Có menu nhưng server không mở shop.");
            }
        } catch (Exception ex) {
            io.log("Remote Sell exception: " + ex);
            active = false;
            pendingSale = false;
            error = "transport_error";
            // Best effort using a self-contained packet; no local coordinate assignment.
            try { if (positionUsed && sameContext()) io.move(io.x(), io.y()); } catch (Exception ignored) {}
            guardUntil = now + 1200L;
            setPhase("failed", "Lỗi gửi gói tin bán đồ; đã yêu cầu hoàn trả vị trí server.");
        }
    }

    private boolean acknowledgeSale(long now) {
        Target target = targets[targetIndex];
        int count = io.quantity(target);
        if (count < 0) {
            finish("failed", "item_changed", "ID vật phẩm đã đổi nội dung; dừng bán.", now);
            return true;
        }
        if (io.inventoryRevision() <= baselineRevision || count >= baselineQuantity) return false;
        int units = baselineQuantity - count;
        soldUnits += units;
        if (!target.counted) {
            ++soldItems;
            target.counted = true;
        }
        target.remaining -= units;
        pendingSale = false;
        dialogId = -1;
        deadline = now + 250L;
        setPhase("send", "Server xác nhận bán " + target.name + " (" + units + " đơn vị).");
        return true;
    }

    private void beginMerchant(long now) {
        menuId = menuOption = -1;
        nextMove = now;
        setPhase("move", "Thử mở shop " + merchants[merchantIndex].name + " từ xa; giữ nguyên bãi treo.");
    }

    private void nextMerchant(long now, String reason) {
        io.log("Remote Sell merchant=" + merchants[merchantIndex].id + ": " + reason);
        if (++merchantIndex < merchants.length) beginMerchant(now);
        else finish("failed", "remote_shop_unavailable",
                "Chưa nhận xác nhận bán hoặc gói mở shop từ xa trên map #" + mapId
                + ". Đã thử vị trí ảo; giữ nguyên bãi treo." + notice(), now);
    }

    private String notice() { return serverNotice.length() == 0 ? "" : " Phản hồi: " + serverNotice; }

    private boolean stepTo(int x, int y, long now, boolean returning) {
        int dx = x - virtualX, dy = y - virtualY;
        double distance = Math.sqrt((double)dx * dx + (double)dy * dy);
        int nextX = distance <= 36 ? x : virtualX + (int)Math.round(dx * 36.0 / distance);
        int nextY = distance <= 36 ? y : virtualY + (int)Math.round(dy * 36.0 / distance);
        if (!returning && !io.safeVirtualPosition(nextX, nextY)) {
            nextMerchant(now, "Đường vị trí ảo chạm cổng chuyển map hoặc vượt biên; đã bỏ qua.");
            return false;
        }
        if (returning && !io.safeVirtualPosition(nextX, nextY)) {
            // The real local position is the only valid restoration endpoint.
            nextX = x;
            nextY = y;
        }
        positionUsed = true;
        io.move(nextX, nextY);
        virtualX = nextX;
        virtualY = nextY;
        nextMove = now + 40L;
        nextKeepAlive = now + 750L;
        return nextX == x && nextY == y;
    }

    private void finish(String result, String code, String text, long now) {
        pendingSale = false;
        dialogId = -1;
        error = code;
        if (positionUsed && sameContext()) {
            finalPhase = result;
            nextMove = now;
            restoreDeadline = now + 8000L;
            setPhase("restore", text);
        } else {
            active = false;
            guardUntil = 0;
            setPhase(result, text);
        }
    }
}
