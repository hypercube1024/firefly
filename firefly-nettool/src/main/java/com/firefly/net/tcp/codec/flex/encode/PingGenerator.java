package com.firefly.net.tcp.codec.flex.encode;

import com.firefly.net.tcp.codec.Generator;
import com.firefly.net.tcp.codec.flex.protocol.Frame;
import com.firefly.net.tcp.codec.flex.protocol.PingFrame;

import java.nio.ByteBuffer;

import static com.firefly.net.tcp.codec.flex.encode.FrameGenerator.headerGenerator;

/**
 * @author Pengtao Qiu
 */
public class PingGenerator implements Generator {

    @Override
    public ByteBuffer generate(Object object) {
        PingFrame pingFrame = (PingFrame) object;
        
        int length = Frame.FRAME_HEADER_LENGTH + 1;
        ByteBuffer buffer = ByteBuffer.allocate(length);
        buffer.put(headerGenerator.generate(pingFrame));

        if (pingFrame.isReply()) {
            buffer.put((byte) 1);
        } else {
            buffer.put((byte) 0);
        }

        buffer.flip();
        return buffer;
    }
}
