package com.firefly.client.http2;

import com.firefly.codec.http2.decode.HttpParser;
import com.firefly.codec.http2.stream.HTTPConnection;
import com.firefly.net.DecoderChain;
import com.firefly.net.Session;

import java.nio.ByteBuffer;

import static com.firefly.utils.io.BufferUtils.toHeapBuffer;

public class HTTP1ClientDecoder extends DecoderChain {

    public HTTP1ClientDecoder(DecoderChain next) {
        super(next);
    }

    @Override
    public void decode(ByteBuffer buffer, Session session) throws Throwable {
        HTTPConnection connection = (HTTPConnection) session.getAttachment();
        ByteBuffer buf = toHeapBuffer(buffer);

        switch (connection.getHttpVersion()) {
            case HTTP_2:
                next.decode(buf, session);
                break;
            case HTTP_1_1:
                final HTTP1ClientConnection http1Connection = (HTTP1ClientConnection) connection;
                final HttpParser parser = http1Connection.getParser();
                while (buf.hasRemaining()) {
                    parser.parseNext(buf);
                    if (http1Connection.getUpgradeHTTP2Complete()) {
                        next.decode(buf, session);
                        break;
                    }
                }
                break;
            default:
                throw new IllegalStateException("client does not support the http version " + connection.getHttpVersion());
        }
    }

}
