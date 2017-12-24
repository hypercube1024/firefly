package com.firefly.net.tcp.codec.ffsocks.decode;

import com.firefly.net.tcp.codec.ffsocks.protocol.Frame;
import com.firefly.utils.lang.Pair;

import java.nio.ByteBuffer;

/**
 * @author Pengtao Qiu
 */
abstract public class AbstractPayloadFrameParser<T extends Frame> implements FfsocksParser<T> {

    protected Frame frame;
    protected State state = State.HEADER;
    protected int payloadLength;
    protected byte[] data;

    public Pair<Result, T> parse(ByteBuffer buffer, Frame header) {
        frame = header;
        while (true) {
            switch (state) {
                case HEADER: {
                    switch (parseFrameHeader(buffer)) {
                        case UNDERFLOW:
                            return new Pair<>(Result.UNDERFLOW, null);
                        case COMPLETE:
                            if (payloadLength == 0) {
                                return generateResult(buffer);
                            } else {
                                return new Pair<>(Result.UNDERFLOW, null);
                            }
                        case OVERFLOW:
                            break;
                    }
                }
                break;
                case PAYLOAD:
                    return parsePayload(buffer);
            }
        }
    }

    abstract protected Result parseFrameHeader(ByteBuffer buffer);

    protected Pair<Result, T> parsePayload(ByteBuffer buffer) {
        if (payloadLength == 0) {
            return generateResult(buffer);
        }

        if (buffer.remaining() < payloadLength) {
            return new Pair<>(Result.UNDERFLOW, null);
        }

        data = new byte[payloadLength];
        buffer.get(data);
        return generateResult(buffer);
    }

    abstract protected Pair<Result, T> generateResult(ByteBuffer buffer);

    protected void reset() {
        frame = null;
        payloadLength = 0;
        data = null;
        state = State.HEADER;
    }

}
