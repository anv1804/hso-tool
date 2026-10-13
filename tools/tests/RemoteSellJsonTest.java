import java.lang.reflect.Method;

public class RemoteSellJsonTest {
    private static Method parser;
    private static int checks;
    private static void value(String json, String key, String expected) throws Exception {
        String actual = (String)parser.invoke(null, json, key);
        ++checks;
        if (expected == null ? actual != null : !expected.equals(actual))
            throw new AssertionError("Incorrect IPC value for " + key + ": " + actual);
    }
    public static void main(String[] args) throws Exception {
        Class<?> bridge = Class.forName("com.silverknight.HsoBridge");
        parser = bridge.getDeclaredMethod("extractJsonValue", String.class, String.class);
        parser.setAccessible(true);
        value("{\"item_name\": \"V\\u00f2ng, \\\"bình an\\\" [Kho\\u00e1]\"}", "item_name", "Vòng, \"bình an\" [Khoá]");
        value("{\"ids\": \"1;2;3\", \"configured\": 1}", "ids", "1;2;3");
        value("{\"ids\": \"\", \"configured\": 0}", "ids", "");
        value("{\"allow_locked\" : true}", "allow_locked", "true");
        value("{\"id\": -3}", "id", "-3");
        value("{\"item_name\":\"Đá\\\\Ngọc\"}", "item_name", "Đá\\Ngọc");
        value("{\"id\":27}", "item_name", null);
        value("{\"item_name\":\"bad\\uXXYY\"}", "item_name", null);
        value("{\"item_name\":\"unterminated}", "item_name", null);
        Method shop = bridge.getMethod("remoteShopOption", int.class, int.class, String[].class);
        String[] classMenu = {"Chiến binh", "Sát thủ", "Pháp sư", "Xạ thủ", "Tháo mề đay"};
        for (int clazz = 0; clazz < 4; ++clazz) {
            ++checks;
            if ((Integer)shop.invoke(null, -4, clazz, classMenu) != clazz)
                throw new AssertionError("Doubar must choose the actual character class");
        }
        ++checks;
        if ((Integer)shop.invoke(null, -34, 0, classMenu) != -1)
            throw new AssertionError("An unrelated NPC's class menu is not a merchant");
        ++checks;
        if ((Integer)shop.invoke(null, -3, 0, new String[]{"Đổi vật phẩm shop", "Mua bán"}) != 1)
            throw new AssertionError("Only the passive buying/selling choice should be selected");
        System.out.println("RemoteSellJson: " + checks + " checks passed");
    }
}
