package com.firefly.client.websocket;

import com.firefly.client.http2.ClientHTTPHandler;
import com.firefly.client.http2.HTTP2Client;
import com.firefly.client.http2.SimpleHTTPClientConfiguration;
import com.firefly.codec.http2.model.*;
import com.firefly.codec.http2.stream.HTTPConnection;
import com.firefly.codec.http2.stream.HTTPOutputStream;
import com.firefly.codec.websocket.frame.Frame;
import com.firefly.codec.websocket.model.IncomingFrames;
import com.firefly.codec.websocket.stream.AbstractWebSocketBuilder;
import com.firefly.codec.websocket.stream.WebSocketConnection;
import com.firefly.codec.websocket.stream.WebSocketPolicy;
import com.firefly.utils.StringUtils;
import com.firefly.utils.concurrent.Promise;
import com.firefly.utils.function.Action2;
import com.firefly.utils.lang.AbstractLifeCycle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.ByteBuffer;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * @author Pengtao Qiu
 */
public class SimpleWebSocketClient extends AbstractLifeCycle {

    protected static Logger log = LoggerFactory.getLogger("firefly-system");

    private final HTTP2Client http2Client;

    public SimpleWebSocketClient() {
        this(new SimpleHTTPClientConfiguration());
    }

    public SimpleWebSocketClient(SimpleHTTPClientConfiguration http2Configuration) {
        http2Client = new HTTP2Client(http2Configuration);
        start();
    }

    public HandshakeBuilder webSocket(String url) {
        try {
            return webSocket(new URL(url));
        } catch (MalformedURLException e) {
            log.error("url exception", e);
            throw new IllegalArgumentException(e);
        }
    }

    public HandshakeBuilder webSocket(URL url) {
        try {
            HttpURI httpURI = new HttpURI(url.toURI());
            if (!StringUtils.hasText(httpURI.getPath().trim())) {
                httpURI.setPath("/");
            }
            return new HandshakeBuilder(url.getHost(), url.getPort(),
                    new MetaData.Request(HttpMethod.GET.asString(), httpURI, HttpVersion.HTTP_1_1, new HttpFields()));
        } catch (URISyntaxException e) {
            log.error("url exception", e);
            throw new IllegalArgumentException(e);
        }
    }

    public class HandshakeBuilder extends AbstractWebSocketBuilder {

        protected final String host;
        protected final int port;
        protected final MetaData.Request request;
        protected WebSocketPolicy webSocketPolicy;

        public HandshakeBuilder(String host, int port, MetaData.Request request) {
            this.host = host;
            this.port = port;
            this.request = request;
        }

        public HandshakeBuilder addExtension(String extension) {
            request.getFields().add(HttpHeader.SEC_WEBSOCKET_EXTENSIONS, extension);
            return this;
        }

        public HandshakeBuilder putExtension(List<String> values) {
            request.getFields().put(HttpHeader.SEC_WEBSOCKET_EXTENSIONS.asString(), values);
            return this;
        }

        public HandshakeBuilder putSubProtocol(List<String> values) {
            request.getFields().put(HttpHeader.SEC_WEBSOCKET_SUBPROTOCOL.asString(), values);
            return this;
        }

        public HandshakeBuilder policy(WebSocketPolicy webSocketPolicy) {
            this.webSocketPolicy = webSocketPolicy;
            return this;
        }

        public HandshakeBuilder onText(Action2<String, WebSocketConnection> onText) {
            super.onText(onText);
            return this;
        }

        public HandshakeBuilder onData(Action2<ByteBuffer, WebSocketConnection> onData) {
            super.onData(onData);
            return this;
        }

        public HandshakeBuilder onError(Action2<Throwable, WebSocketConnection> onError) {
            super.onError(onError);
            return this;
        }

        public CompletableFuture<WebSocketConnection> connect() {
            return http2Client.connect(host, port).thenCompose(conn -> {
                ClientIncomingFrames clientIncomingFrames = new ClientIncomingFrames() {

                    @Override
                    public void incomingError(Throwable t) {
                        HandshakeBuilder.this.onError(t, webSocketConnection);
                    }

                    @Override
                    public void incomingFrame(Frame frame) {
                        HandshakeBuilder.this.onFrame(frame, webSocketConnection);
                    }
                };
                Promise.Completable<WebSocketConnection> future = new Promise.Completable<>();
                if (webSocketPolicy == null) {
                    webSocketPolicy = WebSocketPolicy.newClientPolicy();
                }
                conn.upgradeWebSocket(request, webSocketPolicy, future, new ClientHTTPHandler.Adapter() {
                    @Override
                    public boolean messageComplete(MetaData.Request request, MetaData.Response response,
                                                   HTTPOutputStream output,
                                                   HTTPConnection connection) {
                        log.info("Upgrade websocket success: {}, {}", response.getStatus(), response.getReason());
                        return true;
                    }
                }, clientIncomingFrames);
                return future.thenApply(webSocketConnection -> {
                    clientIncomingFrames.setWebSocketConnection(webSocketConnection);
                    return webSocketConnection;
                });
            });
        }
    }

    abstract protected class ClientIncomingFrames implements IncomingFrames {

        protected WebSocketConnection webSocketConnection;

        public void setWebSocketConnection(WebSocketConnection webSocketConnection) {
            this.webSocketConnection = webSocketConnection;
        }

    }

    @Override
    protected void init() {
    }

    @Override
    protected void destroy() {
        http2Client.stop();
    }
}
