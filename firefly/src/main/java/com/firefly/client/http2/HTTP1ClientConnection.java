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
import com.firefly.net.Session;
import com.firefly.net.tcp.ssl.SSLSession;
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
import java.util.concurrent.atomic.AtomicReference;

public class HTTP1ClientConnection extends AbstractHTTP1Connection implements HTTPClientConnection {

    private static final Logger log = LoggerFactory.getLogger("firefly-system");

    private Promise<HTTPClientConnection> http2ConnectionPromise;
    private Listener http2SessionListener;
    private Promise<Stream> initStream;
    private Stream.Listener initStreamListener;
    private volatile boolean upgradeHTTP2Successfully = false;

    private final ResponseHandlerWrap wrap;

    private static class ResponseHandlerWrap implements ResponseHandler {

        private final AtomicReference<HTTP1ClientResponseHandler> writing = new AtomicReference<>();
        private int status;
        private String reason;
        private HTTP1ClientConnection connection;

        @Override
        public void earlyEOF() {
            HTTP1ClientResponseHandler h = writing.get();
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
            HTTP1ClientResponseHandler h = writing.get();
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

    public HTTP1ClientConnection(HTTP2Configuration config, Session tcpSession, SSLSession sslSession) {
        this(config, sslSession, tcpSession, new ResponseHandlerWrap());
    }

    private HTTP1ClientConnection(HTTP2Configuration config, SSLSession sslSession, Session tcpSession,
                                  ResponseHandler responseHandler) {
        super(config, sslSession, tcpSession, null, responseHandler);
        wrap = (ResponseHandlerWrap) responseHandler;
        wrap.connection = this;
    }

    @Override
    protected HttpParser initHttpParser(HTTP2Configuration config, RequestHandler requestHandler,
                                        ResponseHandler responseHandler) {
        return new HttpParser(responseHandler, config.getMaxRequestHeadLength());
    }

    @Override
    protected HttpGenerator initHttpGenerator() {
        return new HttpGenerator();
    }

    HttpParser getParser() {
        return parser;
    }

    HttpGenerator getGenerator() {
        return generator;
    }

    SSLSession getSSLSession() {
        return sslSession;
    }

    HTTP2Configuration getHTTP2Configuration() {
        return config;
    }

    Session getTcpSession() {
        return tcpSession;
    }

    boolean upgradeProtocolToHTTP2(MetaData.Request request, MetaData.Response response) {
        if (http2ConnectionPromise != null && http2SessionListener != null) {
            String upgradeValue = response.getFields().get(HttpHeader.UPGRADE);
            if (response.getStatus() == HttpStatus.SWITCHING_PROTOCOLS_101 && "h2c".equalsIgnoreCase(upgradeValue)) {
                upgradeHTTP2Successfully = true;

                // initialize http2 client connection;
                final HTTP2ClientConnection http2Connection = new HTTP2ClientConnection(getHTTP2Configuration(),
                        getTcpSession(), null, http2SessionListener) {
                    @Override
                    protected HTTP2Session initHTTP2Session(HTTP2Configuration config, FlowControlStrategy flowControl,
                                                            Listener listener) {
                        return HTTP2ClientSession.initSessionForUpgradingHTTP2(scheduler, this.tcpSession, generator,
                                listener, flowControl, 3, config.getStreamIdleTimeout(), initStream,
                                initStreamListener);
                    }
                };
                getTcpSession().attachObject(http2Connection);
                http2Connection.initialize(getHTTP2Configuration(), http2ConnectionPromise, http2SessionListener);
                return true;
            } else {
                return false;
            }
        } else {
            return false;
        }
    }

    @Override
    public void upgradeHTTP2(final MetaData.Request request, final SettingsFrame settings,
                             final Promise<HTTPClientConnection> promise, final ClientHTTPHandler handler) {
        upgradeHTTP2WithCleartext(request, settings,
                promise, new HTTP2ClientResponseHandler.ClientStreamPromise(request,
                        new Promise.Adapter<>(), true),
                new HTTP2ClientResponseHandler(request, handler, this), new Listener.Adapter() {

                    @Override
                    public Map<Integer, Integer> onPreface(com.firefly.codec.http2.stream.Session session) {
                        return settings.getSettings();
                    }

                    @Override
                    public void onFailure(com.firefly.codec.http2.stream.Session session, Throwable failure) {
                        log.error("client failure, {}", failure, session);
                    }

                }, handler);
    }

    public void upgradeHTTP2WithCleartext(MetaData.Request request, SettingsFrame settings,
                                          final Promise<HTTPClientConnection> promise, final Promise<Stream> initStream,
                                          final Stream.Listener initStreamListener, final Listener listener, final ClientHTTPHandler handler) {
        if (isEncrypted()) {
            throw new IllegalStateException("The TLS TCP connection must use ALPN to upgrade HTTP2");
        }

        this.http2ConnectionPromise = promise;
        this.http2SessionListener = listener;
        this.initStream = initStream;
        this.initStreamListener = initStreamListener;

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

                    request.getFields().add(new HttpField(HttpHeader.HTTP2_SETTINGS,
                            Base64Utils.encodeToUrlSafeString(settingsPayload)));
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

    @Override
    public HTTPOutputStream sendRequestWithContinuation(MetaData.Request request, ClientHTTPHandler handler) {
        request.getFields().put(HttpHeader.EXPECT, HttpHeaderValue.CONTINUE);
        HTTPOutputStream outputStream = getHTTPOutputStream(request, handler);
        try {
            outputStream.commit();
        } catch (IOException e) {
            generator.reset();
            log.error("client generates the HTTP message exception", e);
        }
        return outputStream;
    }

    @Override
    public void send(MetaData.Request request, ClientHTTPHandler handler) {
        try (HTTPOutputStream output = getHTTPOutputStream(request, handler)) {
            log.debug("client request and does not send data");
        } catch (IOException e) {
            generator.reset();
            log.error("client generates the HTTP message exception", e);
        }
    }

    @Override
    public void send(MetaData.Request request, ByteBuffer buffer, ClientHTTPHandler handler) {
        send(request, Collections.singleton(buffer), handler);
    }

    @Override
    public void send(MetaData.Request request, ByteBuffer[] buffers, ClientHTTPHandler handler) {
        send(request, Arrays.asList(buffers), handler);
    }

    @Override
    public void send(MetaData.Request request, Collection<ByteBuffer> buffers, ClientHTTPHandler handler) {
        try (HTTPOutputStream output = getHTTPOutputStream(request, handler)) {
            if (buffers != null) {
                output.writeWithContentLength(buffers);
            }
        } catch (IOException e) {
            generator.reset();
            log.error("client generates the HTTP message exception", e);
        }
    }

    @Override
    public HTTPOutputStream getHTTPOutputStream(MetaData.Request request, ClientHTTPHandler handler) {
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

        private HTTP1ClientRequestOutputStream(HTTP1ClientConnection connection, MetaData.Request request) {
            super(request, true);
            this.connection = connection;
        }

        @Override
        protected void generateHTTPMessageSuccessfully() {
            log.debug("client session {} generates the HTTP message completely", connection.tcpSession.getSessionId());
            connection.generator.reset();
        }

        @Override
        protected void generateHTTPMessageExceptionally(HttpGenerator.Result generatorResult,
                                                        HttpGenerator.State generatorState) {
            if (log.isDebugEnabled()) {
                log.debug("http1 generator error, the result is {}, and the generator state is {}", generatorResult,
                        generatorState);
            }
            connection.getGenerator().reset();
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
            return connection.getGenerator();
        }
    }

    private void checkWrite(MetaData.Request request, HTTP1ClientResponseHandler handler) {
        if (request == null)
            throw new IllegalArgumentException("the http client request is null");

        if (handler == null)
            throw new IllegalArgumentException("the http1 client response handler is null");

        if (!isOpen())
            throw new IllegalStateException("current client session " + tcpSession.getSessionId() + " has been closed");

        if (upgradeHTTP2Successfully)
            throw new IllegalStateException(
                    "current client session " + tcpSession.getSessionId() + " has upgraded HTTP2");

        if (wrap.writing.compareAndSet(null, handler)) {
            request.getFields().put(HttpHeader.HOST, tcpSession.getRemoteAddress().getHostString());
            handler.connection = this;
            handler.request = request;
        } else {
            throw new WritePendingException();
        }
    }

}
