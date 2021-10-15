package com.fireflysource.net.http.server;

import com.fireflysource.common.sys.Result;
import com.fireflysource.net.http.common.HttpConnection;
import com.fireflysource.net.tcp.TcpConnection;
import com.fireflysource.net.websocket.server.WebSocketServerConnectionHandler;

import java.net.InetSocketAddress;
import java.util.concurrent.CompletableFuture;

/**
 * The HTTP server connection.
 *
 * @author Pengtao Qiu
 */
public interface HttpServerConnection extends HttpConnection {

    Listener EMPTY_LISTENER = new Listener.Adapter();

    /**
     * Set HTTP server connection event listener. It receives the HTTP request or exception events.
     *
     * @param listener The HTTP server connection event listener.
     * @return The HTTP server connection.
     */
    HttpServerConnection setListener(Listener listener);

    /**
     * Begin to receive HTTP request.
     */
    void begin();

    /**
     * The HTTP server connection event listener.
     */
    interface Listener {

        /**
         * When the all HTTP headers receive, invokes this method.
         *
         * @param context The routing context. In this stage, the context cannot get the HTTP body.
         * @return The future result.
         */
        CompletableFuture<Void> onHeaderComplete(RoutingContext context);

        /**
         * When the HTTP request is complete, invokes this method. it contains headers and body,
         *
         * @param context The routing context.
         * @return The future result.
         */
        CompletableFuture<Void> onHttpRequestComplete(RoutingContext context);

        /**
         * When the connection parses the error HTTP message, invokes this method.
         *
         * @param context   The routing context. The context may be null.
         * @param throwable An exception.
         * @return The future result.
         */
        CompletableFuture<Void> onException(RoutingContext context, Throwable throwable);

        /**
         * When the server accepts a Websocket handshake request, invokes this method.
         *
         * @param context The routing context.
         * @return The Websocket connection handler.
         */
        CompletableFuture<WebSocketServerConnectionHandler> onWebSocketHandshake(RoutingContext context);

        /**
         * When the server accepts an HTTP tunnel request, invokes this method.
         *
         * @param request The HTTP request.
         * @return If true, create an HTTP tunnel connection.
         */
        CompletableFuture<Boolean> onAcceptHttpTunnel(HttpServerRequest request);

        /**
         * After the server accepts a HTTP tunnel request and then the server will response the HTTP tunnel response, invokes this method.
         *
         * @param context The routing context.
         * @return The future result.
         */
        CompletableFuture<Void> onAcceptHttpTunnelHandshakeResponse(RoutingContext context);

        /**
         * After the server refuses a HTTP tunnel request and then the server will response the HTTP tunnel response, invokes this method.
         *
         * @param context The routing context.
         * @return The future result.
         */
        CompletableFuture<Void> onRefuseHttpTunnelHandshakeResponse(RoutingContext context);

        /**
         * When the HTTP tunnel handshake is complete, invokes this method.
         *
         * @param connection The client TCP connection.
         * @param address The target address.
         * @return The future result.
         */
        CompletableFuture<Void> onHttpTunnelHandshakeComplete(TcpConnection connection, InetSocketAddress address);

        /**
         * The empty listener implement.
         */
        class Adapter implements Listener {

            @Override
            public CompletableFuture<Void> onHeaderComplete(RoutingContext context) {
                return Result.DONE;
            }

            @Override
            public CompletableFuture<Void> onHttpRequestComplete(RoutingContext context) {
                return Result.DONE;
            }

            @Override
            public CompletableFuture<Void> onException(RoutingContext context, Throwable throwable) {
                return Result.DONE;
            }

            @Override
            public CompletableFuture<WebSocketServerConnectionHandler> onWebSocketHandshake(RoutingContext context) {
                return CompletableFuture.completedFuture(new WebSocketServerConnectionHandler());
            }

            @Override
            public CompletableFuture<Boolean> onAcceptHttpTunnel(HttpServerRequest request) {
                return CompletableFuture.completedFuture(true);
            }

            @Override
            public CompletableFuture<Void> onAcceptHttpTunnelHandshakeResponse(RoutingContext context) {
                return Result.DONE;
            }

            @Override
            public CompletableFuture<Void> onRefuseHttpTunnelHandshakeResponse(RoutingContext context) {
                return Result.DONE;
            }

            @Override
            public CompletableFuture<Void> onHttpTunnelHandshakeComplete(TcpConnection connection, InetSocketAddress address) {
                return Result.DONE;
            }
        }
    }


}
