package com.firefly.client.http2;

import com.firefly.codec.http2.stream.HTTPConnection;
import com.firefly.net.EncoderChain;
import com.firefly.net.Session;
import com.firefly.utils.concurrent.Callback;

import java.nio.ByteBuffer;

public class ClientSecureEncoder extends EncoderChain {

    @Override
    public void encode(Object message, Session session) throws Throwable {
        HTTPConnection connection = (HTTPConnection) session.getAttachment();

        switch (connection.getHttpVersion()) {
            case HTTP_2:
                HTTP2ClientConnection http2ClientConnection = (HTTP2ClientConnection) connection;
                http2ClientConnection.writeEncryptMessage(message);
                break;
            case HTTP_1_1:
                HTTP1ClientConnection http1ClientConnection = (HTTP1ClientConnection) connection;
                if (message instanceof ByteBuffer) {
                    http1ClientConnection.getSecureSession().write((ByteBuffer) message, Callback.NOOP);
                } else if (message instanceof ByteBuffer[]) {
                    http1ClientConnection.getSecureSession().write((ByteBuffer[]) message, Callback.NOOP);
                } else {
                    throw new IllegalArgumentException(
                            "the http1 encoder must receive the ByteBuffer, but this message type is "
                                    + message.getClass());
                }
                break;
            default:
                throw new IllegalStateException("client does not support the http version " + connection.getHttpVersion());
        }
    }

}
