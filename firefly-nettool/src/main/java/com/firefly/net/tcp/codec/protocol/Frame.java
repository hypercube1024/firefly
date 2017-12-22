package com.firefly.net.tcp.codec.protocol;

import com.firefly.utils.Assert;

/**
 * The simple session protocol. The current version is 1.
 * The frame header length is 4 bytes. the format:
 * [magic number 0xE4 (1byte)] + [frame type (1byte)] + [version (2bytes)] + payload
 *
 * @author Pengtao Qiu
 */
public class Frame {

    protected final byte magic;
    protected final FrameType type;
    protected final short version;

    public Frame(byte magic, FrameType type, short version) {
        Assert.isTrue(magic == (byte) 0xE4, "The protocol format error");
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

    public short getVersion() {
        return version;
    }
}
