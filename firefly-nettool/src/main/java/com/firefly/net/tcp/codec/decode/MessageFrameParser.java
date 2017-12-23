package com.firefly.net.tcp.codec.decode;

import com.firefly.net.tcp.codec.AbstractByteBufferMessageHandler;
import com.firefly.net.tcp.codec.exception.ProtocolException;
import com.firefly.net.tcp.codec.protocol.*;

/**
 * @author Pengtao Qiu
 */
public class MessageFrameParser extends AbstractByteBufferMessageHandler<MessageFrame> {

    protected byte magic;
    protected FrameType type;
    protected byte version;
    protected boolean endStream;
    protected int streamId;
    protected boolean endFrame;
    protected int payloadLength;
    protected byte[] data;
    protected State state = State.FRAME_HEADER;

    @Override
    protected void parse() {
        parsing:
        while (true) {
            switch (state) {
                case FRAME_HEADER: {
                    switch (parseFrameHeader()) {
                        case UNDERFLOW:
                        case COMPLETE:
                            break parsing;
                        case OVERFLOW:
                            break;
                    }
                }
                break;
                case PAYLOAD: {
                    switch (parsePayload()) {
                        case UNDERFLOW:
                        case COMPLETE:
                            break parsing;
                        case OVERFLOW:
                            break;
                    }
                }
                break;
            }
        }
    }

    protected Result parseFrameHeader() {
        if (buffer.remaining() < minPocketLength()) {
            return Result.UNDERFLOW;
        }

        magic = buffer.get();
        if (magic != Frame.MAGIC) {
            throw new ProtocolException("The frame header format error");
        }

        type = FrameType.from(buffer.get()).orElseThrow(() -> new ProtocolException("not support the frame type"));

        version = buffer.get();
        if (version != Frame.VERSION) {
            throw new ProtocolException("not support the protocol version");
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

    protected Result parsePayload() {
        if (payloadLength == 0) {
            return generateResult();
        }

        if (buffer.remaining() < payloadLength) {
            return Result.UNDERFLOW;
        }

        data = new byte[payloadLength];
        buffer.get(data);
        return generateResult();
    }

    private Result generateResult() {
        switch (type) {
            case CONTROL:
                action.call(new ControlFrame(magic, type, version, endStream, streamId, endFrame, data));
                break;
            case DATA:
                action.call(new DataFrame(magic, type, version, endStream, streamId, endFrame, data));
                break;
        }
        reset();

        if (buffer.hasRemaining()) {
            return Result.OVERFLOW;
        } else {
            return Result.COMPLETE;
        }
    }

    protected void reset() {
        magic = 0;
        type = null;
        version = 0;
        endStream = false;
        streamId = 0;
        endFrame = false;
        payloadLength = 0;
        data = null;
        state = State.FRAME_HEADER;
    }

    protected int minPocketLength() {
        return 9;
    }

    public enum Result {
        UNDERFLOW,
        OVERFLOW,
        COMPLETE
    }

    public enum State {
        FRAME_HEADER,
        PAYLOAD
    }
}
