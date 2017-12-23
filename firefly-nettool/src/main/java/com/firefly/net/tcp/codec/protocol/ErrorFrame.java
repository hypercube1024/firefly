package com.firefly.net.tcp.codec.protocol;

/**
 * The error frame format:
 * [frame header (3 bytes)] + [error code (1 byte)]
 * [reserved (1bit)] + [payload length (15bit)] + [error message]
 *
 * @author Pengtao Qiu
 */
public class ErrorFrame extends Frame {

    private final byte code;
    private final byte[] data;

    public ErrorFrame(byte code, byte[] data) {
        this(MAGIC, FrameType.ERROR, VERSION, code, data);
    }

    public ErrorFrame(byte magic, FrameType type, byte version,
                      byte code, byte[] data) {
        super(magic, type, version);
        this.code = code;
        this.data = data;
    }

    public byte getCode() {
        return code;
    }

    public byte[] getData() {
        return data;
    }
}
