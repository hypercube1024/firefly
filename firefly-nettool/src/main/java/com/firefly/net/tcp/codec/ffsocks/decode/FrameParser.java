package com.firefly.net.tcp.codec.ffsocks.decode;

import com.firefly.net.tcp.codec.AbstractByteBufferMessageHandler;
import com.firefly.net.tcp.codec.exception.ProtocolException;
import com.firefly.net.tcp.codec.ffsocks.protocol.Frame;
import com.firefly.net.tcp.codec.ffsocks.protocol.FrameType;
import com.firefly.utils.Assert;
import com.firefly.utils.lang.Pair;

import java.util.EnumMap;
import java.util.Map;

import static com.firefly.net.tcp.codec.ffsocks.protocol.Frame.FRAME_HEADER_LENGTH;

/**
 * @author Pengtao Qiu
 */
public class FrameParser extends AbstractByteBufferMessageHandler<Frame> {

    protected Map<FrameType, FfsocksParser<? extends Frame>> parserMap = new EnumMap<>(FrameType.class);

    protected Frame frame;
    protected State state = State.HEADER;

    public FrameParser() {
        parserMap.put(FrameType.CONTROL, new MessageFrameParser());
        parserMap.put(FrameType.DATA, new MessageFrameParser());
        parserMap.put(FrameType.PING, new PingFrameParser());
        parserMap.put(FrameType.DISCONNECTION, new DisconnectionFrameParser());
    }

    @Override
    protected void parse() {
        parsing:
        while (true) {
            switch (state) {
                case HEADER: {
                    switch (parseFrameHeader()) {
                        case UNDERFLOW:
                        case COMPLETE:
                            break parsing;
                        case OVERFLOW:
                            break;
                    }
                }
                break;
                case FRAME: {
                    switch (parseFrame()) {
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

    protected FfsocksParser.Result parseFrameHeader() {
        if (buffer.remaining() < minPocketLength()) {
            return FfsocksParser.Result.UNDERFLOW;
        }

        byte magic = buffer.get();
        if (magic != Frame.MAGIC) {
            throw new ProtocolException("The frame header format error");
        }

        FrameType type = FrameType.from(buffer.get()).orElseThrow(() -> new ProtocolException("not support the frame type"));

        byte version = buffer.get();
        if (version != Frame.VERSION) {
            throw new ProtocolException("not support the protocol version");
        }

        frame = new Frame(magic, type, version);
        state = State.FRAME;

        if (buffer.hasRemaining()) {
            return FfsocksParser.Result.OVERFLOW;
        } else {
            return FfsocksParser.Result.COMPLETE;
        }
    }

    protected FfsocksParser.Result parseFrame() {
        Assert.state(frame != null, "The frame header is null");
        FfsocksParser<? extends Frame> parser = getParser(frame.getType());
        if (parser == null) {
            throw new ProtocolException("The frame type is not supported");
        }

        Pair<FfsocksParser.Result, ? extends Frame> pair = parser.parse(buffer, frame);
        switch (pair.first) {
            case UNDERFLOW:
                break;
            case OVERFLOW:
            case COMPLETE:
                action.call(pair.second);
                reset();
                break;
        }
        return pair.first;
    }

    protected FfsocksParser<? extends Frame> getParser(FrameType type) {
        return parserMap.get(type);
    }

    protected int minPocketLength() {
        return FRAME_HEADER_LENGTH;
    }

    protected void reset() {
        frame = null;
        state = State.HEADER;
    }

    public enum State {
        HEADER, FRAME
    }
}
