package com.firefly.net.tcp.codec.decode;

import com.firefly.net.tcp.codec.protocol.ControlFrame;
import com.firefly.net.tcp.codec.protocol.DataFrame;
import com.firefly.net.tcp.codec.protocol.Frame;
import com.firefly.net.tcp.codec.protocol.MessageFrame;
import com.firefly.utils.lang.Pair;

import java.nio.ByteBuffer;

import static com.firefly.net.tcp.codec.protocol.MessageFrame.MESSAGE_FRAME_HEADER_LENGTH;

/**
 * @author Pengtao Qiu
 */
public class MessageFrameParser implements FfsocksParser<MessageFrame> {

    protected Frame frame;
    protected boolean endStream;
    protected int streamId;
    protected boolean endFrame;
    protected int payloadLength;
    protected byte[] data;
    protected State state = State.MESSAGE_HEADER;

    @Override
    public Pair<Result, MessageFrame> parse(ByteBuffer buffer, Frame header) {
        while (true) {
            switch (state) {
                case MESSAGE_HEADER: {
                    switch (parseFrameHeader(buffer, header)) {
                        case UNDERFLOW:
                        case COMPLETE:
                            return new Pair<>(Result.UNDERFLOW, null);
                        case OVERFLOW:
                            break;
                    }
                }
                break;
                case PAYLOAD:
                    return parsePayload(buffer);
            }
        }
    }

    protected Result parseFrameHeader(ByteBuffer buffer, Frame frame) {
        if (buffer.remaining() < minPocketLength()) {
            return Result.UNDERFLOW;
        }

        this.frame = frame;

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

    protected Pair<Result, MessageFrame> parsePayload(ByteBuffer buffer) {
        if (payloadLength == 0) {
            return generateResult(buffer);
        }

        if (buffer.remaining() < payloadLength) {
            return new Pair<>(Result.UNDERFLOW, null);
        }

        data = new byte[payloadLength];
        buffer.get(data);
        return generateResult(buffer);
    }

    private Pair<Result, MessageFrame> generateResult(ByteBuffer buffer) {
        Pair<Result, MessageFrame> pair = new Pair<>();
        switch (frame.getType()) {
            case CONTROL:
                pair.second = new ControlFrame(frame, endStream, streamId, endFrame, data);
                break;
            case DATA:
                pair.second = new DataFrame(frame, endStream, streamId, endFrame, data);
                break;
        }
        reset();

        if (buffer.hasRemaining()) {
            pair.first = Result.OVERFLOW;
        } else {
            pair.first = Result.COMPLETE;
        }
        return pair;
    }

    protected void reset() {
        frame = null;
        endStream = false;
        streamId = 0;
        endFrame = false;
        payloadLength = 0;
        data = null;
        state = State.MESSAGE_HEADER;
    }

    protected int minPocketLength() {
        return MESSAGE_FRAME_HEADER_LENGTH;
    }

    public enum State {
        MESSAGE_HEADER,
        PAYLOAD
    }
}
