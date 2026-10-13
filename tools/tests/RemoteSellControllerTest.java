import com.silverknight.RemoteSellController;
import com.silverknight.RemoteSellController.Target;
import com.silverknight.RemoteSellController.Merchant;
import java.util.ArrayList;

public class RemoteSellControllerTest {
    private static int checks;
    private static void check(boolean ok, String why) {
        ++checks;
        if (!ok) throw new AssertionError(why);
    }

    private static class Fake implements RemoteSellController.Transport {
        boolean connected = true, alive = true, allowed = true, safe = true, failSale;
        int player = 42, map = 7, area = 3, x = 0, y = 0, quantity = 1;
        long revision;
        int sales, menus, selections, confirmations, selectedNpc, selectedMenu, selectedOption;
        ArrayList<int[]> moves = new ArrayList<int[]>();
        ArrayList<Short> saleIds = new ArrayList<Short>();
        java.util.HashMap<Short, Integer> stock = new java.util.HashMap<Short, Integer>();
        public boolean connected() { return connected; }
        public int playerId() { return player; }
        public int mapId() { return map; }
        public int area() { return area; }
        public int x() { return x; }
        public int y() { return y; }
        public boolean alive() { return alive; }
        public int quantity(Target item) { return stock.containsKey(item.id) ? stock.get(item.id) : quantity; }
        public boolean allowed(Target item) { return allowed; }
        public long inventoryRevision() { return revision; }
        public boolean safeVirtualPosition(int x, int y) { return safe; }
        public void move(int x, int y) { moves.add(new int[] {x, y}); }
        public void menu(int id) { ++menus; }
        public void selectMenu(int id, int menuId, int option) {
            ++selections; selectedNpc = id; selectedMenu = menuId; selectedOption = option;
        }
        public void sell(Target item) {
            if (failSale) throw new IllegalStateException("simulated send failure");
            ++sales;
            saleIds.add(item.id);
        }
        public void confirm(int id, int type) { ++confirmations; }
        public void log(String text) {}
        void acknowledge(int count) { quantity = count; ++revision; }
    }

    private static Target target(int quantity) {
        return new Target((short)27, (byte)3, "Vòng bình an [Khoá]", quantity, true, true);
    }
    private static RemoteSellController create(Fake io, int count) {
        RemoteSellController c = new RemoteSellController(io);
        check(c.start(new Target[] {target(count)},
                new Merchant[] {new Merchant(-3, 36, 0, "Lisa")}, 0), "start sale");
        c.tick(0);
        return c;
    }
    private static void reachMenu(RemoteSellController c) {
        c.tick(4001); // direct attempt timed out; enter virtual movement
        c.tick(4041); // one 36px virtual step, then request menu
        check("wait_menu".equals(c.phase()), "wait for menu");
    }
    private static void reachShop(RemoteSellController c) {
        reachMenu(c);
        check(!c.onMenu(-90, 0, 0), "do not choose an unrelated NPC menu");
        check(c.onMenu(-3, 7, 2), "accept actual merchant menu index");
        c.tick(4081);
        check("wait_shop".equals(c.phase()), "wait for real shop reply");
    }
    private static void drain(RemoteSellController c, long from) {
        for (long now = from; now < from + 12000 && c.isActive(); now += 40) c.tick(now);
        check(!c.isActive(), "transaction terminates");
    }

    private static void directSale() {
        Fake io = new Fake();
        RemoteSellController c = create(io, 1);
        check(io.sales == 1 && io.menus == 0 && io.moves.isEmpty(), "direct sale stays in place");
        io.quantity = 0;
        c.tick(100);
        check(c.soldItems() == 0, "a local inventory change alone is not server confirmation");
        ++io.revision;
        c.tick(140);
        drain(c, 400);
        check("done".equals(c.phase()) && c.soldItems() == 1, "direct acknowledgement succeeds");
        check(io.moves.isEmpty(), "direct mode does not even send virtual movement");
    }

    private static void realShopRequired() {
        Fake io = new Fake();
        RemoteSellController c = create(io, 1);
        check(!c.onShop(0), "a shop packet before negotiation cannot unlock a retry");
        reachShop(c);
        check(io.selections == 1 && io.selectedNpc == -3 && io.selectedMenu == 7
                && io.selectedOption == 2, "send the returned menu ID and option, not hardcoded 0");
        check(!c.onShop(3) && !c.onShop(2) && !c.onShop(17) && !c.onShop(9), "reject chest/hair/player store/rebuild");
        c.tick(4200);
        check(io.sales == 1, "do not sell just because a menu appeared");
        check(c.onShop(4), "material merchant is also an NPC shop");
        c.tick(4240);
        c.tick(4480);
        check(io.sales == 2, "retry after shop acknowledgement");
        io.acknowledge(0);
        c.tick(4520);
        drain(c, 4800);
        check("done".equals(c.phase()) && c.soldUnits() == 1, "remote shop sale confirmed");
        int[] restored = io.moves.get(io.moves.size() - 1);
        check(restored[0] == 0 && restored[1] == 0 && io.x == 0 && io.y == 0 && io.map == 7,
                "restore server position, never local coordinates or map");
        check(c.guardPosition(4900), "guard late position echoes after restore, including coordinate zero");
    }

    private static void lateDirectReply() {
        Fake io = new Fake();
        RemoteSellController c = create(io, 1);
        reachMenu(c);
        io.acknowledge(0);
        c.tick(4100);
        check(c.onMenu(-3, 7, 2), "consume our late menu without showing a modal");
        check(io.selections == 0, "late reply does not select a shop");
        drain(c, 4400);
        check(io.sales == 1 && c.soldItems() == 1, "late direct reply does not duplicate the sale");
    }

    private static void partialStack() {
        Fake io = new Fake(); io.quantity = 3;
        RemoteSellController c = create(io, 3);
        io.acknowledge(2); c.tick(100); c.tick(400);
        io.acknowledge(1); c.tick(500); c.tick(800);
        io.acknowledge(0); c.tick(900); drain(c, 1200);
        check(io.sales == 3 && c.soldUnits() == 3 && c.soldItems() == 1,
                "quantity decrease acknowledges partial stacks, without waiting for removal");
    }

    private static void batchSale() {
        Fake io = new Fake();
        io.stock.put((short)27, 1); io.stock.put((short)28, 1);
        RemoteSellController c = new RemoteSellController(io);
        Target second = new Target((short)28, (byte)3, "Áo vải xanh", 1, false, false);
        check(c.start(new Target[] {target(1), second}, null, 0), "start batch");
        c.tick(0); c.tick(100);
        check(io.sales == 1 && io.saleIds.get(0) == 27, "only one outstanding item");
        io.stock.put((short)27, 0); ++io.revision; c.tick(140); c.tick(200);
        check(io.sales == 1, "space successive sales by acknowledgement plus delay");
        c.tick(400);
        check(io.sales == 2 && io.saleIds.get(1) == 28, "send second item after first acknowledgement");
        io.stock.put((short)28, 0); ++io.revision; c.tick(440); drain(c, 720);
        check(c.soldItems() == 2 && "done".equals(c.phase()), "batch sale records both confirmed items");
    }

    private static void serverDenial() {
        Fake io = new Fake();
        RemoteSellController c = create(io, 1);
        reachShop(c);
        c.onNotice("Bạn ở quá xa cửa hàng.");
        drain(c, 8120);
        check("failed".equals(c.phase()) && "remote_shop_unavailable".equals(c.error()), "server denial is failure");
        check(c.soldItems() == 0 && io.sales == 1, "no invented success and no repeated sell packets");
        check(c.message().contains("quá xa"), "include server diagnostic");
        check(io.moves.get(io.moves.size() - 1)[0] == 0, "restore even after rejection");
    }

    private static void slowMenu() {
        Fake io = new Fake();
        RemoteSellController c = create(io, 1);
        reachMenu(c);
        c.tick(10041); // Real traces contained menu replies delayed by six seconds.
        check("wait_menu".equals(c.phase()), "keep waiting for a delayed menu");
        c.onMenu(-3, 0, 0);
        c.tick(10081);
        c.tick(15081);
        check("wait_shop".equals(c.phase()), "allow more than four seconds for the shop reply");
        c.onShop(0); c.tick(15121); c.tick(15400);
        check(io.sales == 2, "retry only after the delayed real shop reply");
        c.cancel(15440); drain(c, 15480);
    }

    private static void confirmationsAndBusy() {
        Fake io = new Fake();
        RemoteSellController c = create(io, 1);
        check(!c.start(new Target[] {target(1)}, null, 10), "duplicate click cannot spawn another sale");
        check(!c.onDialog(1, 0, "Bạn có muốn bán Gậy gỗ?"), "unrelated item dialog stays manual");
        check(c.onDialog(2, 0, "Bạn có muốn bán Vòng bình an với giá 1 Vàng không?"), "matching sale dialog");
        check(io.confirmations == 0, "callback does not write network packets");
        c.tick(20);
        check(io.confirmations == 1, "game loop sends confirmation");
        c.onDialog(2, 0, "Bạn có muốn bán Vòng bình an với giá 1 Vàng không?"); c.tick(30);
        check(io.confirmations == 1, "do not replay the same confirmation");
        c.cancel(40); drain(c, 80);
        check("cancelled".equals(c.phase()) && !c.onDialog(2, 0, "Bạn có muốn bán Vòng bình an?"), "no confirmation after cancel");
    }

    private static void contextAndSafety() {
        Fake io = new Fake(); RemoteSellController c = create(io, 1);
        reachMenu(c); int moves = io.moves.size();
        io.map = 1; c.tick(4100);
        check(!c.isActive() && io.moves.size() == moves && !c.guardPosition(4100), "never restore into a different map");
        check(!c.onMenu(-3, 7, 2), "ignore old context reply");
        io = new Fake(); c = create(io, 1); io.connected = false; c.tick(100);
        check(!c.isActive() && "context_changed".equals(c.error()), "disconnect terminates sale");
        io = new Fake(); c = create(io, 1); reachMenu(c); io.x = 12; c.tick(4100); drain(c, 4140);
        check("cancelled".equals(c.phase()) && io.moves.get(io.moves.size()-1)[0] == 12, "manual movement cancels and restores to new local position");
        io = new Fake(); io.safe = false; c = create(io, 1); c.tick(4001); drain(c, 4041);
        check(io.moves.isEmpty() && io.menus == 0 && "failed".equals(c.phase()), "do not send virtual positions through a portal");
        io = new Fake(); c = create(io, 1); io.quantity = -1; c.tick(100);
        check("item_changed".equals(c.error()), "reused inventory ID stops sale");
        io = new Fake(); io.allowed = false; c = create(io, 1); drain(c, 100);
        check(io.sales == 0, "recheck the filter before sending");
        io = new Fake(); io.failSale = true; c = create(io, 1);
        check(!c.isActive() && "transport_error".equals(c.error()), "send failure releases transaction");
    }

    public static void main(String[] args) {
        directSale(); realShopRequired(); lateDirectReply(); partialStack();
        batchSale(); serverDenial(); slowMenu(); confirmationsAndBusy(); contextAndSafety();
        System.out.println("RemoteSellController: " + checks + " checks passed");
    }
}
