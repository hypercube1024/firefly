package com.firefly.codec.websocket.decode;

import com.firefly.codec.websocket.stream.impl.WebSocketConnectionImpl;
import com.firefly.net.DecoderChain;
import com.firefly.net.Session;

import java.nio.ByteBuffer;

/**
 * @author Pengtao Qiu
 */
public class WebSocketDecoder extends DecoderChain {

    public WebSocketDecoder() {
        super(null);
    }

    @Override
    public void decode(ByteBuffer buffer, Session session) {
        if (!buffer.hasRemaining()) {
            return;
        }

        WebSocketConnectionImpl webSocketConnection = (WebSocketConnectionImpl) session.getAttachment();
        while (buffer.hasRemaining()) {
            webSocketConnection.getParser().parse(buffer);
        }
    }
}
