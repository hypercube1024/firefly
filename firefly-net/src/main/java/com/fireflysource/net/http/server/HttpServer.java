package com.fireflysource.net.http.server;

import com.fireflysource.common.lifecycle.LifeCycle;
import com.fireflysource.net.tcp.TcpConnection;
import com.fireflysource.net.tcp.secure.SecureEngineFactory;
import com.fireflysource.net.websocket.server.WebSocketServerConnectionBuilder;

import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.BiFunction;
import java.util.function.Function;

/**
 * @author Pengtao Qiu
 */
public interface HttpServer extends LifeCycle {

    /**
     * Register a new router.
     *
     * @return The router.
     */
    Router router();

    /**
     * Register a new router.
     *
     * @param id The router id.
     * @return The router.
     */
    Router router(int id);

    /**
     * Create a new websocket connection builder.
     *
     * @return The websocket connection builder.
     */
    WebSocketServerConnectionBuilder websocket();

    /**
     * Create a new websocket connection builder.
     *
     * @param path The websocket url.
     * @return The websocket connection builder.
     */
    WebSocketServerConnectionBuilder websocket(String path);

    /**
     * HTTP headers received callback.
     *
     * @param function The HTTP headers received callback.
     * @return The HTTP server.
     */
    HttpServer onHeaderComplete(Function<RoutingContext, CompletableFuture<Void>> function);

    /**
     * HTTP server exception callback.
     *
     * @param biFunction The HTTP server exception callback.
     * @return The HTTP server.
     */
    HttpServer onException(BiFunction<RoutingContext, Throwable, CompletableFuture<Void>> biFunction);

    /**
     * The router not found callback.
     *
     * @param function Invoke this function when the HTTP server does not find the router.
     * @return The HTTP server.
     */
    HttpServer onRouterNotFound(Function<RoutingContext, CompletableFuture<Void>> function);

    /**
     * The router complete callback.
     *
     * @param function Invoke this function when the last router executes successfully.
     * @return The HTTP server.
     */
    HttpServer onRouterComplete(Function<RoutingContext, CompletableFuture<Void>> function);

    /**
     * The accept HTTP tunnel callback.
     *
     * @param function Invoke this function when the server accepts a HTTP tunnel request.
     * @return The HTTP server.
     */
    HttpServer onAcceptHttpTunnel(Function<HttpServerRequest, CompletableFuture<Boolean>> function);

    /**
     * Accept HTTP tunnel handshake response callback. The default response: HTTP/1.1 200 Connection Established.
     *
     * @param function Invoke this function after the server accepts a HTTP tunnel request and then the server will response the HTTP tunnel response.
     * @return The HTTP server.
     */
    HttpServer onAcceptHttpTunnelHandshakeResponse(Function<RoutingContext, CompletableFuture<Void>> function);

    /**
     * Refuse HTTP tunnel handshake response callback. The default response: HTTP/1.1 407 Proxy Authentication Required.
     *
     * @param function Invoke this function after the server refuses a HTTP tunnel request and then the server will response the HTTP tunnel response.
     * @return The HTTP server.
     */
    HttpServer onRefuseHttpTunnelHandshakeResponse(Function<RoutingContext, CompletableFuture<Void>> function);

    /**
     * The HTTP tunnel handshake complete callback.
     *
     * @param function Invoke this function when the HTTP tunnel handshake is complete.
     * @return The HTTP server.
     */
    HttpServer onHttpTunnelHandshakeComplete(Function<TcpConnection, CompletableFuture<Void>> function);

    /**
     * Set the TLS engine factory.
     *
     * @param secureEngineFactory The TLS engine factory.
     * @return The HTTP server.
     */
    HttpServer secureEngineFactory(SecureEngineFactory secureEngineFactory);

    /**
     * The supported application layer protocols.
     *
     * @param supportedProtocols The supported application layer protocols.
     * @return The HTTP server.
     */
    HttpServer supportedProtocols(List<String> supportedProtocols);

    /**
     * Create a TLS engine using advisory peer information.
     * Applications using this factory method are providing hints for an internal session reuse strategy.
     * Some cipher suites (such as Kerberos) require remote hostname information, in which case peerHost needs to be specified.
     *
     * @param peerHost the non-authoritative name of the host.
     * @return The HTTP server.
     */
    HttpServer peerHost(String peerHost);

    /**
     * Create a TLS engine using advisory peer information.
     * Applications using this factory method are providing hints for an internal session reuse strategy.
     * Some cipher suites (such as Kerberos) require remote hostname information, in which case peerHost needs to be specified.
     *
     * @param peerPort the non-authoritative port.
     * @return The HTTP server.
     */
    HttpServer peerPort(int peerPort);

    /**
     * Enable the TLS protocol over the TCP connection.
     *
     * @return The HTTP server.
     */
    HttpServer enableSecureConnection();

    /**
     * Set the TCP idle timeout. The unit is second.
     *
     * @param timeout The TCP idle timeout. Time unit is second.
     * @return The HTTP server.
     */
    HttpServer timeout(Long timeout);

    /**
     * Bind a server TCP address
     *
     * @param address The server TCP address.
     */
    void listen(SocketAddress address);

    /**
     * Bind the server host and port.
     *
     * @param host The server host.
     * @param port The server port.
     */
    default void listen(String host, int port) {
        listen(new InetSocketAddress(host, port));
    }
}
