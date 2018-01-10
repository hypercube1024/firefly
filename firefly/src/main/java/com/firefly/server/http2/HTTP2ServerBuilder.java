package com.firefly.server.http2;

import com.firefly.codec.http2.model.BadMessageException;
import com.firefly.codec.http2.model.HttpMethod;
import com.firefly.codec.http2.model.HttpStatus;
import com.firefly.codec.websocket.frame.BinaryFrame;
import com.firefly.codec.websocket.frame.Frame;
import com.firefly.codec.websocket.frame.TextFrame;
import com.firefly.codec.websocket.stream.WebSocketConnection;
import com.firefly.net.SecureSessionFactory;
import com.firefly.server.http2.router.Handler;
import com.firefly.server.http2.router.Router;
import com.firefly.server.http2.router.RouterManager;
import com.firefly.server.http2.router.RoutingContext;
import com.firefly.server.http2.router.handler.body.HTTPBodyConfiguration;
import com.firefly.server.http2.router.handler.error.AbstractErrorResponseHandler;
import com.firefly.server.http2.router.handler.error.DefaultErrorResponseHandlerLoader;
import com.firefly.server.http2.router.impl.RoutingContextImpl;
import com.firefly.utils.function.Action1;
import com.firefly.utils.function.Action2;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.ByteBuffer;
import java.util.*;

/**
 * @author Pengtao Qiu
 */
public class HTTP2ServerBuilder {

    private static Logger log = LoggerFactory.getLogger("firefly-system");

    private static ThreadLocal<RoutingContext> currentCtx = new ThreadLocal<>();

    private SimpleHTTPServer server;
    private RouterManager routerManager;
    private Router currentRouter;
    private final List<WebSocketBuilder> webSocketBuilders = new LinkedList<>();

    public HTTP2ServerBuilder httpsServer() {
        SimpleHTTPServerConfiguration configuration = new SimpleHTTPServerConfiguration();
        configuration.setSecureConnectionEnabled(true);
        return httpServer(configuration, new HTTPBodyConfiguration());
    }

    public HTTP2ServerBuilder httpsServer(SecureSessionFactory secureSessionFactory) {
        SimpleHTTPServerConfiguration configuration = new SimpleHTTPServerConfiguration();
        configuration.setSecureConnectionEnabled(true);
        configuration.setSecureSessionFactory(secureSessionFactory);
        return httpServer(configuration, new HTTPBodyConfiguration());
    }

    public HTTP2ServerBuilder httpServer() {
        return httpServer(new SimpleHTTPServerConfiguration(), new HTTPBodyConfiguration());
    }

    public HTTP2ServerBuilder httpServer(SimpleHTTPServerConfiguration serverConfiguration,
                                         HTTPBodyConfiguration httpBodyConfiguration) {
        AbstractErrorResponseHandler handler = DefaultErrorResponseHandlerLoader.getInstance().getHandler();
        server = new SimpleHTTPServer(serverConfiguration);
        server.badMessage((status, reason, request) -> {
            RoutingContext ctx = new RoutingContextImpl(request, Collections.emptyNavigableSet());
            handler.render(ctx, status, new BadMessageException(reason));
        });
        routerManager = RouterManager.create(httpBodyConfiguration);
        return this;
    }

    public SimpleHTTPServer getServer() {
        return server;
    }

    /**
     * register a new router
     *
     * @return HTTP2ServerBuilder
     */
    public HTTP2ServerBuilder router() {
        currentRouter = routerManager.register();
        return this;
    }

    public HTTP2ServerBuilder router(Integer id) {
        currentRouter = routerManager.register(id);
        return this;
    }

    private void check() {
        if (server == null) {
            throw new IllegalStateException("the http server has not been created, please call httpServer() first");
        }
    }

    public HTTP2ServerBuilder listen(String host, int port) {
        check();
        webSocketBuilders.forEach(WebSocketBuilder::listenWebSocket);
        server.headerComplete(routerManager::accept).listen(host, port);
        return this;
    }

    public HTTP2ServerBuilder listen() {
        check();
        webSocketBuilders.forEach(WebSocketBuilder::listenWebSocket);
        server.headerComplete(routerManager::accept).listen();
        return this;
    }

    public HTTP2ServerBuilder stop() {
        check();
        server.stop();
        return this;
    }

    // delegated Router methods
    public HTTP2ServerBuilder path(String url) {
        currentRouter.path(url);
        return this;
    }

    public HTTP2ServerBuilder paths(List<String> paths) {
        currentRouter.paths(paths);
        return this;
    }

    public HTTP2ServerBuilder pathRegex(String regex) {
        currentRouter.pathRegex(regex);
        return this;
    }

    public HTTP2ServerBuilder method(String method) {
        currentRouter.method(method);
        return this;
    }

    public HTTP2ServerBuilder methods(String[] methods) {
        Arrays.stream(methods).forEach(this::method);
        return this;
    }

    public HTTP2ServerBuilder method(HttpMethod httpMethod) {
        currentRouter.method(httpMethod);
        return this;
    }

    public HTTP2ServerBuilder methods(HttpMethod[] methods) {
        Arrays.stream(methods).forEach(this::method);
        return this;
    }

    public HTTP2ServerBuilder get(String url) {
        currentRouter.get(url);
        return this;
    }

    public HTTP2ServerBuilder post(String url) {
        currentRouter.post(url);
        return this;
    }

    public HTTP2ServerBuilder put(String url) {
        currentRouter.put(url);
        return this;
    }

    public HTTP2ServerBuilder delete(String url) {
        currentRouter.delete(url);
        return this;
    }

    public HTTP2ServerBuilder consumes(String contentType) {
        currentRouter.consumes(contentType);
        return this;
    }

    public HTTP2ServerBuilder produces(String accept) {
        currentRouter.produces(accept);
        return this;
    }

    public HTTP2ServerBuilder handler(Handler handler) {
        currentRouter.handler(ctx -> handlerWrap(handler, ctx));
        return this;
    }

    protected void handlerWrap(Handler handler, RoutingContext ctx) {
        try {
            currentCtx.set(ctx);
            handler.handle(ctx);
        } catch (Exception e) {
            ctx.fail(e);
            log.error("http server handler exception", e);
        } finally {
            currentCtx.remove();
        }
    }

    public HTTP2ServerBuilder asyncHandler(Handler handler) {
        currentRouter.handler(ctx -> {
            ctx.getResponse().setAsynchronous(true);
            server.getHandlerExecutorService().execute(() -> handlerWrap(handler, ctx));
        });
        return this;
    }

    public WebSocketBuilder webSocket(String path) {
        WebSocketBuilder webSocketBuilder = new WebSocketBuilder(path);
        webSocketBuilders.add(webSocketBuilder);
        return webSocketBuilder;
    }

    public class WebSocketBuilder {
        protected final String path;
        protected Action1<WebSocketConnection> onConnect;
        protected Action2<String, WebSocketConnection> onText;
        protected Action2<ByteBuffer, WebSocketConnection> onData;
        protected Action2<Throwable, WebSocketConnection> onError;

        public WebSocketBuilder(String path) {
            this.path = path;
        }

        public WebSocketBuilder onConnect(Action1<WebSocketConnection> onConnect) {
            this.onConnect = onConnect;
            return this;
        }

        public WebSocketBuilder onText(Action2<String, WebSocketConnection> onText) {
            this.onText = onText;
            return this;
        }

        public WebSocketBuilder onData(Action2<ByteBuffer, WebSocketConnection> onData) {
            this.onData = onData;
            return this;
        }

        public WebSocketBuilder onError(Action2<Throwable, WebSocketConnection> onError) {
            this.onError = onError;
            return this;
        }

        public HTTP2ServerBuilder listen(String host, int port) {
            return HTTP2ServerBuilder.this.listen(host, port);
        }

        public HTTP2ServerBuilder listen() {
            return HTTP2ServerBuilder.this.listen();
        }

        private HTTP2ServerBuilder listenWebSocket() {
            server.registerWebSocket(path, new WebSocketHandler() {

                @Override
                public void onConnect(WebSocketConnection webSocketConnection) {
                    Optional.ofNullable(onConnect).ifPresent(c -> c.call(webSocketConnection));
                }

                @Override
                public void onFrame(Frame frame, WebSocketConnection connection) {
                    switch (frame.getType()) {
                        case TEXT:
                            TextFrame textFrame = (TextFrame) frame;
                            Optional.ofNullable(onText).ifPresent(t -> t.call(textFrame.getPayloadAsUTF8(), connection));
                            break;
                        case BINARY:
                            BinaryFrame binaryFrame = (BinaryFrame) frame;
                            Optional.ofNullable(onData).ifPresent(d -> d.call(binaryFrame.getPayload(), connection));
                            break;
                    }
                }

                @Override
                public void onError(Throwable t, WebSocketConnection connection) {
                    Optional.ofNullable(onError).ifPresent(e -> e.call(t, connection));
                }
            });
            router().path(path).handler(ctx -> {
            });
            return HTTP2ServerBuilder.this;
        }

    }

    public static Optional<RoutingContext> getCurrentCtx() {
        return Optional.ofNullable(currentCtx.get());
    }
}
