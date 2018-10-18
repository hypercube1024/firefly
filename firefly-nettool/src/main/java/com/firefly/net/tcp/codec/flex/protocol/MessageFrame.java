package com.firefly.net.tcp.codec.flex.protocol;

import com.firefly.utils.Assert;
import com.firefly.utils.codec.HexUtils;

import java.util.Optional;

/**
 * The message frame format:
 * [frame header (3 bytes)] + [stream end flag (1 bit)] + [stream id (31bit)]
 * [frame end flag (1bit)] + [payload length (15bit)] + [data payload]
 * <p>
 * The payload length range of a data frame is from 0 to 32767 bytes.
 * <p>
 * The frame end flag is 1 represents the last control or data frame.
 * <p>
 * If you send a frame with stream end flag is 1, that means the stream is local close,
 * you can not sand any frames to the remote endpoint.
 *
 * @author Pengtao Qiu
 */
public class MessageFrame extends Frame {

    public static final int MESSAGE_FRAME_HEADER_LENGTH = 6;

    protected final boolean endStream;
    protected final int streamId;
    protected final boolean endFrame;
    protected final byte[] data;

    public MessageFrame(byte magic, FrameType type, byte version, boolean endStream, int streamId, boolean endFrame, byte[] data) {
        super(magic, type, version);
        this.endStream = endStream;
        this.streamId = streamId;
        this.endFrame = endFrame;
        this.data = data;
        if (data != null) {
            Assert.isTrue(data.length <= MAX_PAYLOAD_LENGTH, "The data length must be not greater than the max payload length");
        }
    }

    public boolean isEndStream() {
        return endStream;
    }

    public int getStreamId() {
        return streamId;
    }

    public boolean isEndFrame() {
        return endFrame;
    }

    public byte[] getData() {
        return data;
    }

    @Override
    public String toString() {
        return "MessageFrame{" +
                "endStream=" + endStream +
                ", streamId=" + streamId +
                ", endFrame=" + endFrame +
                ", type=" + type +
                ", version=" + version +
                ", data=" + Optional.ofNullable(data).map(d -> d.length).orElse(0) + ", "
                + Optional.ofNullable(data).map(HexUtils::bytesToHex).orElse("") +
                '}';
    }
}
