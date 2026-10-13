import com.silverknight.NpcChatPayload;
import java.io.*;

public final class NpcChatPayloadTest {
    private static int checks;
    private static void check(boolean value) { if (!value) throw new AssertionError("NPC chat check " + checks); ++checks; }
    private static byte[] hex(String value) {
        byte[] bytes = new byte[value.length()/2];
        for (int i=0;i<bytes.length;i++) bytes[i]=(byte)Integer.parseInt(value.substring(i*2,i*2+2),16);
        return bytes;
    }
    private static NpcChatPayload read(byte[] bytes) throws IOException {
        return NpcChatPayload.read(new DataInputStream(new ByteArrayInputStream(bytes)));
    }
    private static byte[] frame(boolean textFirst, byte id, String text) throws IOException {
        ByteArrayOutputStream bytes=new ByteArrayOutputStream(); DataOutputStream output=new DataOutputStream(bytes);
        if (textFirst) { output.writeUTF(text); output.writeByte(id); }
        else { output.writeByte(id); output.writeUTF(text); }
        return bytes.toByteArray();
    }
    private static void rejects(byte[] bytes) {
        try { read(bytes); throw new AssertionError("Malformed NPC chat accepted"); }
        catch (IOException expected) { ++checks; }
    }
    public static void main(String[] args) throws Exception {
        // Real server response captured on 13/10/2026, opcode 60, Graham (-58).
        byte[] live=hex("001F4368C3BA632062E1BAA16E206368C6A1692067616D65207675692076E1BABBC6");
        NpcChatPayload chat=read(live);
        check(chat.npcId==-58);
        check(chat.text.equals("Chúc bạn chơi game vui vẻ"));
        chat=read(frame(false,(byte)-57,"Chúc bạn chơi game vui vẻ"));
        check(chat.npcId==-57);check(chat.text.equals("Chúc bạn chơi game vui vẻ"));
        for (boolean textFirst:new boolean[]{true,false}) {
            chat=read(frame(textFirst,(byte)-58,""));check(chat.npcId==-58&&chat.text.isEmpty());
            chat=read(frame(textFirst,(byte)-57,"NPC\u0000đá"));check(chat.text.equals("NPC\u0000đá"));
        }
        rejects(java.util.Arrays.copyOf(live,live.length-1));
        rejects(new byte[0]);
        byte[] extra=java.util.Arrays.copyOf(live,live.length+1);rejects(extra);
        System.out.println("NpcChatPayload: "+checks+" checks passed (live capture, legacy layout, malformed data)");
    }
}
