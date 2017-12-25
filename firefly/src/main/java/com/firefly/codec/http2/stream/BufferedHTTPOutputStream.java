package com.firefly.codec.http2.stream;

import com.firefly.net.io.BufferedNetOutputStream;

/**
 * @author Pengtao Qiu
 */
public class BufferedHTTPOutputStream extends BufferedNetOutputStream {

    public BufferedHTTPOutputStream(HTTPOutputStream output, int bufferSize) {
        super(output, bufferSize);
    }
}
