package com.fireflysource.net.http.server;

import com.fireflysource.common.lifecycle.LifeCycle;
import com.fireflysource.net.http.common.model.HttpMethod;
import com.fireflysource.net.tcp.secure.SecureEngineFactory;

import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.List;

/**
 * @author Pengtao Qiu
 */
public interface HttpServer extends LifeCycle {

    /**
     * Register a new router.
     *
     * @return The HTTP server.
     */
    HttpServer router();

    /**
     * Register a new router.
     *
     * @param id The router id.
     * @return The HTTP server.
     */
    HttpServer router(int id);

    /**
     * Bind a URL for this router.
     *
     * @param url The URL.
     * @return The HTTP server.
     */
    HttpServer path(String url);

    /**
     * Bind some URLs for this router.
     *
     * @param urlList The URL list.
     * @return The HTTP server.
     */
    HttpServer paths(List<String> urlList);

    /**
     * Bind URL using regex.
     *
     * @param regex The URL regex.
     * @return The HTTP server.
     */
    HttpServer pathRegex(String regex);

    /**
     * Bind HTTP method.
     *
     * @param httpMethod The HTTP method.
     * @return The HTTP server.
     */
    HttpServer method(String httpMethod);

    /**
     * Bind HTTP method.
     *
     * @param httpMethod The HTTP method.
     * @return The HTTP server.
     */
    HttpServer method(HttpMethod httpMethod);

    /**
     * Bind get method and URL.
     *
     * @param url The URL.
     * @return The HTTP server.
     */
    HttpServer get(String url);

    /**
     * Bind post method and URL.
     *
     * @param url The URL.
     * @return The HTTP server.
     */
    HttpServer post(String url);

    /**
     * Bind put method and URL.
     *
     * @param url The URL.
     * @return The HTTP server.
     */
    HttpServer put(String url);

    /**
     * Bind delete method and URL.
     *
     * @param url The URL.
     * @return The HTTP server.
     */
    HttpServer delete(String url);

    /**
     * Bind the request content type.
     *
     * @param contentType The request content type.
     * @return The HTTP server.
     */
    HttpServer consumes(String contentType);

    /**
     * Bind remote accepted content type.
     *
     * @param accept The remote accepted content type.
     * @return The HTTP server.
     */
    HttpServer produces(String accept);

    /**
     * Set router handler. When the HTTP server accepted request, and the request match this router,
     * the server will call this handler to process request.
     *
     * @param handler router handler.
     * @return The HTTP server.
     */
    HttpServer handler(Router.Handler handler);

    /**
     * Enable this router.
     *
     * @return The HTTP server.
     */
    HttpServer enable();

    /**
     * Disable this router.
     *
     * @return The HTTP server.
     */
    HttpServer disable();

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
     * @return The HTTP server.
     */
    HttpServer listen(SocketAddress address);

    /**
     * Bind the server host and port.
     *
     * @param host The server host.
     * @param port The server port.
     * @return The HTTP server.
     */
    default HttpServer listen(String host, int port) {
        return listen(new InetSocketAddress(host, port));
    }
}
