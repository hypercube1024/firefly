package com.firefly.net.tcp.codec.encode;

import com.firefly.net.tcp.codec.Generator;
import com.firefly.net.tcp.codec.protocol.Frame;

import java.nio.ByteBuffer;

/**
 * @author Pengtao Qiu
 */
public class FrameHeaderGenerator implements Generator<Frame> {

    @Override
    public ByteBuffer generate(Frame frame) {
        ByteBuffer buffer = ByteBuffer.allocate(Frame.FRAME_HEADER_LENGTH);
        buffer.put(frame.getMagic())
              .put(frame.getType().getValue())
              .put(frame.getVersion()).flip();
        return buffer;
    }
}
