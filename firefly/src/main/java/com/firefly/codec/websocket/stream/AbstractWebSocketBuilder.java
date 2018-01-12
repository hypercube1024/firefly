package com.firefly.codec.websocket.stream;

import com.firefly.codec.websocket.frame.DataFrame;
import com.firefly.codec.websocket.frame.Frame;
import com.firefly.utils.function.Action2;

import java.nio.ByteBuffer;
import java.util.Optional;

/**
 * @author Pengtao Qiu
 */
abstract public class AbstractWebSocketBuilder {

    protected Action2<String, WebSocketConnection> onText;
    protected Action2<ByteBuffer, WebSocketConnection> onData;
    protected Action2<Throwable, WebSocketConnection> onError;

    public AbstractWebSocketBuilder onText(Action2<String, WebSocketConnection> onText) {
        this.onText = onText;
        return this;
    }

    public AbstractWebSocketBuilder onData(Action2<ByteBuffer, WebSocketConnection> onData) {
        this.onData = onData;
        return this;
    }

    public AbstractWebSocketBuilder onError(Action2<Throwable, WebSocketConnection> onError) {
        this.onError = onError;
        return this;
    }

    public void onFrame(Frame frame, WebSocketConnection connection) {
        switch (frame.getType()) {
            case TEXT:
                Optional.ofNullable(onText).ifPresent(t -> t.call(((DataFrame) frame).getPayloadAsUTF8(), connection));
                break;
            case CONTINUATION:
            case BINARY:
                Optional.ofNullable(onData).ifPresent(d -> d.call(frame.getPayload(), connection));
                break;
        }
    }

    public void onError(Throwable t, WebSocketConnection connection) {
        Optional.ofNullable(onError).ifPresent(e -> e.call(t, connection));
    }
}
