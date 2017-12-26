package com.firefly.server.http2;

import com.firefly.codec.http2.decode.HttpParser;
import com.firefly.codec.http2.decode.HttpParser.RequestHandler;
import com.firefly.codec.http2.decode.HttpParser.ResponseHandler;
import com.firefly.codec.http2.decode.SettingsBodyParser;
import com.firefly.codec.http2.encode.HttpGenerator;
import com.firefly.codec.http2.frame.HeadersFrame;
import com.firefly.codec.http2.frame.PrefaceFrame;
import com.firefly.codec.http2.frame.SettingsFrame;
import com.firefly.codec.http2.model.*;
import com.firefly.codec.http2.stream.*;
import com.firefly.net.SecureSession;
import com.firefly.net.Session;
import com.firefly.utils.Assert;
import com.firefly.utils.codec.Base64Utils;
import com.firefly.utils.concurrent.Callback;
import com.firefly.utils.concurrent.Promise;
import com.firefly.utils.io.BufferUtils;
import com.firefly.utils.lang.TypeUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.ByteBuffer;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;

import static com.firefly.codec.http2.encode.PredefinedHTTP1Response.CONTINUE_100_BYTES;
import static com.firefly.codec.http2.encode.PredefinedHTTP1Response.H2C_BYTES;

public class HTTP1ServerConnection extends AbstractHTTP1Connection implements HTTPServerConnection {

    protected static final Logger log = LoggerFactory.getLogger("firefly-system");

    private final ServerSessionListener serverSessionListener;
    private final HTTP1ServerRequestHandler serverRequestHandler;
    AtomicBoolean upgradeHTTP2Complete = new AtomicBoolean(false);
    Promise<HTTPTunnelConnection> tunnelConnectionPromise;

    HTTP1ServerConnection(HTTP2Configuration config, Session tcpSession, SecureSession secureSession,
                          HTTP1ServerRequestHandler requestHandler, ServerSessionListener serverSessionListener) {
        super(config, secureSession, tcpSession, requestHandler, null);
        requestHandler.connection = this;
        this.serverSessionListener = serverSessionListener;
        this.serverRequestHandler = requestHandler;
    }

    @Override
    protected HttpParser initHttpParser(HTTP2Configuration config, RequestHandler requestHandler, ResponseHandler responseHandler) {
        return new HttpParser(requestHandler, config.getMaxRequestHeadLength());
    }

    HttpParser getParser() {
        return parser;
    }

    SecureSession getSecureSession() {
        return secureSession;
    }

    Session getTcpSession() {
        return tcpSession;
    }

    HTTP2Configuration getHTTP2Configuration() {
        return config;
    }

    public MetaData.Request getRequest() {
        return serverRequestHandler.request;
    }

    public MetaData.Response getResponse() {
        return serverRequestHandler.response;
    }

    void response100Continue() {
        serverRequestHandler.outputStream.response100Continue();
    }

    private void responseH2c() {
        serverRequestHandler.outputStream.responseH2c();
    }

    @Override
    public void upgradeHTTPTunnel(Promise<HTTPTunnelConnection> tunnelConnectionPromise) {
        this.tunnelConnectionPromise = tunnelConnectionPromise;
    }

    @Override
    public CompletableFuture<HTTPTunnelConnection> upgradeHTTPTunnel() {
        Promise.Completable<HTTPTunnelConnection> c = new Promise.Completable<>();
        tunnelConnectionPromise = c;
        return c;
    }

    HTTP1ServerTunnelConnection createHTTPTunnel() {
        if (tunnelConnectionPromise != null) {
            HTTP1ServerTunnelConnection tunnelConnection = new HTTP1ServerTunnelConnection(secureSession, tcpSession, httpVersion);
            tunnelConnectionPromise.succeeded(tunnelConnection);
            tcpSession.attachObject(tunnelConnection);
            return tunnelConnection;
        } else {
            return null;
        }
    }

    static class HTTP1ServerResponseOutputStream extends AbstractHTTP1OutputStream {

        private final HTTP1ServerConnection connection;
        private final HttpGenerator httpGenerator;

        HTTP1ServerResponseOutputStream(MetaData.Response response, HTTP1ServerConnection connection) {
            super(response, false);
            this.connection = connection;
            httpGenerator = new HttpGenerator(true, true);
        }

        HTTP1ServerConnection getHTTP1ServerConnection() {
            return connection;
        }

        void responseH2c() {
//            getSession().encode(ByteBuffer.wrap(H2C_BYTES));
            CompletableFuture<Boolean> future = new CompletableFuture<>();
            getSession().write(ByteBuffer.wrap(H2C_BYTES), new Callback() {
                @Override
                public void succeeded() {
                    future.complete(true);
                }

                @Override
                public void failed(Throwable x) {
                    future.completeExceptionally(x);
                }
            });
            try {
                future.get(2, TimeUnit.SECONDS);
            } catch (Exception e) {
                log.error("Write h2c response exception", e);
            }
        }

        void response100Continue() {
            getSession().encode(ByteBuffer.wrap(CONTINUE_100_BYTES));
        }

        @Override
        protected void generateHTTPMessageSuccessfully() {
            log.debug("server session {} generates the HTTP message completely", connection.getSessionId());

            final MetaData.Response response = connection.getResponse();
            final MetaData.Request request = connection.getRequest();

            String requestConnectionValue = request.getFields().get(HttpHeader.CONNECTION);
            String responseConnectionValue = response.getFields().get(HttpHeader.CONNECTION);

            switch (request.getHttpVersion()) {
                case HTTP_1_0:
                    if ("keep-alive".equalsIgnoreCase(requestConnectionValue)
                            && "keep-alive".equalsIgnoreCase(responseConnectionValue)) {
                        log.debug("the server {} connection {} is persistent", response.getHttpVersion(), connection.getSessionId());
                    } else {
                        connection.close();
                    }
                    break;
                case HTTP_1_1: // the persistent connection is default in HTTP 1.1
                    if ("close".equalsIgnoreCase(requestConnectionValue)
                            || "close".equalsIgnoreCase(responseConnectionValue)) {
                        connection.close();
                    } else {
                        log.debug("the server {} connection {} is persistent", response.getHttpVersion(),
                                connection.getSessionId());
                    }
                    break;
                default:
                    throw new IllegalStateException("server response does not support the http version " + connection.getHttpVersion());
            }

        }

        @Override
        protected void generateHTTPMessageExceptionally(HttpGenerator.Result actualResult,
                                                        HttpGenerator.State actualState,
                                                        HttpGenerator.Result expectedResult,
                                                        HttpGenerator.State expectedState) {
            log.error("http1 generator error, actual: [{}, {}], expected: [{}, {}]", actualResult, actualState, expectedResult, expectedState);
            throw new IllegalStateException("server generates http message exception.");
        }

        @Override
        protected ByteBuffer getHeaderByteBuffer() {
            return BufferUtils.allocate(connection.getHTTP2Configuration().getMaxResponseHeadLength());
        }

        @Override
        protected ByteBuffer getTrailerByteBuffer() {
            return BufferUtils.allocate(connection.getHTTP2Configuration().getMaxResponseTrailerLength());
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

    boolean directUpgradeHTTP2(MetaData.Request request) {
        if (HttpMethod.PRI.is(request.getMethod())) {
            HTTP2ServerConnection http2ServerConnection = new HTTP2ServerConnection(config, tcpSession, secureSession,
                    serverSessionListener);
            tcpSession.attachObject(http2ServerConnection);
            http2ServerConnection.getParser().directUpgrade();
            upgradeHTTP2Complete.compareAndSet(false, true);
            return true;
        } else {
            return false;
        }
    }

    boolean upgradeProtocol(MetaData.Request request, MetaData.Response response) {
        switch (Protocol.from(request)) {
            case H2: {
                HttpField settingsField = request.getFields().getField(HttpHeader.HTTP2_SETTINGS);
                Assert.notNull(settingsField, "The http2 setting field must be not null.");

                final byte[] settings = Base64Utils.decodeFromUrlSafeString(settingsField.getValue());
                if (log.isDebugEnabled()) {
                    log.debug("the server received settings {}", TypeUtils.toHexString(settings));
                }

                SettingsFrame settingsFrame = SettingsBodyParser.parseBody(BufferUtils.toBuffer(settings));
                if (settingsFrame == null) {
                    throw new BadMessageException("settings frame parsing error");
                } else {
                    responseH2c();

                    HTTP2ServerConnection http2ServerConnection = new HTTP2ServerConnection(config,
                            tcpSession, secureSession, serverSessionListener);
                    tcpSession.attachObject(http2ServerConnection);
                    http2ServerConnection.getParser().standardUpgrade();

                    serverSessionListener.onAccept(http2ServerConnection.getHttp2Session());
                    SessionSPI sessionSPI = http2ServerConnection.getSessionSPI();

                    sessionSPI.onFrame(new PrefaceFrame());
                    sessionSPI.onFrame(settingsFrame);
                    sessionSPI.onFrame(new HeadersFrame(1, request, null, true));
                }
                upgradeHTTP2Complete.compareAndSet(false, true);
                return true;
            }
            case WEB_SOCKET: {
                // TODO
                return false;
            }
            default:
                return false;
        }
    }

}
