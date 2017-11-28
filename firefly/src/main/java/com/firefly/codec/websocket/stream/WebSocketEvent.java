package com.firefly.codec.websocket.stream;

import com.firefly.codec.websocket.frame.Frame;
import com.firefly.codec.websocket.model.BatchMode;
import com.firefly.codec.websocket.model.CloseInfo;
import com.firefly.codec.websocket.model.IncomingFrames;
import com.firefly.codec.websocket.model.WebSocketPolicy;

import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.nio.ByteBuffer;

public interface WebSocketEvent extends IncomingFrames {
    WebSocketPolicy getPolicy();

    WebSocketSession getSession();

    BatchMode getBatchMode();

    void onBinaryFrame(ByteBuffer buffer, boolean fin) throws IOException;

    void onBinaryMessage(byte[] data);

    void onClose(CloseInfo close);

    void onConnect();

    void onContinuationFrame(ByteBuffer buffer, boolean fin) throws IOException;

    void onError(Throwable t);

    void onFrame(Frame frame);

    void onInputStream(InputStream stream) throws IOException;

    void onPing(ByteBuffer buffer);

    void onPong(ByteBuffer buffer);

    void onReader(Reader reader) throws IOException;

    void onTextFrame(ByteBuffer buffer, boolean fin) throws IOException;

    void onTextMessage(String message);

    void openSession(WebSocketSession session);
}
