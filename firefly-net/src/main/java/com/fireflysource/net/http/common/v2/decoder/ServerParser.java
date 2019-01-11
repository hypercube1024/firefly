package com.fireflysource.net.http.common.v2.decoder;

import com.fireflysource.common.io.BufferUtils;
import com.fireflysource.common.slf4j.LazyLogger;
import com.fireflysource.common.sys.SystemLogger;
import com.fireflysource.net.http.common.v2.frame.ErrorCode;
import com.fireflysource.net.http.common.v2.frame.Flags;
import com.fireflysource.net.http.common.v2.frame.FrameType;

import java.nio.ByteBuffer;

public class ServerParser extends Parser {
    private static final LazyLogger LOG = SystemLogger.create(ServerParser.class);

    private final Listener listener;
    private final PrefaceParser prefaceParser;
    private State state = State.PREFACE;
    private boolean notifyPreface = true;

    public ServerParser(Listener listener, int maxDynamicTableSize, int maxHeaderSize) {
        super(listener, maxDynamicTableSize, maxHeaderSize);
        this.listener = listener;
        this.prefaceParser = new PrefaceParser(listener);
    }

    /**
     * <p>A direct upgrade is an unofficial upgrade from HTTP/1.1 to HTTP/2.0.</p>
     * <p>A direct upgrade is initiated when {@code org.eclipse.jetty.server.HttpConnection}
     * sees a request with these bytes:</p>
     * <pre>
     * PRI * HTTP/2.0\r\n
     * \r\n
     * </pre>
     * <p>This request is part of the HTTP/2.0 preface, indicating that a
     * HTTP/2.0 client is attempting a h2c direct connection.</p>
     * <p>This is not a standard HTTP/1.1 Upgrade path.</p>
     */
    public void directUpgrade() {
        if (state != State.PREFACE)
            throw new IllegalStateException();
        prefaceParser.directUpgrade();
    }

    /**
     * <p>The standard HTTP/1.1 upgrade path.</p>
     */
    public void standardUpgrade() {
        if (state != State.PREFACE)
            throw new IllegalStateException();
        notifyPreface = false;
    }

    @Override
    public void parse(ByteBuffer buffer) {
        try {
            if (LOG.isDebugEnabled())
                LOG.debug("Parsing {}", buffer);

            while (true) {
                switch (state) {
                    case PREFACE: {
                        if (!prefaceParser.parse(buffer))
                            return;
                        if (notifyPreface)
                            onPreface();
                        state = State.SETTINGS;
                        break;
                    }
                    case SETTINGS: {
                        if (!parseHeader(buffer))
                            return;
                        if (getFrameType() != FrameType.SETTINGS.getType() || hasFlag(Flags.ACK)) {
                            BufferUtils.clear(buffer);
                            notifyConnectionFailure(ErrorCode.PROTOCOL_ERROR.code, "invalid_preface");
                            return;
                        }
                        if (!parseBody(buffer))
                            return;
                        state = State.FRAMES;
                        break;
                    }
                    case FRAMES: {
                        // Stay forever in the FRAMES state.
                        super.parse(buffer);
                        return;
                    }
                    default: {
                        throw new IllegalStateException();
                    }
                }
            }
        } catch (Throwable x) {
            LOG.debug("http2 server parser exception", x);
            BufferUtils.clear(buffer);
            notifyConnectionFailure(ErrorCode.PROTOCOL_ERROR.code, "parser_error");
        }
    }

    protected void onPreface() {
        notifyPreface();
    }

    private void notifyPreface() {
        try {
            listener.onPreface();
        } catch (Throwable x) {
            LOG.info("Failure while notifying listener " + listener, x);
        }
    }

    public interface Listener extends Parser.Listener {
        void onPreface();

        class Adapter extends Parser.Listener.Adapter implements Listener {
            @Override
            public void onPreface() {
            }
        }

        class Wrapper extends Parser.Listener.Wrapper implements Listener {
            public Wrapper(ServerParser.Listener listener) {
                super(listener);
            }

            @Override
            public ServerParser.Listener getParserListener() {
                return (Listener) super.getParserListener();
            }

            @Override
            public void onPreface() {
                getParserListener().onPreface();
            }
        }
    }

    private enum State {
        PREFACE, SETTINGS, FRAMES
    }
}
