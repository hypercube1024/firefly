package com.firefly.net.tcp.codec.protocol;

/**
 * The data frame is used to transfer content.
 *
 * @author Pengtao Qiu
 */
public class DataFrame extends MessageFrame {

    public DataFrame(boolean endStream, int streamId,
                     boolean endFrame, byte[] data) {
        this(MAGIC, FrameType.DATA, VERSION, endStream, streamId, endFrame, data);
    }

    public DataFrame(Frame frame, boolean endStream, int streamId,
                     boolean endFrame, byte[] data) {
        this(frame.magic, frame.type, frame.version, endStream, streamId, endFrame, data);
    }

    public DataFrame(byte magic, FrameType type, byte version,
                     boolean endStream, int streamId,
                     boolean endFrame, byte[] data) {
        super(magic, type, version, endStream, streamId, endFrame, data);
    }

}
