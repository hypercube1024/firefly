package com.firefly.net.tcp.codec.ffsocks.stream.impl;

import com.firefly.net.io.BufferedNetOutputStream;

/**
 * @author Pengtao Qiu
 */
public class BufferedFfsocksOutputStream extends BufferedNetOutputStream {

    public BufferedFfsocksOutputStream(FfsocksOutputStream output, int bufferSize) {
        super(output, bufferSize);
    }

}
