package com.firefly.server.http2;

import com.firefly.codec.common.AbstractConnection;
import com.firefly.codec.http2.decode.HttpParser;
import com.firefly.codec.websocket.decode.WebSocketDecoder;
import com.firefly.net.DecoderChain;
import com.firefly.net.Session;

import java.nio.ByteBuffer;

import static com.firefly.utils.io.BufferUtils.toHeapBuffer;

public class HTTP1ServerDecoder extends DecoderChain {

    private final WebSocketDecoder webSocketDecoder;
    private final HTTP2ServerDecoder http2ServerDecoder;

    public HTTP1ServerDecoder(WebSocketDecoder webSocketDecoder, HTTP2ServerDecoder http2ServerDecoder) {
        super(null);
        this.webSocketDecoder = webSocketDecoder;
        this.http2ServerDecoder = http2ServerDecoder;
    }


    @Override
    public void decode(ByteBuffer buffer, Session session) {
        ByteBuffer buf = toHeapBuffer(buffer);
        AbstractConnection abstractConnection = (AbstractConnection) session.getAttachment();
        switch (abstractConnection.getConnectionType()) {
            case HTTP1: {
                final HTTP1ServerConnection http1Connection = (HTTP1ServerConnection) session.getAttachment();
                if (http1Connection.getTunnelConnectionPromise() == null) {
                    final HttpParser parser = http1Connection.getParser();
                    while (buf.hasRemaining()) {
                        parser.parseNext(buf);
                        if (http1Connection.getUpgradeHTTP2Complete()) {
                            http2ServerDecoder.decode(buf, session);
                            break;
                        } else if (http1Connection.getUpgradeWebSocketComplete()) {
                            webSocketDecoder.decode(buf, session);
                            break;
                        }
                    }
                } else {
                    HTTP1ServerTunnelConnection tunnelConnection = http1Connection.createHTTPTunnel();
                    if (tunnelConnection.content != null) {
                        tunnelConnection.content.call(buf);
                    }
                }
            }
            break;
            case HTTP2: {
                http2ServerDecoder.decode(buf, session);
            }
            break;
            case WEB_SOCKET: {
                webSocketDecoder.decode(buf, session);
            }
            break;
            case HTTP_TUNNEL: {
                HTTP1ServerTunnelConnection tunnelConnection = (HTTP1ServerTunnelConnection) session.getAttachment();
                if (tunnelConnection.content != null) {
                    tunnelConnection.content.call(buf);
                }
            }
            break;
            default:
                throw new IllegalStateException("client does not support the protocol " + abstractConnection.getConnectionType());
        }
    }
}
