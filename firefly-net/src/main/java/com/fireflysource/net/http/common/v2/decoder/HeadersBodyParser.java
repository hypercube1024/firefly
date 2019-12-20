package com.fireflysource.net.http.common.v2.decoder;

import com.fireflysource.common.io.BufferUtils;
import com.fireflysource.net.http.common.model.MetaData;
import com.fireflysource.net.http.common.v2.frame.*;

import java.nio.ByteBuffer;

public class HeadersBodyParser extends BodyParser {
    private final HeaderBlockParser headerBlockParser;
    private final HeaderBlockFragments headerBlockFragments;
    private State state = State.PREPARE;
    private int cursor;
    private int length;
    private int paddingLength;
    private boolean exclusive;
    private int parentStreamId;
    private int weight;

    public HeadersBodyParser(HeaderParser headerParser, Parser.Listener listener, HeaderBlockParser headerBlockParser, HeaderBlockFragments headerBlockFragments) {
        super(headerParser, listener);
        this.headerBlockParser = headerBlockParser;
        this.headerBlockFragments = headerBlockFragments;
    }

    private void reset() {
        state = State.PREPARE;
        cursor = 0;
        length = 0;
        paddingLength = 0;
        exclusive = false;
        parentStreamId = 0;
        weight = 0;
    }

    @Override
    protected void emptyBody(ByteBuffer buffer) {
        if (hasFlag(Flags.END_HEADERS)) {
            MetaData metaData = headerBlockParser.parse(BufferUtils.EMPTY_BUFFER, 0);
            onHeaders(0, 0, false, metaData);
        } else {
            headerBlockFragments.setStreamId(getStreamId());
            headerBlockFragments.setEndStream(isEndStream());
            if (hasFlag(Flags.PRIORITY))
                connectionFailure(buffer, ErrorCode.PROTOCOL_ERROR.code, "invalid_headers_priority_frame");
        }
    }

    @Override
    public boolean parse(ByteBuffer buffer) {
        boolean loop = false;
        while (buffer.hasRemaining() || loop) {
            switch (state) {
                case PREPARE: {
                    // SPEC: wrong streamId is treated as connection error.
                    if (getStreamId() == 0) {
                        return connectionFailure(buffer, ErrorCode.PROTOCOL_ERROR.code, "invalid_headers_frame");
                    }

                    length = getBodyLength();

                    if (isPadding())
                        state = State.PADDING_LENGTH;
                    else if (hasFlag(Flags.PRIORITY))
                        state = State.EXCLUSIVE;
                    else
                        state = State.HEADERS;
                    break;
                }
                case PADDING_LENGTH: {
                    paddingLength = buffer.get() & 0xFF;
                    --length;
                    length -= paddingLength;
                    state = hasFlag(Flags.PRIORITY) ? State.EXCLUSIVE : State.HEADERS;
                    loop = length == 0;
                    if (length < 0)
                        return connectionFailure(buffer, ErrorCode.FRAME_SIZE_ERROR.code, "invalid_headers_frame_padding");
                    break;
                }
                case EXCLUSIVE: {
                    // We must only peek the first byte and not advance the buffer
                    // because the 31 least significant bits represent the stream id.
                    int currByte = buffer.get(buffer.position());
                    exclusive = (currByte & 0x80) == 0x80;
                    state = State.PARENT_STREAM_ID;
                    break;
                }
                case PARENT_STREAM_ID: {
                    if (buffer.remaining() >= 4) {
                        parentStreamId = buffer.getInt();
                        parentStreamId &= 0x7F_FF_FF_FF;
                        length -= 4;
                        state = State.WEIGHT;
                        if (length < 1)
                            return connectionFailure(buffer, ErrorCode.FRAME_SIZE_ERROR.code, "invalid_headers_frame");
                    } else {
                        state = State.PARENT_STREAM_ID_BYTES;
                        cursor = 4;
                    }
                    break;
                }
                case PARENT_STREAM_ID_BYTES: {
                    int currByte = buffer.get() & 0xFF;
                    --cursor;
                    parentStreamId += currByte << (8 * cursor);
                    --length;
                    if (cursor > 0 && length <= 0)
                        return connectionFailure(buffer, ErrorCode.FRAME_SIZE_ERROR.code, "invalid_headers_frame");
                    if (cursor == 0) {
                        parentStreamId &= 0x7F_FF_FF_FF;
                        state = State.WEIGHT;
                        if (length < 1)
                            return connectionFailure(buffer, ErrorCode.FRAME_SIZE_ERROR.code, "invalid_headers_frame");
                    }
                    break;
                }
                case WEIGHT: {
                    // SPEC: stream cannot depend on itself.
                    if (getStreamId() == parentStreamId)
                        return connectionFailure(buffer, ErrorCode.PROTOCOL_ERROR.code, "invalid_priority_frame");
                    weight = (buffer.get() & 0xFF) + 1;
                    --length;
                    state = State.HEADERS;
                    loop = length == 0;
                    break;
                }
                case HEADERS: {
                    if (hasFlag(Flags.END_HEADERS)) {
                        MetaData metaData = headerBlockParser.parse(buffer, length);
                        if (metaData == HeaderBlockParser.SESSION_FAILURE)
                            return false;
                        if (metaData != null) {
                            if (LOG.isDebugEnabled())
                                LOG.debug("Parsed {} frame hpack from {}", FrameType.HEADERS, buffer);
                            state = State.PADDING;
                            loop = paddingLength == 0;
                            if (metaData != HeaderBlockParser.STREAM_FAILURE)
                                onHeaders(parentStreamId, weight, exclusive, metaData);
                        }
                    } else {
                        int remaining = buffer.remaining();
                        if (remaining < length) {
                            headerBlockFragments.storeFragment(buffer, remaining, false);
                            length -= remaining;
                        } else {
                            headerBlockFragments.setStreamId(getStreamId());
                            headerBlockFragments.setEndStream(isEndStream());
                            if (hasFlag(Flags.PRIORITY))
                                headerBlockFragments.setPriorityFrame(new PriorityFrame(getStreamId(), parentStreamId, weight, exclusive));
                            headerBlockFragments.storeFragment(buffer, length, false);
                            state = State.PADDING;
                            loop = paddingLength == 0;
                        }
                    }
                    break;
                }
                case PADDING: {
                    int size = Math.min(buffer.remaining(), paddingLength);
                    buffer.position(buffer.position() + size);
                    paddingLength -= size;
                    if (paddingLength == 0) {
                        reset();
                        return true;
                    }
                    break;
                }
                default: {
                    throw new IllegalStateException();
                }
            }
        }
        return false;
    }

    private void onHeaders(int parentStreamId, int weight, boolean exclusive, MetaData metaData) {
        PriorityFrame priorityFrame = null;
        if (hasFlag(Flags.PRIORITY))
            priorityFrame = new PriorityFrame(getStreamId(), parentStreamId, weight, exclusive);
        HeadersFrame frame = new HeadersFrame(getStreamId(), metaData, priorityFrame, isEndStream());
        notifyHeaders(frame);
    }

    private enum State {
        PREPARE, PADDING_LENGTH, EXCLUSIVE, PARENT_STREAM_ID, PARENT_STREAM_ID_BYTES, WEIGHT, HEADERS, PADDING
    }
}
