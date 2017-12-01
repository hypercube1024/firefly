package com.firefly.client.http2;

import com.firefly.codec.http2.stream.HTTPConnection;
import com.firefly.net.EncoderChain;
import com.firefly.net.Session;
import com.firefly.utils.concurrent.Callback;

import java.nio.ByteBuffer;

public class HTTP1ClientEncoder extends EncoderChain {

    public HTTP1ClientEncoder(EncoderChain next) {
        super(next);
    }

    @Override
    public void encode(Object message, Session session) throws Throwable {
        HTTPConnection connection = (HTTPConnection) session.getAttachment();

        switch (connection.getHttpVersion()) {
            case HTTP_2:
                next.encode(message, session);
                break;
            case HTTP_1_1:
                if (connection.isEncrypted()) {
                    next.encode(message, session);
                } else {
                    if (message instanceof ByteBuffer) {
                        session.write((ByteBuffer) message, Callback.NOOP);
                    } else if (message instanceof ByteBuffer[]) {
                        session.write((ByteBuffer[]) message, Callback.NOOP);
                    } else {
                        throw new IllegalArgumentException(
                                "the http1 encoder must receive the ByteBuffer, but this message type is "
                                        + message.getClass());
                    }
                }
                break;
            default:
                throw new IllegalArgumentException("client does not support the http version " + connection.getHttpVersion());
        }
    }

}
