package com.fireflysource.net.websocket.common.extension.compress;

import com.fireflysource.common.slf4j.LazyLogger;
import com.fireflysource.common.sys.Result;
import com.fireflysource.common.sys.SystemLogger;
import com.fireflysource.net.websocket.common.exception.BadPayloadException;
import com.fireflysource.net.websocket.common.exception.ProtocolException;
import com.fireflysource.net.websocket.common.frame.Frame;
import com.fireflysource.net.websocket.common.model.ExtensionConfig;
import com.fireflysource.net.websocket.common.model.OpCode;
import com.fireflysource.net.websocket.common.model.WebSocketBehavior;

import java.nio.ByteBuffer;
import java.util.function.Consumer;
import java.util.zip.DataFormatException;

/**
 * Per Message Deflate Compression extension for WebSocket.
 * <p>
 * Attempts to follow <a href="https://tools.ietf.org/html/rfc7692">Compression Extensions for WebSocket</a>
 */
public class PerMessageDeflateExtension extends CompressExtension {

    private static LazyLogger LOG = SystemLogger.create(PerMessageDeflateExtension.class);

    private ExtensionConfig configRequested;
    private ExtensionConfig configNegotiated;
    private boolean incomingContextTakeover = true;
    private boolean outgoingContextTakeover = true;
    private boolean incomingCompressed;

    @Override
    public String getName() {
        return "permessage-deflate";
    }

    @Override
    public void incomingFrame(Frame frame) {
        // Incoming frames are always non concurrent because
        // they are read and parsed with a single thread, and
        // therefore there is no need for synchronization.

        // This extension requires the RSV1 bit set only in the first frame.
        // Subsequent continuation frames don't have RSV1 set, but are compressed.
        if (frame.getType().isData()) {
            incomingCompressed = frame.isRsv1();
        }

        if (OpCode.isControlFrame(frame.getOpCode()) || !incomingCompressed) {
            nextIncomingFrame(frame);
            return;
        }

        if (frame.getOpCode() == OpCode.CONTINUATION && frame.isRsv1()) {
            // Per RFC7692 we MUST Fail the websocket connection
            throw new ProtocolException("Invalid RSV1 set on permessage-deflate CONTINUATION frame");
        }

        ByteAccumulator accumulator = newByteAccumulator();

        try {
            ByteBuffer payload = frame.getPayload();
            decompress(accumulator, payload);
            if (frame.isFin()) {
                decompress(accumulator, TAIL_BYTES_BUF.slice());
            }

            forwardIncoming(frame, accumulator);
        } catch (DataFormatException e) {
            throw new BadPayloadException(e);
        }

        if (frame.isFin())
            incomingCompressed = false;
    }

    @Override
    protected void nextIncomingFrame(Frame frame) {
        if (frame.isFin() && !incomingContextTakeover) {
            LOG.debug("Incoming Context Reset");
            decompressCount.set(0);
            getInflater().reset();
        }
        super.nextIncomingFrame(frame);
    }

    @Override
    protected void nextOutgoingFrame(Frame frame, Consumer<Result<Void>> result) {
        if (frame.isFin() && !outgoingContextTakeover) {
            LOG.debug("Outgoing Context Reset");
            getDeflater().reset();
        }
        super.nextOutgoingFrame(frame, result);
    }

    @Override
    int getRsvUseMode() {
        return RSV_USE_ONLY_FIRST;
    }

    @Override
    int getTailDropMode() {
        return TAIL_DROP_FIN_ONLY;
    }

    @Override
    public void setConfig(final ExtensionConfig config) {
        configRequested = new ExtensionConfig(config);
        configNegotiated = new ExtensionConfig(config.getName());

        for (String key : config.getParameterKeys()) {
            key = key.trim();
            switch (key) {
                case "client_max_window_bits":
                case "server_max_window_bits": {
                    // Don't negotiate these parameters
                    break;
                }
                case "client_no_context_takeover": {
                    configNegotiated.setParameter("client_no_context_takeover");
                    switch (getPolicy().getBehavior()) {
                        case WebSocketBehavior.CLIENT:
                            incomingContextTakeover = false;
                            break;
                        case WebSocketBehavior.SERVER:
                            outgoingContextTakeover = false;
                            break;
                    }
                    break;
                }
                case "server_no_context_takeover": {
                    configNegotiated.setParameter("server_no_context_takeover");
                    switch (getPolicy().getBehavior()) {
                        case WebSocketBehavior.CLIENT:
                            outgoingContextTakeover = false;
                            break;
                        case WebSocketBehavior.SERVER:
                            incomingContextTakeover = false;
                            break;
                    }
                    break;
                }
                default: {
                    throw new IllegalArgumentException();
                }
            }
        }

        LOG.debug("config: outgoingContextTakeover={}, incomingContextTakeover={} : {}", outgoingContextTakeover, incomingContextTakeover, this);

        super.setConfig(configNegotiated);
    }

    @Override
    public String toString() {
        return String.format("%s[requested=\"%s\", negotiated=\"%s\"]",
                getClass().getSimpleName(),
                configRequested.getParameterizedName(),
                configNegotiated.getParameterizedName());
    }
}
