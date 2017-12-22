package com.firefly.net.tcp.codec.protocol;

/**
 * The ping frame format:
 * [frame header (4 bytes)] + [reply (1 byte)]
 * <p>
 * If the reply is false, the current endpoint need reply a ping frame to the opposite end.
 *
 * @author Pengtao Qiu
 */
public class PingFrame extends Frame {

    private final boolean reply;

    public PingFrame(byte magic, short version, boolean reply) {
        super(magic, FrameType.PING, version);
        this.reply = reply;
    }

    public boolean isReply() {
        return reply;
    }
}
