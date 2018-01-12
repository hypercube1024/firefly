package com.firefly.net.tcp.codec.flex.decode;

import com.firefly.net.tcp.codec.exception.ProtocolException;
import com.firefly.net.tcp.codec.flex.protocol.Frame;
import com.firefly.net.tcp.codec.flex.protocol.PingFrame;
import com.firefly.utils.lang.Pair;

import java.nio.ByteBuffer;

/**
 * @author Pengtao Qiu
 */
public class PingFrameParser implements FlexParser<PingFrame> {

    @Override
    public Pair<Result, PingFrame> parse(ByteBuffer buffer, Frame header) {
        Pair<Result, PingFrame> pair = new Pair<>();
        switch (buffer.get()) {
            case 0:
                pair.second = new PingFrame(header, false);
                break;
            case 1:
                pair.second = new PingFrame(header, true);
                break;
            default:
                throw new ProtocolException("The reply flag must be 0 or 1");
        }
        if (buffer.hasRemaining()) {
            pair.first = Result.OVERFLOW;
        } else {
            pair.first = Result.COMPLETE;
        }
        return pair;
    }
}
