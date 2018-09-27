package com.firefly.codec.http2.encode;

import com.firefly.codec.http2.frame.Frame;

import java.nio.ByteBuffer;
import java.util.LinkedList;
import java.util.List;

public class DisconnectGenerator extends FrameGenerator {

    private static final List<ByteBuffer> EMPTY = new LinkedList<>();

    public DisconnectGenerator() {
        super(null);
    }

    @Override
    public List<ByteBuffer> generate(Frame frame) {
        return EMPTY;
    }

}
