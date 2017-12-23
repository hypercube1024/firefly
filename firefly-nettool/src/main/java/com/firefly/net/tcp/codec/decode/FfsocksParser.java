package com.firefly.net.tcp.codec.decode;

import com.firefly.net.tcp.codec.protocol.Frame;
import com.firefly.utils.lang.Pair;

import java.nio.ByteBuffer;

/**
 * @author Pengtao Qiu
 */
public interface FfsocksParser<T extends Frame> {

    Pair<Result, T> parse(ByteBuffer buffer, Frame header);

    enum Result {
        UNDERFLOW,
        OVERFLOW,
        COMPLETE
    }
}
