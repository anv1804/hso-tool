import com.silverknight.GrahamNpcMenu;
import java.util.Arrays;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;

public final class GrahamNpcMenuTest {
    public static void main(String[] args) throws Exception {
        String[] original = GrahamNpcMenu.options();
        if (original.length != 4 || !"Bán đá quý".equals(original[1]))
            throw new AssertionError("Use the exact observed Graham option order");
        if (!GrahamNpcMenu.isShopOption(0) || !GrahamNpcMenu.isShopOption(2)
                || GrahamNpcMenu.isShopOption(1) || GrahamNpcMenu.isShopOption(3))
            throw new AssertionError("Only shop-list options can replay a catalog; sell/help cannot");
        String[] server = {"Mua bán", "Bán đá quý"};
        GrahamNpcMenu.remember(-58, 7, server, "Graham");
        server[1] = "Mua vật phẩm khác";
        if (GrahamNpcMenu.menuId() != 7 || !"Bán đá quý".equals(GrahamNpcMenu.options()[1]))
            throw new AssertionError("Keep menu ID and copy the server options together");
        String[] snapshot = GrahamNpcMenu.options(); snapshot[0] = "changed";
        if (!"Mua bán".equals(GrahamNpcMenu.options()[0]))
            throw new AssertionError("A reader cannot mutate the menu options");
        GrahamNpcMenu.remember(-57, 2, original, "Other NPC");
        GrahamNpcMenu.remember(-58, 2, new String[]{null}, "Malformed");
        if (GrahamNpcMenu.menuId() != 7 || !Arrays.equals(GrahamNpcMenu.options(), new String[]{"Mua bán", "Bán đá quý"}))
            throw new AssertionError("Unrelated and malformed replies cannot replace Graham's menu");
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        DataOutputStream data = new DataOutputStream(bytes);
        data.writeUTF("Đồ thương nhân"); data.writeByte(0); data.writeShort(2); data.writeShort(1); data.writeShort(2);
        if (!GrahamNpcMenu.validMerchantCatalog(bytes.toByteArray()))
            throw new AssertionError("Accept the observed potion-shop catalog format");
        if (GrahamNpcMenu.validMerchantCatalog(Arrays.copyOf(bytes.toByteArray(), bytes.size()-1))
                || GrahamNpcMenu.validMerchantCatalog(new byte[5000]) || GrahamNpcMenu.validMerchantCatalog(null))
            throw new AssertionError("Reject truncated, oversized and missing catalogs");
        bytes.reset(); data.writeUTF("Other player's shop"); data.writeByte(17); data.writeShort(1); data.writeShort(1);
        if (GrahamNpcMenu.validMerchantCatalog(bytes.toByteArray()))
            throw new AssertionError("Never replay a different shop type");
        System.out.println("GrahamNpcMenu: 8 checks passed");
    }
}
