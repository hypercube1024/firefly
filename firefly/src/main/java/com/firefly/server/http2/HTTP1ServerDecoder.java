package com.firefly.server.http2;

import com.firefly.codec.http2.decode.HttpParser;
import com.firefly.codec.http2.stream.HTTPConnection;
import com.firefly.net.DecoderChain;
import com.firefly.net.Session;

import java.nio.ByteBuffer;

public class HTTP1ServerDecoder extends DecoderChain {

    public HTTP1ServerDecoder(DecoderChain http2ServerDecoder) {
        super(http2ServerDecoder);
    }

    @Override
    public void decode(ByteBuffer buf, Session session) throws Throwable {
        HTTPConnection connection = (HTTPConnection) session.getAttachment();

        switch (connection.getConnectionType()) {
            case HTTP2: {
                next.decode(buf, session);
            }
            break;
            case HTTP1: {
                final HTTP1ServerConnection http1Connection = (HTTP1ServerConnection) connection;
                if (http1Connection.tunnelConnectionPromise == null) {
                    final HttpParser parser = http1Connection.getParser();
                    while (buf.hasRemaining()) {
                        parser.parseNext(buf);
                    }
                } else {
                    HTTP1ServerTunnelConnection tunnelConnection = http1Connection.createHTTPTunnel();
                    if (tunnelConnection.content != null) {
                        tunnelConnection.content.call(buf);
                    }
                }
            }
            break;
            case HTTP_TUNNEL: {
                HTTP1ServerTunnelConnection tunnelConnection = (HTTP1ServerTunnelConnection) connection;
                if (tunnelConnection.content != null) {
                    tunnelConnection.content.call(buf);
                }
            }
            break;
            default:
                throw new IllegalStateException("server does not support the connection type " + connection.getConnectionType());
        }
    }

}
