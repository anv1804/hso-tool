package com.silverknight;

import java.io.DataInputStream;
import java.io.IOException;

/** Decode NPC chat from the current server and the older Java-client layout. */
public final class NpcChatPayload {
    public final byte npcId;
    public final String text;

    private NpcChatPayload(byte npcId, String text) {
        this.npcId = npcId;
        this.text = text;
    }

    public static NpcChatPayload read(DataInputStream input) throws IOException {
        if (!input.markSupported()) throw new IOException("NPC chat requires a resettable message payload");
        input.mark(input.available() + 1);
        try {
            // Live opcode 60: writeUTF(text), writeByte(npcId).
            String text = input.readUTF();
            if (input.available() != 1) throw new IOException("Not a text-first NPC chat payload");
            return new NpcChatPayload(input.readByte(), text);
        } catch (IOException textFirstError) {
            input.reset();
            // Preserve compatibility with the original client's ID-first layout.
            byte id = input.readByte();
            String text = input.readUTF();
            if (input.available() != 0) throw new IOException("Unexpected trailing NPC chat data");
            return new NpcChatPayload(id, text);
        }
    }
}
