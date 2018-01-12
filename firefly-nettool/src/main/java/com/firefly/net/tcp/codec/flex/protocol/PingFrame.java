package com.firefly.net.tcp.codec.flex.protocol;

/**
 * The ping frame format:
 * [frame header (3 bytes)] + [reply (1 byte)]
 * <p>
 * If the reply is false, the current endpoint need reply a ping frame to the opposite end.
 *
 * @author Pengtao Qiu
 */
public class PingFrame extends Frame {

    private final boolean reply;

    public PingFrame(boolean reply) {
        this(MAGIC, FrameType.PING, VERSION, reply);
    }

    public PingFrame(Frame frame, boolean reply) {
        this(frame.magic, frame.type, frame.version, reply);
    }

    public PingFrame(byte magic, FrameType type, byte version, boolean reply) {
        super(magic, type, version);
        this.reply = reply;
    }

    public boolean isReply() {
        return reply;
    }

    @Override
    public String toString() {
        return "PingFrame{" +
                "reply=" + reply +
                ", type=" + type +
                ", version=" + version +
                '}';
    }
}
