package com.firefly.server.http2;

import com.codahale.metrics.Meter;
import com.firefly.codec.http2.model.HttpStatus;
import com.firefly.codec.http2.model.MetaData;
import com.firefly.codec.http2.stream.HTTPConnection;
import com.firefly.codec.http2.stream.HTTPOutputStream;
import com.firefly.codec.websocket.frame.Frame;
import com.firefly.codec.websocket.stream.WebSocketConnection;
import com.firefly.codec.websocket.stream.WebSocketPolicy;
import com.firefly.utils.function.Action1;
import com.firefly.utils.function.Action2;
import com.firefly.utils.function.Action3;
import com.firefly.utils.io.IO;
import com.firefly.utils.lang.AbstractLifeCycle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.ForkJoinWorkerThread;

public class SimpleHTTPServer extends AbstractLifeCycle {

    private static Logger log = LoggerFactory.getLogger("firefly-system");

    private static final int defaultPoolSize = Integer.getInteger("com.firefly.server.http2.async.defaultPoolSize", Runtime.getRuntime().availableProcessors());

    private HTTP2Server http2Server;
    private SimpleHTTPServerConfiguration configuration;

    private Action1<SimpleRequest> headerComplete;
    private Action3<Integer, String, SimpleRequest> badMessage;
    private Action1<SimpleRequest> earlyEof;
    private Action1<HTTPConnection> acceptConnection;
    private Action2<SimpleRequest, HTTPServerConnection> tunnel;

    private Meter requestMeter;
    private final ExecutorService handlerExecutorService;

    private Map<String, WebSocketHandler> webSocketHandlerMap = new HashMap<>();
    private WebSocketPolicy webSocketPolicy;

    public SimpleHTTPServer() {
        this(new SimpleHTTPServerConfiguration());
    }

    public SimpleHTTPServer(SimpleHTTPServerConfiguration configuration) {
        this.configuration = configuration;
        requestMeter = this.configuration.getTcpConfiguration()
                                         .getMetricReporterFactory()
                                         .getMetricRegistry()
                                         .meter("http2.SimpleHTTPServer.request.count");
        handlerExecutorService = new ForkJoinPool(defaultPoolSize, pool -> {
            ForkJoinWorkerThread workerThread = ForkJoinPool.defaultForkJoinWorkerThreadFactory.newThread(pool);
            workerThread.setName("firefly-http-server-handler-pool-" + workerThread.getPoolIndex());
            return workerThread;
        }, null, true);
    }

    public SimpleHTTPServer acceptHTTPTunnelConnection(Action2<SimpleRequest, HTTPServerConnection> tunnel) {
        this.tunnel = tunnel;
        return this;
    }

    public SimpleHTTPServer headerComplete(Action1<SimpleRequest> headerComplete) {
        this.headerComplete = headerComplete;
        return this;
    }

    public SimpleHTTPServer earlyEof(Action1<SimpleRequest> earlyEof) {
        this.earlyEof = earlyEof;
        return this;
    }

    public SimpleHTTPServer badMessage(Action3<Integer, String, SimpleRequest> badMessage) {
        this.badMessage = badMessage;
        return this;
    }

    public SimpleHTTPServer acceptConnection(Action1<HTTPConnection> acceptConnection) {
        this.acceptConnection = acceptConnection;
        return this;
    }

    public SimpleHTTPServer registerWebSocket(String uri, WebSocketHandler webSocketHandler) {
        webSocketHandlerMap.put(uri, webSocketHandler);
        return this;
    }

    public SimpleHTTPServer webSocketPolicy(WebSocketPolicy webSocketPolicy) {
        this.webSocketPolicy = webSocketPolicy;
        return this;
    }

    public ExecutorService getNetExecutorService() {
        return http2Server.getNetExecutorService();
    }

    public ExecutorService getHandlerExecutorService() {
        return handlerExecutorService;
    }

    public SimpleHTTPServerConfiguration getConfiguration() {
        return configuration;
    }

    public void listen(String host, int port) {
        configuration.setHost(host);
        configuration.setPort(port);
        listen();
    }

    public void listen() {
        start();
    }

    @Override
    protected void init() {
        http2Server = new HTTP2Server(configuration.getHost(), configuration.getPort(), configuration, new ServerHTTPHandler.Adapter().acceptConnection(acceptConnection).acceptHTTPTunnelConnection((request, response, out, connection) -> {
            SimpleRequest r = new SimpleRequest(request, response, out, connection);
            request.setAttachment(r);
            if (tunnel != null) {
                tunnel.call(r, connection);
            }
            return true;
        }).headerComplete((request, response, out, connection) -> {
            SimpleRequest r = new SimpleRequest(request, response, out, connection);
            request.setAttachment(r);
            if (headerComplete != null) {
                headerComplete.call(r);
            }
            requestMeter.mark();
            return false;
        }).content((buffer, request, response, out, connection) -> {
            SimpleRequest r = (SimpleRequest) request.getAttachment();
            if (r.content != null) {
                r.content.call(buffer);
            } else {
                r.requestBody.add(buffer);
            }
            return false;
        }).contentComplete((request, response, out, connection) -> {
            SimpleRequest r = (SimpleRequest) request.getAttachment();
            if (r.contentComplete != null) {
                r.contentComplete.call(r);
            }
            return false;
        }).messageComplete((request, response, out, connection) -> {
            SimpleRequest r = (SimpleRequest) request.getAttachment();
            if (r.messageComplete != null) {
                r.messageComplete.call(r);
            }
            if (!r.getResponse().isAsynchronous()) {
                IO.close(r.getResponse());
            }
            return true;
        }).badMessage((status, reason, request, response, out, connection) -> {
            if (badMessage != null) {
                if (request.getAttachment() != null) {
                    SimpleRequest r = (SimpleRequest) request.getAttachment();
                    badMessage.call(status, reason, r);
                } else {
                    SimpleRequest r = new SimpleRequest(request, response, out, connection);
                    request.setAttachment(r);
                    badMessage.call(status, reason, r);
                }
            }
        }).earlyEOF((request, response, out, connection) -> {
            if (earlyEof != null) {
                if (request.getAttachment() != null) {
                    SimpleRequest r = (SimpleRequest) request.getAttachment();
                    earlyEof.call(r);
                } else {
                    SimpleRequest r = new SimpleRequest(request, response, out, connection);
                    request.setAttachment(r);
                    earlyEof.call(r);
                }
            }
        }), new WebSocketHandler() {

            @Override
            public boolean acceptUpgrade(MetaData.Request request, MetaData.Response response,
                                         HTTPOutputStream output,
                                         HTTPConnection connection) {
                log.info("The connection {} will upgrade to WebSocket connection", connection.getSessionId());
                WebSocketHandler handler = webSocketHandlerMap.get(request.getURI().getPath());
                if (handler == null) {
                    response.setStatus(HttpStatus.BAD_REQUEST_400);
                    try (HTTPOutputStream out = output) {
                        out.write(("The " + request.getURI().getPath() + " can not upgrade to WebSocket").getBytes(StandardCharsets.UTF_8));
                    } catch (IOException e) {
                        log.error("Write http message exception", e);
                    }
                    return false;
                } else {
                    return handler.acceptUpgrade(request, response, output, connection);
                }
            }

            @Override
            public void onConnect(WebSocketConnection webSocketConnection) {
                Optional.ofNullable(webSocketHandlerMap.get(webSocketConnection.getUpgradeRequest().getURI().getPath()))
                        .ifPresent(handler -> handler.onConnect(webSocketConnection));
            }

            @Override
            public WebSocketPolicy getWebSocketPolicy() {
                if (webSocketPolicy != null) {
                    return webSocketPolicy;
                } else {
                    return defaultWebSocketPolicy;
                }
            }

            @Override
            public void onFrame(Frame frame, WebSocketConnection connection) {
                Optional.ofNullable(webSocketHandlerMap.get(connection.getUpgradeRequest().getURI().getPath()))
                        .ifPresent(handler -> handler.onFrame(frame, connection));
            }

            @Override
            public void onError(Throwable t, WebSocketConnection connection) {
                Optional.ofNullable(webSocketHandlerMap.get(connection.getUpgradeRequest().getURI().getPath()))
                        .ifPresent(handler -> handler.onError(t, connection));
            }
        });
        http2Server.start();
    }

    @Override
    protected void destroy() {
        try {
            handlerExecutorService.shutdown();
        } catch (Exception e) {
            log.warn("simple http server handler pool shutdown exception", e);
        } finally {
            http2Server.stop();
        }
    }

}
