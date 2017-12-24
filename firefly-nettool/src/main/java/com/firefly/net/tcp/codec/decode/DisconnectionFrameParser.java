package com.firefly.net.tcp.codec.decode;

import com.firefly.net.tcp.codec.protocol.DisconnectionFrame;
import com.firefly.net.tcp.codec.protocol.Frame;
import com.firefly.utils.lang.Pair;

import java.nio.ByteBuffer;

import static com.firefly.net.tcp.codec.protocol.DisconnectionFrame.DISCONNECTION_FRAME_HEADER_LENGTH;

/**
 * @author Pengtao Qiu
 */
public class DisconnectionFrameParser extends AbstractPayloadFrameParser<DisconnectionFrame> {

    protected byte code;

    @Override
    protected Result parseFrameHeader(ByteBuffer buffer) {
        if (buffer.remaining() < DISCONNECTION_FRAME_HEADER_LENGTH) {
            return Result.UNDERFLOW;
        }

        code = buffer.get();

        short length = buffer.getShort();
        payloadLength = Frame.removeEndFlag(length);
        state = State.PAYLOAD;

        if (buffer.hasRemaining()) {
            return Result.OVERFLOW;
        } else {
            return Result.COMPLETE;
        }
    }

    @Override
    protected Pair<Result, DisconnectionFrame> generateResult(ByteBuffer buffer) {
        Pair<Result, DisconnectionFrame> pair = new Pair<>();
        pair.second = new DisconnectionFrame(frame, code, data);

        if (buffer.hasRemaining()) {
            pair.first = Result.OVERFLOW;
        } else {
            pair.first = Result.COMPLETE;
        }

        reset();
        return pair;
    }

    protected void reset() {
        code = 0;
        super.reset();
    }

}
