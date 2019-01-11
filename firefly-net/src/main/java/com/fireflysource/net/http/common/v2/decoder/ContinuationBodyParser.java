package com.fireflysource.net.http.common.v2.decoder;


import com.fireflysource.net.http.common.model.MetaData;
import com.fireflysource.net.http.common.v2.frame.ErrorCode;
import com.fireflysource.net.http.common.v2.frame.Flags;
import com.fireflysource.net.http.common.v2.frame.HeadersFrame;

import java.nio.ByteBuffer;

public class ContinuationBodyParser extends BodyParser {
    private final HeaderBlockParser headerBlockParser;
    private final HeaderBlockFragments headerBlockFragments;
    private State state = State.PREPARE;
    private int length;

    public ContinuationBodyParser(HeaderParser headerParser, Parser.Listener listener, HeaderBlockParser headerBlockParser, HeaderBlockFragments headerBlockFragments) {
        super(headerParser, listener);
        this.headerBlockParser = headerBlockParser;
        this.headerBlockFragments = headerBlockFragments;
    }

    @Override
    protected void emptyBody(ByteBuffer buffer) {
        if (hasFlag(Flags.END_HEADERS))
            onHeaders();
    }

    @Override
    public boolean parse(ByteBuffer buffer) {
        while (buffer.hasRemaining()) {
            switch (state) {
                case PREPARE: {
                    // SPEC: wrong streamId is treated as connection error.
                    if (getStreamId() == 0)
                        return connectionFailure(buffer, ErrorCode.PROTOCOL_ERROR.code, "invalid_continuation_frame");

                    if (getStreamId() != headerBlockFragments.getStreamId())
                        return connectionFailure(buffer, ErrorCode.PROTOCOL_ERROR.code, "invalid_continuation_stream");

                    length = getBodyLength();
                    state = State.FRAGMENT;
                    break;
                }
                case FRAGMENT: {
                    int remaining = buffer.remaining();
                    if (remaining < length) {
                        headerBlockFragments.storeFragment(buffer, remaining, false);
                        length -= remaining;
                        break;
                    } else {
                        boolean last = hasFlag(Flags.END_HEADERS);
                        headerBlockFragments.storeFragment(buffer, length, last);
                        reset();
                        if (last)
                            return onHeaders();
                        return true;
                    }
                }
                default: {
                    throw new IllegalStateException();
                }
            }
        }
        return false;
    }

    private boolean onHeaders() {
        ByteBuffer headerBlock = headerBlockFragments.complete();
        MetaData metaData = headerBlockParser.parse(headerBlock, headerBlock.remaining());
        if (metaData == HeaderBlockParser.SESSION_FAILURE)
            return false;
        if (metaData == null || metaData == HeaderBlockParser.STREAM_FAILURE)
            return true;
        HeadersFrame frame = new HeadersFrame(getStreamId(), metaData, headerBlockFragments.getPriorityFrame(), headerBlockFragments.isEndStream());
        notifyHeaders(frame);
        return true;
    }

    private void reset() {
        state = State.PREPARE;
        length = 0;
    }

    private enum State {
        PREPARE, FRAGMENT
    }
}
