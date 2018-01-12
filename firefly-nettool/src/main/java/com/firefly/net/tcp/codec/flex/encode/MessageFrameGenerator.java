package com.firefly.net.tcp.codec.flex.encode;

import com.firefly.net.tcp.codec.Generator;
import com.firefly.net.tcp.codec.flex.protocol.Frame;
import com.firefly.net.tcp.codec.flex.protocol.MessageFrame;

import java.nio.ByteBuffer;
import java.util.Optional;

import static com.firefly.net.tcp.codec.flex.encode.FrameGenerator.headerGenerator;
import static com.firefly.net.tcp.codec.flex.protocol.Frame.FRAME_HEADER_LENGTH;
import static com.firefly.net.tcp.codec.flex.protocol.MessageFrame.MESSAGE_FRAME_HEADER_LENGTH;

/**
 * @author Pengtao Qiu
 */
public class MessageFrameGenerator implements Generator {

    @Override
    public ByteBuffer generate(Object object) {
        MessageFrame messageFrame = (MessageFrame) object;
        short payloadLength = Optional.ofNullable(messageFrame.getData()).map(d -> d.length)
                                      .filter(len -> len <= Frame.MAX_PAYLOAD_LENGTH)
                                      .map(Integer::shortValue).orElse((short) 0);

        ByteBuffer buffer = ByteBuffer.allocate(FRAME_HEADER_LENGTH + MESSAGE_FRAME_HEADER_LENGTH + payloadLength);

        // generate header
        buffer.put(headerGenerator.generate(messageFrame));
        if (messageFrame.isEndStream()) {
            buffer.putInt(Frame.addEndFlag(messageFrame.getStreamId()));
        } else {
            buffer.putInt(Frame.removeEndFlag(messageFrame.getStreamId()));
        }

        // generate payload
        if (messageFrame.isEndFrame()) {
            buffer.putShort(Frame.addEndFlag(payloadLength));
        } else {
            buffer.putShort(Frame.removeEndFlag(payloadLength));
        }
        if (payloadLength > 0) {
            buffer.put(messageFrame.getData());
        }
        
        buffer.flip();
        return buffer;
    }

}
