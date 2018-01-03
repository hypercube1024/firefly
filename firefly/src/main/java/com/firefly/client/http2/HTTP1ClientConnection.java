package com.firefly.client.http2;

import com.firefly.codec.http2.decode.HttpParser;
import com.firefly.codec.http2.decode.HttpParser.RequestHandler;
import com.firefly.codec.http2.decode.HttpParser.ResponseHandler;
import com.firefly.codec.http2.encode.HttpGenerator;
import com.firefly.codec.http2.frame.SettingsFrame;
import com.firefly.codec.http2.model.*;
import com.firefly.codec.http2.model.MetaData.Request;
import com.firefly.codec.http2.stream.*;
import com.firefly.codec.http2.stream.Session.Listener;
import com.firefly.codec.websocket.model.IncomingFrames;
import com.firefly.codec.websocket.model.WebSocketBehavior;
import com.firefly.codec.websocket.stream.WebSocketConnection;
import com.firefly.codec.websocket.stream.WebSocketPolicy;
import com.firefly.codec.websocket.stream.impl.WebSocketConnectionImpl;
import com.firefly.net.SecureSession;
import com.firefly.net.Session;
import com.firefly.utils.Assert;
import com.firefly.utils.codec.B64Code;
import com.firefly.utils.codec.Base64Utils;
import com.firefly.utils.concurrent.Promise;
import com.firefly.utils.io.BufferUtils;
import com.firefly.utils.io.IO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.WritePendingException;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import static com.firefly.codec.websocket.model.WebSocketConstants.SPEC_VERSION;

public class HTTP1ClientConnection extends AbstractHTTP1Connection implements HTTPClientConnection {

    private static final Logger log = LoggerFactory.getLogger("firefly-system");

    private Promise<WebSocketConnection> webSocketConnectionPromise;
    private IncomingFrames incomingFrames;
    private WebSocketPolicy policy;
    private Promise<HTTP2ClientConnection> http2ConnectionPromise;
    private volatile HTTP2ClientConnection http2Connection;
    private ClientHTTP2SessionListener http2SessionListener;
    private final AtomicBoolean upgradeHTTP2Complete = new AtomicBoolean(false);
    private final AtomicBoolean upgradeWebSocketComplete = new AtomicBoolean(false);
    private final ResponseHandlerWrap wrap;

    private static class ResponseHandlerWrap implements ResponseHandler {

        private final AtomicReference<HTTP1ClientResponseHandler> writing = new AtomicReference<>();
        private int status;
        private String reason;
        private HTTP1ClientConnection connection;

        @Override
        public void earlyEOF() {
            HTTP1ClientResponseHandler h = writing.getAndSet(null);
            if (h != null) {
                h.earlyEOF();
            } else {
                IO.close(connection);
            }
        }


        @Override
        public void parsedHeader(HttpField field) {
            writing.get().parsedHeader(field);
        }

        @Override
        public boolean headerComplete() {
            return writing.get().headerComplete();
        }

        @Override
        public boolean content(ByteBuffer item) {
            return writing.get().content(item);
        }

        @Override
        public boolean contentComplete() {
            return writing.get().contentComplete();
        }

        @Override
        public void parsedTrailer(HttpField field) {
            writing.get().parsedTrailer(field);
        }

        @Override
        public boolean messageComplete() {
            if (status == 100 && "Continue".equalsIgnoreCase(reason)) {
                log.debug("client received the 100 Continue response");
                connection.getParser().reset();
                return true;
            } else {
                return writing.getAndSet(null).messageComplete();
            }
        }

        @Override
        public void badMessage(int status, String reason) {
            HTTP1ClientResponseHandler h = writing.getAndSet(null);
            if (h != null) {
                h.badMessage(status, reason);
            } else {
                IO.close(connection);
            }
        }

        @Override
        public int getHeaderCacheSize() {
            return 1024;
        }

        @Override
        public boolean startResponse(HttpVersion version, int status, String reason) {
            this.status = status;
            this.reason = reason;
            return writing.get().startResponse(version, status, reason);
        }

    }

    public HTTP1ClientConnection(HTTP2Configuration config, Session tcpSession, SecureSession secureSession) {
        this(config, secureSession, tcpSession, new ResponseHandlerWrap());
    }

    private HTTP1ClientConnection(HTTP2Configuration config, SecureSession secureSession, Session tcpSession,
                                  ResponseHandler responseHandler) {
        super(config, secureSession, tcpSession, null, responseHandler);
        wrap = (ResponseHandlerWrap) responseHandler;
        wrap.connection = this;
    }

    @Override
    protected HttpParser initHttpParser(HTTP2Configuration config, RequestHandler requestHandler,
                                        ResponseHandler responseHandler) {
        return new HttpParser(responseHandler, config.getMaxRequestHeadLength());
    }

    HttpParser getParser() {
        return parser;
    }

    SecureSession getSecureSession() {
        return secureSession;
    }

    HTTP2Configuration getHTTP2Configuration() {
        return config;
    }

    Session getTcpSession() {
        return tcpSession;
    }

    @Override
    public void upgradeHTTP2(Request request, SettingsFrame settings, Promise<HTTP2ClientConnection> promise,
                             ClientHTTPHandler upgradeHandler,
                             ClientHTTPHandler http2ResponseHandler) {
        Promise<Stream> initStream = new HTTP2ClientResponseHandler.ClientStreamPromise(request, new Promise<HTTPOutputStream>() {

            @Override
            public void failed(Throwable x) {
                log.error("Create client output stream exception", x);
            }
        });
        Stream.Listener initStreamListener = new HTTP2ClientResponseHandler(request, http2ResponseHandler, this);
        ClientHTTP2SessionListener listener = new ClientHTTP2SessionListener() {

            @Override
            public Map<Integer, Integer> onPreface(com.firefly.codec.http2.stream.Session session) {
                return settings.getSettings();
            }

        };
        upgradeHTTP2(request, settings, promise, initStream, initStreamListener, listener, upgradeHandler);
    }

    public void upgradeHTTP2(Request request, SettingsFrame settings,
                             Promise<HTTP2ClientConnection> promise, Promise<Stream> initStream,
                             Stream.Listener initStreamListener, ClientHTTP2SessionListener listener,
                             ClientHTTPHandler handler) {
        if (isEncrypted()) {
            throw new IllegalStateException("The TLS TCP connection must use ALPN to upgrade HTTP2");
        }

        this.http2ConnectionPromise = promise;
        this.http2SessionListener = listener;
        http2Connection = new HTTP2ClientConnection(getHTTP2Configuration(),
                getTcpSession(), null, http2SessionListener) {
            @Override
            protected HTTP2Session initHTTP2Session(HTTP2Configuration config, FlowControlStrategy flowControl,
                                                    Listener listener) {
                return HTTP2ClientSession.initSessionForUpgradingHTTP2(scheduler, this.tcpSession, generator,
                        listener, flowControl, 3, config.getStreamIdleTimeout(), initStream,
                        initStreamListener);
            }
        };

        // generate http2 upgrading headers
        request.getFields().add(new HttpField(HttpHeader.CONNECTION, "Upgrade, HTTP2-Settings"));
        request.getFields().add(new HttpField(HttpHeader.UPGRADE, "h2c"));
        if (settings != null) {
            List<ByteBuffer> byteBuffers = http2Generator.control(settings);
            if (byteBuffers != null && byteBuffers.size() > 0) {
                try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
                    for (ByteBuffer buffer : byteBuffers) {
                        out.write(BufferUtils.toArray(buffer));
                    }
                    byte[] settingsFrame = out.toByteArray();
                    byte[] settingsPayload = new byte[settingsFrame.length - 9];
                    System.arraycopy(settingsFrame, 9, settingsPayload, 0, settingsPayload.length);

                    request.getFields().add(new HttpField(HttpHeader.HTTP2_SETTINGS, Base64Utils.encodeToUrlSafeString(settingsPayload)));
                } catch (IOException e) {
                    log.error("generate http2 upgrading settings exception", e);
                }
            } else {
                request.getFields().add(new HttpField(HttpHeader.HTTP2_SETTINGS, ""));
            }
        } else {
            request.getFields().add(new HttpField(HttpHeader.HTTP2_SETTINGS, ""));
        }

        send(request, handler);
    }

    boolean upgradeProtocolComplete(MetaData.Response response) {
        switch (Protocol.from(response)) {
            case H2: {
                if (http2ConnectionPromise != null && http2SessionListener != null && http2Connection != null) {
                    upgradeHTTP2Complete.compareAndSet(false, true);
                    getTcpSession().attachObject(http2Connection);
                    http2SessionListener.setConnection(http2Connection);
                    http2Connection.initialize(getHTTP2Configuration(), http2ConnectionPromise, http2SessionListener);
                    return true;
                } else {
                    resetUpgradeProtocol();
                    return false;
                }
            }
            case WEB_SOCKET: {
                if (webSocketConnectionPromise != null && incomingFrames != null && policy != null) {
                    upgradeWebSocketComplete.compareAndSet(false, true);
                    WebSocketConnection webSocketConnection = new WebSocketConnectionImpl(secureSession, tcpSession, incomingFrames, policy);
                    getTcpSession().attachObject(webSocketConnection);
                    webSocketConnectionPromise.succeeded(webSocketConnection);
                    return true;
                } else {
                    resetUpgradeProtocol();
                    return false;
                }
            }
            default:
                resetUpgradeProtocol();
                return false;
        }
    }

    private void resetUpgradeProtocol() {
        http2ConnectionPromise = null;
        http2SessionListener = null;
        http2Connection = null;
        webSocketConnectionPromise = null;
        incomingFrames = null;
        policy = null;
    }

    @Override
    public void upgradeWebSocket(Request request, WebSocketPolicy policy, Promise<WebSocketConnection> promise,
                                 ClientHTTPHandler upgradeHandler, IncomingFrames incomingFrames) {
        Assert.isTrue(HttpMethod.GET.is(request.getMethod()), "The method of the request MUST be GET in the websocket handshake.");
        Assert.isTrue(policy.getBehavior() == WebSocketBehavior.CLIENT, "The websocket behavior MUST be client");

        request.getFields().put(HttpHeader.SEC_WEBSOCKET_VERSION, String.valueOf(SPEC_VERSION));
        request.getFields().put(HttpHeader.UPGRADE, "websocket");
        request.getFields().put(HttpHeader.CONNECTION, "Upgrade");
        request.getFields().put(HttpHeader.SEC_WEBSOCKET_KEY, genRandomKey());
        webSocketConnectionPromise = promise;
        this.incomingFrames = incomingFrames;
        this.policy = policy;
        send(request, upgradeHandler);
    }

    private String genRandomKey() {
        byte[] bytes = new byte[16];
        ThreadLocalRandom.current().nextBytes(bytes);
        return new String(B64Code.encode(bytes));
    }

    @Override
    public HTTPOutputStream sendRequestWithContinuation(Request request, ClientHTTPHandler handler) {
        request.getFields().put(HttpHeader.EXPECT, HttpHeaderValue.CONTINUE);
        HTTPOutputStream outputStream = getHTTPOutputStream(request, handler);
        try {
            outputStream.commit();
        } catch (IOException e) {
            log.error("client generates the HTTP message exception", e);
        }
        return outputStream;
    }

    @Override
    public void send(Request request, ClientHTTPHandler handler) {
        try (HTTPOutputStream output = getHTTPOutputStream(request, handler)) {
            log.debug("client request and does not send data");
        } catch (IOException e) {
            log.error("client generates the HTTP message exception", e);
        }
    }

    @Override
    public void send(Request request, ByteBuffer buffer, ClientHTTPHandler handler) {
        send(request, Collections.singleton(buffer), handler);
    }

    @Override
    public void send(Request request, ByteBuffer[] buffers, ClientHTTPHandler handler) {
        send(request, Arrays.asList(buffers), handler);
    }

    @Override
    public void send(Request request, Collection<ByteBuffer> buffers, ClientHTTPHandler handler) {
        try (HTTPOutputStream output = getHTTPOutputStream(request, handler)) {
            if (buffers != null) {
                output.writeWithContentLength(buffers);
            }
        } catch (IOException e) {
            log.error("client generates the HTTP message exception", e);
        }
    }

    @Override
    public HTTPOutputStream getHTTPOutputStream(Request request, ClientHTTPHandler handler) {
        HTTP1ClientResponseHandler http1ClientResponseHandler = new HTTP1ClientResponseHandler(handler);
        checkWrite(request, http1ClientResponseHandler);
        http1ClientResponseHandler.outputStream = new HTTP1ClientRequestOutputStream(this, wrap.writing.get().request);
        return http1ClientResponseHandler.outputStream;
    }

    @Override
    public void send(Request request, Promise<HTTPOutputStream> promise, ClientHTTPHandler handler) {
        promise.succeeded(getHTTPOutputStream(request, handler));
    }

    static class HTTP1ClientRequestOutputStream extends AbstractHTTP1OutputStream {

        private final HTTP1ClientConnection connection;
        private final HttpGenerator httpGenerator;

        private HTTP1ClientRequestOutputStream(HTTP1ClientConnection connection, Request request) {
            super(request, true);
            this.connection = connection;
            httpGenerator = new HttpGenerator();
        }

        @Override
        protected void generateHTTPMessageSuccessfully() {
            log.debug("client session {} generates the HTTP message completely", connection.tcpSession.getSessionId());
        }

        @Override
        protected void generateHTTPMessageExceptionally(HttpGenerator.Result actualResult,
                                                        HttpGenerator.State actualState,
                                                        HttpGenerator.Result expectedResult,
                                                        HttpGenerator.State expectedState) {
            log.error("http1 generator error, actual: [{}, {}], expected: [{}, {}]", actualResult, actualState, expectedResult, expectedState);
            throw new IllegalStateException("client generates http message exception.");
        }

        @Override
        protected ByteBuffer getHeaderByteBuffer() {
            return BufferUtils.allocate(connection.getHTTP2Configuration().getMaxRequestHeadLength());
        }

        @Override
        protected ByteBuffer getTrailerByteBuffer() {
            return BufferUtils.allocate(connection.getHTTP2Configuration().getMaxRequestTrailerLength());
        }

        @Override
        protected Session getSession() {
            return connection.getTcpSession();
        }

        @Override
        protected HttpGenerator getHttpGenerator() {
            return httpGenerator;
        }
    }

    private void checkWrite(Request request, HTTP1ClientResponseHandler handler) {
        Assert.notNull(request, "The http client request is null.");
        Assert.notNull(handler, "The http1 client response handler is null.");
        Assert.state(isOpen(), "The current connection " + tcpSession.getSessionId() + " has been closed.");
        Assert.state(!upgradeHTTP2Complete.get(), "The current connection " + tcpSession.getSessionId() + " has upgraded HTTP2.");
        Assert.state(!upgradeWebSocketComplete.get(), "The current connection " + tcpSession.getSessionId() + " has upgraded WebSocket.");

        if (wrap.writing.compareAndSet(null, handler)) {
            request.getFields().put(HttpHeader.HOST, tcpSession.getRemoteAddress().getHostString());
            handler.connection = this;
            handler.request = request;
        } else {
            throw new WritePendingException();
        }
    }

    @Override
    public void close() {
        if (isOpen()) {
            super.close();
        }
    }

    @Override
    public boolean isClosed() {
        return !isOpen();
    }

    @Override
    public boolean isOpen() {
        return tcpSession.isOpen() && !upgradeHTTP2Complete.get() && !upgradeWebSocketComplete.get();
    }

    public boolean getUpgradeHTTP2Complete() {
        return upgradeHTTP2Complete.get();
    }

    public boolean getUpgradeWebSocketComplete() {
        return upgradeWebSocketComplete.get();
    }
}
