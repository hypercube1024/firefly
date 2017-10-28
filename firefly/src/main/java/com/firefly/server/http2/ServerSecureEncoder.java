package com.firefly.server.http2;

import com.firefly.codec.http2.stream.HTTPConnection;
import com.firefly.net.EncoderChain;
import com.firefly.net.Session;
import com.firefly.utils.concurrent.Callback;

import java.nio.ByteBuffer;

public class ServerSecureEncoder extends EncoderChain {

    @Override
    public void encode(Object message, Session session) throws Throwable {
        HTTPConnection connection = (HTTPConnection) session.getAttachment();

        switch (connection.getHttpVersion()) {
            case HTTP_2: {
                HTTP2ServerConnection http2ServerConnection = (HTTP2ServerConnection) connection;
                http2ServerConnection.writeEncryptMessage(message);
            }
            break;
            case HTTP_1_1:
                if (message instanceof ByteBuffer) {
                    HTTP1ServerConnection http1ServerConnection = (HTTP1ServerConnection) connection;
                    http1ServerConnection.getSecureSession().write((ByteBuffer) message, Callback.NOOP);
                } else {
                    throw new IllegalArgumentException(
                            "the http1 encoder must receive the ByteBuffer, but this message type is "
                                    + message.getClass());
                }
                break;
            default:
                throw new IllegalStateException("server does not support the http version " + connection.getHttpVersion());
        }
    }

}
