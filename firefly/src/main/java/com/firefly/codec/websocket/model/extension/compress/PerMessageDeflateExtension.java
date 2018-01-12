package com.firefly.codec.websocket.model.extension.compress;

import com.firefly.codec.websocket.exception.BadPayloadException;
import com.firefly.codec.websocket.frame.Frame;
import com.firefly.codec.websocket.model.ExtensionConfig;
import com.firefly.codec.websocket.model.OpCode;
import com.firefly.utils.concurrent.Callback;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.ByteBuffer;
import java.util.zip.DataFormatException;

/**
 * Per Message Deflate Compression extension for WebSocket.
 * <p>
 * Attempts to follow <a href="https://tools.ietf.org/html/rfc7692">Compression Extensions for WebSocket</a>
 */
public class PerMessageDeflateExtension extends CompressExtension {
    private static Logger LOG = LoggerFactory.getLogger("firefly-system");

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
    protected void nextOutgoingFrame(Frame frame, Callback callback) {
        if (frame.isFin() && !outgoingContextTakeover) {
            LOG.debug("Outgoing Context Reset");
            getDeflater().reset();
        }
        super.nextOutgoingFrame(frame, callback);
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
                        case CLIENT:
                            incomingContextTakeover = false;
                            break;
                        case SERVER:
                            outgoingContextTakeover = false;
                            break;
                    }
                    break;
                }
                case "server_no_context_takeover": {
                    configNegotiated.setParameter("server_no_context_takeover");
                    switch (getPolicy().getBehavior()) {
                        case CLIENT:
                            outgoingContextTakeover = false;
                            break;
                        case SERVER:
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

        LOG.debug("config: outgoingContextTakover={}, incomingContextTakeover={} : {}", outgoingContextTakeover, incomingContextTakeover, this);

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
