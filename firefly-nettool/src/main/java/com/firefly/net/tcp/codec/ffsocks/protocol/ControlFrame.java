package com.firefly.net.tcp.codec.ffsocks.protocol;

/**
 * The control frame is used to transfer the meta-information of an application.
 *
 * @author Pengtao Qiu
 */
public class ControlFrame extends MessageFrame {

    public ControlFrame(boolean endStream, int streamId,
                        boolean endFrame, byte[] data) {
        this(MAGIC, FrameType.CONTROL, VERSION, endStream, streamId, endFrame, data);
    }

    public ControlFrame(Frame frame, boolean endStream, int streamId,
                        boolean endFrame, byte[] data) {
        this(frame.magic, frame.type, frame.version, endStream, streamId, endFrame, data);
    }

    public ControlFrame(byte magic, FrameType type, byte version,
                        boolean endStream, int streamId,
                        boolean endFrame, byte[] data) {
        super(magic, type, version, endStream, streamId, endFrame, data);
    }
}
