package com.firefly.net.tcp.codec.flex.stream.impl;

import com.firefly.net.io.BufferedNetOutputStream;

/**
 * @author Pengtao Qiu
 */
public class FlexBufferedOutputStream extends BufferedNetOutputStream {

    public FlexBufferedOutputStream(FlexOutputStream output, int bufferSize) {
        super(output, bufferSize);
    }

}
