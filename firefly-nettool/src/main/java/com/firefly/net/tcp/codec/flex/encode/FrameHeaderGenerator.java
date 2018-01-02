package com.firefly.net.tcp.codec.flex.encode;

import com.firefly.net.tcp.codec.Generator;
import com.firefly.net.tcp.codec.flex.protocol.Frame;

import java.nio.ByteBuffer;

/**
 * @author Pengtao Qiu
 */
public class FrameHeaderGenerator implements Generator {

    @Override
    public ByteBuffer generate(Object object) {
        Frame frame = (Frame) object;
        ByteBuffer buffer = ByteBuffer.allocate(Frame.FRAME_HEADER_LENGTH);
        buffer.put(frame.getMagic())
              .put(frame.getType().getValue())
              .put(frame.getVersion()).flip();
        return buffer;
    }
}
