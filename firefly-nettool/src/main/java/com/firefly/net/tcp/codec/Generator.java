package com.firefly.net.tcp.codec;

import java.nio.ByteBuffer;

/**
 * @author Pengtao Qiu
 */
public interface Generator<T> {

    ByteBuffer generate(T obj);

}
