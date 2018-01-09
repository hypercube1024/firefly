package com.firefly.client.http2;

import com.firefly.codec.http2.decode.Parser;
import com.firefly.codec.http2.encode.Generator;
import com.firefly.codec.http2.frame.*;
import com.firefly.codec.http2.model.HttpHeader;
import com.firefly.codec.http2.model.HttpHeaderValue;
import com.firefly.codec.http2.model.MetaData;
import com.firefly.codec.http2.model.MetaData.Request;
import com.firefly.codec.http2.stream.*;
import com.firefly.codec.http2.stream.Session.Listener;
import com.firefly.codec.websocket.model.IncomingFrames;
import com.firefly.codec.websocket.stream.WebSocketConnection;
import com.firefly.codec.websocket.stream.WebSocketPolicy;
import com.firefly.net.SecureSession;
import com.firefly.net.Session;
import com.firefly.utils.concurrent.Callback;
import com.firefly.utils.concurrent.FuturePromise;
import com.firefly.utils.concurrent.Promise;
import com.firefly.utils.concurrent.Scheduler;
import com.firefly.utils.exception.CommonRuntimeException;
import com.firefly.utils.io.BufferUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class HTTP2ClientConnection extends AbstractHTTP2Connection implements HTTPClientConnection {

    private static Logger log = LoggerFactory.getLogger("firefly-system");

    public void initialize(HTTP2Configuration config, final Promise<? super HTTP2ClientConnection> promise,
                           final Listener listener) {
        Map<Integer, Integer> settings = listener.onPreface(getHttp2Session());
        if (settings == null) {
            settings = Collections.emptyMap();
        }
        PrefaceFrame prefaceFrame = new PrefaceFrame();
        SettingsFrame settingsFrame = new SettingsFrame(settings, false);
        SessionSPI sessionSPI = getSessionSPI();
        int windowDelta = config.getInitialSessionRecvWindow() - FlowControlStrategy.DEFAULT_WINDOW_SIZE;
        Callback callback = new Callback() {

            @Override
            public void succeeded() {
                promise.succeeded(HTTP2ClientConnection.this);
            }

            @Override
            public void failed(Throwable x) {
                HTTP2ClientConnection.this.close();
                promise.failed(x);
            }
        };

        if (windowDelta > 0) {
            sessionSPI.updateRecvWindow(windowDelta);
            sessionSPI.frames(null, callback, prefaceFrame, settingsFrame, new WindowUpdateFrame(0, windowDelta));
        } else {
            sessionSPI.frames(null, callback, prefaceFrame, settingsFrame);
        }

        Scheduler.Future pingFuture = scheduler.scheduleAtFixedRate(() -> getHttp2Session().ping(new PingFrame(false), new Callback() {
            public void succeeded() {
                log.info("The session {} sent ping frame success", getSessionId());
            }

            public void failed(Throwable x) {
                log.warn("the session {} sends ping frame failure. {}", getSessionId(), x.getMessage());
            }
        }), config.getHttp2PingInterval(), config.getHttp2PingInterval(), TimeUnit.MILLISECONDS);

        onClose(c -> pingFuture.cancel());
    }

    public HTTP2ClientConnection(HTTP2Configuration config, Session tcpSession, SecureSession secureSession,
                                 Listener listener) {
        super(config, tcpSession, secureSession, listener);
    }

    @Override
    protected HTTP2Session initHTTP2Session(HTTP2Configuration config, FlowControlStrategy flowControl,
                                            Listener listener) {
        return new HTTP2ClientSession(scheduler, this.tcpSession, this.generator, listener, flowControl, config.getStreamIdleTimeout());
    }

    @Override
    protected Parser initParser(HTTP2Configuration config) {
        return new Parser(http2Session, config.getMaxDynamicTableSize(), config.getMaxRequestHeadLength());
    }

    Parser getParser() {
        return parser;
    }

    Generator getGenerator() {
        return generator;
    }

    SessionSPI getSessionSPI() {
        return http2Session;
    }

    @Override
    public void send(Request request, ClientHTTPHandler handler) {
        Promise<HTTPOutputStream> promise = new Promise<HTTPOutputStream>() {

            @Override
            public void succeeded(HTTPOutputStream output) {
                try {
                    output.close();
                } catch (IOException e) {
                    log.error("write data unsuccessfully", e);
                }

            }

            @Override
            public void failed(Throwable x) {
                log.error("write data unsuccessfully", x);
            }
        };

        request(request, true, promise, handler);
    }

    @Override
    public void send(Request request, final ByteBuffer buffer, ClientHTTPHandler handler) {
        send(request, Collections.singleton(buffer), handler);
    }

    @Override
    public void send(Request request, final ByteBuffer[] buffers, ClientHTTPHandler handler) {
        send(request, Arrays.asList(buffers), handler);
    }

    @Override
    public void send(MetaData.Request request, Collection<ByteBuffer> buffers, ClientHTTPHandler handler) {
        long contentLength = BufferUtils.remaining(buffers);
        request.getFields().put(HttpHeader.CONTENT_LENGTH, String.valueOf(contentLength));

        Promise<HTTPOutputStream> promise = new Promise<HTTPOutputStream>() {

            @Override
            public void succeeded(HTTPOutputStream output) {
                try {
                    output.writeWithContentLength(buffers);
                } catch (IOException e) {
                    log.error("write data unsuccessfully", e);
                }
            }

            @Override
            public void failed(Throwable x) {
                log.error("write data unsuccessfully", x);
            }
        };

        send(request, promise, handler);
    }

    @Override
    public HTTPOutputStream sendRequestWithContinuation(MetaData.Request request, ClientHTTPHandler handler) {
        request.getFields().put(HttpHeader.EXPECT, HttpHeaderValue.CONTINUE);
        return getHTTPOutputStream(request, handler);
    }

    @Override
    public HTTPOutputStream getHTTPOutputStream(final Request request, final ClientHTTPHandler handler) {
        FuturePromise<HTTPOutputStream> promise = new FuturePromise<>();
        send(request, promise, handler);
        try {
            return promise.get();
        } catch (Throwable e) {
            log.error("get http output stream unsuccessfully", e);
            return null;
        }
    }

    @Override
    public void send(final Request request, final Promise<HTTPOutputStream> promise, final ClientHTTPHandler handler) {
        request(request, false, promise, handler);
    }

    public void request(final Request request, boolean endStream,
                        final Promise<HTTPOutputStream> promise,
                        final ClientHTTPHandler handler) {
        http2Session.newStream(new HeadersFrame(request, null, endStream),
                new HTTP2ClientResponseHandler.ClientStreamPromise(request, promise),
                new HTTP2ClientResponseHandler(request, handler, this));
    }

    @Override
    public void upgradeHTTP2(Request request, SettingsFrame settings, Promise<HTTP2ClientConnection> promise,
                             ClientHTTPHandler upgradeHandler,
                             ClientHTTPHandler http2ResponseHandler) {
        throw new CommonRuntimeException("The current connection version is http2, it does not need to upgrading.");
    }

    @Override
    public void upgradeWebSocket(Request request, WebSocketPolicy policy, Promise<WebSocketConnection> promise,
                                 ClientHTTPHandler upgradeHandler, IncomingFrames incomingFrames) {
        throw new CommonRuntimeException("The current connection version is http2, it can not upgrade WebSocket.");
    }

}
