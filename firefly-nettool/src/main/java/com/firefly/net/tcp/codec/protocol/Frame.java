package com.firefly.net.tcp.codec.protocol;

import com.firefly.utils.Assert;

/**
 * The simple session protocol. The current version is 1.
 * The frame header length is 3 bytes. the format:
 * [magic number 0xE4 (1byte)] + [frame type (1byte)] + [version (1bytes)]
 *
 * @author Pengtao Qiu
 */
public class Frame {

    public static final byte MAGIC = (byte) 0xE4;
    public static final byte VERSION = (byte) 0x01;
    public static final int MAX_PAYLOAD_LENGTH = Short.MAX_VALUE;
    public static final int FRAME_HEADER_LENGTH = 3;

    protected final byte magic;
    protected final FrameType type;
    protected final byte version;

    public Frame(byte magic, FrameType type, byte version) {
        Assert.isTrue(magic == MAGIC, "The protocol format error");
        this.magic = magic;
        this.type = type;
        this.version = version;
    }

    public FrameType getType() {
        return type;
    }

    public byte getMagic() {
        return magic;
    }

    public byte getVersion() {
        return version;
    }

    public static boolean isEnd(final short data) {
        short endFlag = (short) (data & Short.MIN_VALUE);
        return endFlag == Short.MIN_VALUE;
    }

    public static short removeEndFlag(final short data) {
        return (short) (data & Short.MAX_VALUE);
    }

    public static short addEndFlag(final short data) {
        return (short) (data | Short.MIN_VALUE);
    }

    public static boolean isEnd(final int data) {
        int endFlag = data & Integer.MIN_VALUE;
        return endFlag == Integer.MIN_VALUE;
    }

    public static int removeEndFlag(final int data) {
        return data & Integer.MAX_VALUE;
    }

    public static int addEndFlag(final int data) {
        return data | Integer.MIN_VALUE;
    }
}
