import net.Message;
import net.ISession;
import net.IMessageHandler;
import netcommand.global.GlobalService;
import java.util.ArrayList;
import java.util.Arrays;

public class RemoteSellPacketTest {
    private static class FakeSession implements ISession {
        ArrayList<Message> packets = new ArrayList<Message>();
        public boolean isConnected() { return true; }
        public void setHandler(IMessageHandler handler) {}
        public void connect(String host, String port) {}
        public void sendMessage(Message message) { packets.add(message); }
        public void close() {}
    }
    private static class Service extends GlobalService {
        byte sharedCommand() { return this.m.command; }
    }
    private static void packet(Message msg, int command, int... data) {
        byte[] expected = new byte[data.length];
        for (int i = 0; i < data.length; ++i) expected[i] = (byte)data[i];
        if (msg.command != (byte)command || !Arrays.equals(msg.getData(), expected))
            throw new AssertionError("Bad packet " + command);
    }
    public static void main(String[] args) {
        FakeSession session = new FakeSession();
        Service service = new Service(); service.setSession(session); service.init((byte)99);
        service.remoteSellMove(756, 120);
        service.remoteSellMenu(-3);
        service.remoteSellShop(-3, 7, 2);
        service.remoteSellItem((short)27, (byte)3);
        service.remoteSellConfirm(77, 0);
        packet(session.packets.get(0), 4, 2, 244, 0, 120);
        packet(session.packets.get(1), 23, 253);
        packet(session.packets.get(2), -30, 255, 253, 7, 2);
        packet(session.packets.get(3), 18, 3, 0, 27, 1);
        packet(session.packets.get(4), -32, 0, 77, 0, 1);
        if (service.sharedCommand() != 99) throw new AssertionError("Remote sale overwrote shared message");
        service.remoteSellItem((short)300, (byte)7);
        packet(session.packets.get(5), 18, 7, 1, 44, 1);
        packet(session.packets.get(3), 18, 3, 0, 27, 1);
        System.out.println("RemoteSellPacket: 8 checks passed (correct opcodes, payloads, isolated messages)");
    }
}
