package com.firefly.client.http2;

import com.firefly.codec.http2.frame.SettingsFrame;
import com.firefly.codec.http2.model.MetaData.Request;
import com.firefly.codec.http2.stream.HTTPConnection;
import com.firefly.codec.http2.stream.HTTPOutputStream;
import com.firefly.codec.websocket.stream.WebSocketConnection;
import com.firefly.utils.concurrent.Promise;

import java.nio.ByteBuffer;
import java.util.Collection;

public interface HTTPClientConnection extends HTTPConnection {

    void send(Request request, Promise<HTTPOutputStream> promise, ClientHTTPHandler handler);

    void send(Request request, ClientHTTPHandler handler);

    void send(Request request, ByteBuffer buffer, ClientHTTPHandler handler);

    void send(Request request, ByteBuffer[] buffers, ClientHTTPHandler handler);

    void send(Request request, Collection<ByteBuffer> buffers, ClientHTTPHandler handler);

    HTTPOutputStream sendRequestWithContinuation(Request request, ClientHTTPHandler handler);

    HTTPOutputStream getHTTPOutputStream(Request request, ClientHTTPHandler handler);

    void upgradeHTTP2(Request request, SettingsFrame settings,
                      Promise<HTTP2ClientConnection> promise, ClientHTTPHandler handler);

    void upgradeWebSocket(Request request, Promise<WebSocketConnection> promise);
}
