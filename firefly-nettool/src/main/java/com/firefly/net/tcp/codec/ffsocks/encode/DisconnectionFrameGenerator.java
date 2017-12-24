package com.firefly.net.tcp.codec.ffsocks.encode;

import com.firefly.net.tcp.codec.Generator;
import com.firefly.net.tcp.codec.ffsocks.protocol.DisconnectionFrame;
import com.firefly.net.tcp.codec.ffsocks.protocol.Frame;

import java.nio.ByteBuffer;
import java.util.Optional;

import static com.firefly.net.tcp.codec.ffsocks.encode.FrameGenerator.headerGenerator;
import static com.firefly.net.tcp.codec.ffsocks.protocol.DisconnectionFrame.DISCONNECTION_FRAME_HEADER_LENGTH;
import static com.firefly.net.tcp.codec.ffsocks.protocol.Frame.FRAME_HEADER_LENGTH;

/**
 * @author Pengtao Qiu
 */
public class DisconnectionFrameGenerator implements Generator {
    @Override
    public ByteBuffer generate(Object object) {
        DisconnectionFrame frame = (DisconnectionFrame) object;

        short payloadLength = Optional.ofNullable(frame.getData()).map(d -> d.length)
                                      .filter(len -> len <= Frame.MAX_PAYLOAD_LENGTH)
                                      .map(Integer::shortValue).orElse((short) 0);

        ByteBuffer buffer = ByteBuffer.allocate(FRAME_HEADER_LENGTH + DISCONNECTION_FRAME_HEADER_LENGTH + payloadLength);

        // generate header
        buffer.put(headerGenerator.generate(frame));
        buffer.put(frame.getCode());

        // generate payload
        buffer.putShort(Frame.removeEndFlag(payloadLength));
        if (payloadLength > 0) {
            buffer.put(frame.getData());
        }

        buffer.flip();
        return buffer;
    }
}
