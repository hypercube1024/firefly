package com.firefly.net.tcp.codec.flex.protocol;

import com.firefly.utils.Assert;

/**
 * The error frame format:
 * [frame header (3 bytes)] + [error code (1 byte)]
 * [reserved (1bit)] + [payload length (15bit)] + [error message]
 *
 * @author Pengtao Qiu
 */
public class DisconnectionFrame extends Frame {

    public static final int DISCONNECTION_FRAME_HEADER_LENGTH = 3;

    private final byte code;
    private final byte[] data;

    public DisconnectionFrame(byte code, byte[] data) {
        this(MAGIC, FrameType.DISCONNECTION, VERSION, code, data);
    }

    public DisconnectionFrame(Frame frame, byte code, byte[] data) {
        this(frame.magic, frame.type, frame.version, code, data);
    }

    public DisconnectionFrame(byte magic, FrameType type, byte version,
                              byte code, byte[] data) {
        super(magic, type, version);
        this.code = code;
        this.data = data;
        if (data != null) {
            Assert.isTrue(data.length <= MAX_PAYLOAD_LENGTH, "The data length must be not greater than the max payload length");
        }
    }

    public byte getCode() {
        return code;
    }

    public byte[] getData() {
        return data;
    }

    @Override
    public String toString() {
        return "DisconnectionFrame{" +
                "code=" + code +
                '}';
    }
}
