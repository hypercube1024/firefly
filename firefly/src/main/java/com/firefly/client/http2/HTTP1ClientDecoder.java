package com.firefly.client.http2;

import com.firefly.codec.common.AbstractConnection;
import com.firefly.codec.http2.decode.HttpParser;
import com.firefly.codec.websocket.decode.WebSocketDecoder;
import com.firefly.net.DecoderChain;
import com.firefly.net.Session;

import java.nio.ByteBuffer;

import static com.firefly.utils.io.BufferUtils.toHeapBuffer;

public class HTTP1ClientDecoder extends DecoderChain {

    private final WebSocketDecoder webSocketDecoder;
    private final HTTP2ClientDecoder http2ClientDecoder;

    public HTTP1ClientDecoder(WebSocketDecoder webSocketDecoder, HTTP2ClientDecoder http2ClientDecoder) {
        super(null);
        this.webSocketDecoder = webSocketDecoder;
        this.http2ClientDecoder = http2ClientDecoder;
    }

    @Override
    public void decode(ByteBuffer buffer, Session session) {
        ByteBuffer buf = toHeapBuffer(buffer);
        AbstractConnection abstractConnection = (AbstractConnection) session.getAttachment();
        switch (abstractConnection.getConnectionType()) {
            case HTTP1: {
                final HTTP1ClientConnection http1Connection = (HTTP1ClientConnection) session.getAttachment();
                final HttpParser parser = http1Connection.getParser();
                while (buf.hasRemaining()) {
                    parser.parseNext(buf);
                    if (http1Connection.getUpgradeHTTP2Complete()) {
                        http2ClientDecoder.decode(buf, session);
                        break;
                    } else if (http1Connection.getUpgradeWebSocketComplete()) {
                        webSocketDecoder.decode(buf, session);
                        break;
                    }
                }
            }
            break;
            case HTTP2: {
                http2ClientDecoder.decode(buf, session);
            }
            break;
            case WEB_SOCKET: {
                webSocketDecoder.decode(buf, session);
            }
            break;
            default:
                throw new IllegalStateException("client does not support the protocol " + abstractConnection.getConnectionType());
        }
    }

}
