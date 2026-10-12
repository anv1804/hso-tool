/*
 * Decompiled with CFR 0.152.
 */
package net;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;

public class Message {
    public byte command;
    private ByteArrayOutputStream os = null;
    private DataOutputStream dos = null;
    private ByteArrayInputStream iss = null;
    private DataInputStream dis = null;

    public Message() {
    }

    public Message(int command) {
        this.command = (byte)command;
        this.os = new ByteArrayOutputStream();
        this.dos = new DataOutputStream(this.os);
    }

    public Message(byte command) {
        this.command = command;
        this.os = new ByteArrayOutputStream();
        this.dos = new DataOutputStream(this.os);
    }

    public Message(byte command, byte[] data) {
        this.command = command;
        this.iss = new ByteArrayInputStream(data);
        this.dis = new DataInputStream(this.iss);
    }

    public byte[] getData() {
        return this.os.toByteArray();
    }

    public DataInputStream reader() {
        return this.dis;
    }

    public DataOutputStream writer() {
        return this.dos;
    }

    public void cleanup() {
    }
}

