package com.firefly.net.tcp.codec.flex.decode;

import com.firefly.net.tcp.codec.flex.protocol.Frame;
import com.firefly.utils.lang.Pair;

import java.nio.ByteBuffer;

/**
 * @author Pengtao Qiu
 */
public interface FlexParser<T extends Frame> {

    Pair<Result, T> parse(ByteBuffer buffer, Frame header);

    enum Result {
        UNDERFLOW,
        OVERFLOW,
        COMPLETE
    }

    enum State {
        HEADER,
        PAYLOAD
    }
}
