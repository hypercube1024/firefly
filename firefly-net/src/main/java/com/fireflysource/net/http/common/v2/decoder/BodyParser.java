package com.fireflysource.net.http.common.v2.decoder;

import com.fireflysource.common.io.BufferUtils;
import com.fireflysource.common.slf4j.LazyLogger;
import com.fireflysource.common.sys.SystemLogger;
import com.fireflysource.net.http.common.v2.frame.*;

import java.nio.ByteBuffer;

/**
 * <p>The base parser for the frame body of HTTP/2 frames.</p>
 * <p>Subclasses implement {@link #parse(ByteBuffer)} to parse
 * the frame specific body.</p>
 *
 * @see Parser
 */
public abstract class BodyParser {
    public static final LazyLogger LOG = SystemLogger.create(BodyParser.class);

    private final HeaderParser headerParser;
    private final Parser.Listener listener;

    protected BodyParser(HeaderParser headerParser, Parser.Listener listener) {
        this.headerParser = headerParser;
        this.listener = listener;
    }

    /**
     * <p>Parses the body bytes in the given {@code buffer}; only the body
     * bytes are consumed, therefore when this method returns, the buffer
     * may contain unconsumed bytes.</p>
     *
     * @param buffer the buffer to parse
     * @return true if the whole body bytes were parsed, false if not enough
     * body bytes were present in the buffer
     */
    public abstract boolean parse(ByteBuffer buffer);

    protected void emptyBody(ByteBuffer buffer) {
        connectionFailure(buffer, ErrorCode.PROTOCOL_ERROR.code, "invalid_frame");
    }

    protected boolean hasFlag(int bit) {
        return headerParser.hasFlag(bit);
    }

    protected boolean isPadding() {
        return headerParser.hasFlag(Flags.PADDING);
    }

    protected boolean isEndStream() {
        return headerParser.hasFlag(Flags.END_STREAM);
    }

    protected int getStreamId() {
        return headerParser.getStreamId();
    }

    protected int getBodyLength() {
        return headerParser.getLength();
    }

    protected void notifyData(DataFrame frame) {
        try {
            listener.onData(frame);
        } catch (Throwable x) {
            LOG.info("Failure while notifying listener " + listener, x);
        }
    }

    protected void notifyHeaders(HeadersFrame frame) {
        try {
            listener.onHeaders(frame);
        } catch (Throwable x) {
            LOG.info("Failure while notifying listener " + listener, x);
        }
    }

    protected void notifyPriority(PriorityFrame frame) {
        try {
            listener.onPriority(frame);
        } catch (Throwable x) {
            LOG.info("Failure while notifying listener " + listener, x);
        }
    }

    protected void notifyReset(ResetFrame frame) {
        try {
            listener.onReset(frame);
        } catch (Throwable x) {
            LOG.info("Failure while notifying listener " + listener, x);
        }
    }

    protected void notifySettings(SettingsFrame frame) {
        try {
            listener.onSettings(frame);
        } catch (Throwable x) {
            LOG.info("Failure while notifying listener " + listener, x);
        }
    }

    protected void notifyPushPromise(PushPromiseFrame frame) {
        try {
            listener.onPushPromise(frame);
        } catch (Throwable x) {
            LOG.info("Failure while notifying listener " + listener, x);
        }
    }

    protected void notifyPing(PingFrame frame) {
        try {
            listener.onPing(frame);
        } catch (Throwable x) {
            LOG.info("Failure while notifying listener " + listener, x);
        }
    }

    protected void notifyGoAway(GoAwayFrame frame) {
        try {
            listener.onGoAway(frame);
        } catch (Throwable x) {
            LOG.info("Failure while notifying listener " + listener, x);
        }
    }

    protected void notifyWindowUpdate(WindowUpdateFrame frame) {
        try {
            listener.onWindowUpdate(frame);
        } catch (Throwable x) {
            LOG.info("Failure while notifying listener " + listener, x);
        }
    }

    protected boolean connectionFailure(ByteBuffer buffer, int error, String reason) {
        BufferUtils.clear(buffer);
        notifyConnectionFailure(error, reason);
        return false;
    }

    private void notifyConnectionFailure(int error, String reason) {
        try {
            listener.onConnectionFailure(error, reason);
        } catch (Throwable x) {
            LOG.info("Failure while notifying listener " + listener, x);
        }
    }

    protected void streamFailure(int streamId, int error, String reason) {
        notifyStreamFailure(streamId, error, reason);
    }

    private void notifyStreamFailure(int streamId, int error, String reason) {
        try {
            listener.onStreamFailure(streamId, error, reason);
        } catch (Throwable x) {
            LOG.info("Failure while notifying listener " + listener, x);
        }
    }
}
