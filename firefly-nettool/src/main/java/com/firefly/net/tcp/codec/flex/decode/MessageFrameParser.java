package com.firefly.net.tcp.codec.flex.decode;

import com.firefly.net.tcp.codec.flex.protocol.ControlFrame;
import com.firefly.net.tcp.codec.flex.protocol.DataFrame;
import com.firefly.net.tcp.codec.flex.protocol.Frame;
import com.firefly.net.tcp.codec.flex.protocol.MessageFrame;
import com.firefly.utils.lang.Pair;

import java.nio.ByteBuffer;

import static com.firefly.net.tcp.codec.flex.protocol.MessageFrame.MESSAGE_FRAME_HEADER_LENGTH;

/**
 * @author Pengtao Qiu
 */
public class MessageFrameParser extends AbstractPayloadFrameParser<MessageFrame> {

    protected boolean endStream;
    protected int streamId;
    protected boolean endFrame;

    @Override
    protected Result parseFrameHeader(ByteBuffer buffer) {
        if (buffer.remaining() < minPocketLength()) {
            return Result.UNDERFLOW;
        }

        int stream = buffer.getInt();
        endStream = Frame.isEnd(stream);
        streamId = Frame.removeEndFlag(stream);

        short length = buffer.getShort();
        endFrame = Frame.isEnd(length);
        payloadLength = Frame.removeEndFlag(length);

        state = State.PAYLOAD;
        if (buffer.hasRemaining()) {
            return Result.OVERFLOW;
        } else {
            return Result.COMPLETE;
        }
    }

    @Override
    protected Pair<Result, MessageFrame> generateResult(ByteBuffer buffer) {
        Pair<Result, MessageFrame> pair = new Pair<>();
        switch (frame.getType()) {
            case CONTROL:
                pair.second = new ControlFrame(frame, endStream, streamId, endFrame, data);
                break;
            case DATA:
                pair.second = new DataFrame(frame, endStream, streamId, endFrame, data);
                break;
        }

        if (buffer.hasRemaining()) {
            pair.first = Result.OVERFLOW;
        } else {
            pair.first = Result.COMPLETE;
        }

        reset();
        return pair;
    }

    protected void reset() {
        endStream = false;
        streamId = 0;
        endFrame = false;
        super.reset();
    }

    protected int minPocketLength() {
        return MESSAGE_FRAME_HEADER_LENGTH;
    }
}
