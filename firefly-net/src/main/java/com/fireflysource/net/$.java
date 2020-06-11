package com.fireflysource.net;

import com.fireflysource.net.http.client.HttpClient;
import com.fireflysource.net.http.client.HttpClientFactory;
import com.fireflysource.net.http.common.HttpConfig;
import com.fireflysource.net.http.server.HttpServer;
import com.fireflysource.net.http.server.HttpServerFactory;
import com.fireflysource.net.tcp.TcpClient;
import com.fireflysource.net.tcp.TcpClientFactory;
import com.fireflysource.net.tcp.TcpServer;
import com.fireflysource.net.tcp.TcpServerFactory;
import com.fireflysource.net.tcp.aio.TcpConfig;

/**
 * @author Pengtao Qiu
 */
public interface $ {

    /**
     * Get the HTTP client. It is singleton. The client shares global tcp channel group.
     *
     * @return The HTTP client.
     */
    static HttpClient httpClient() {
        return SharedTcpChannelGroup.INSTANCE.getHttpClient();
    }

    /**
     * Create the HTTP client. The client shares global tcp channel group.
     *
     * @param httpConfig The HTTP config.
     * @return The HTTP client.
     */
    static HttpClient httpClient(HttpConfig httpConfig) {
        return SharedTcpChannelGroup.INSTANCE.createHttpClient(httpConfig);
    }

    /**
     * Create a new HTTP client.
     *
     * @return The HTTP client.
     */
    static HttpClient createHttpClient() {
        return HttpClientFactory.create();
    }

    /**
     * Create a new HTTP client.
     *
     * @param httpConfig The HTTP config.
     * @return The HTTP client.
     */
    static HttpClient createHttpClient(HttpConfig httpConfig) {
        return HttpClientFactory.create(httpConfig);
    }

    /**
     * Create the HTTP server. It shares global tcp channel group.
     *
     * @return The HTTP server.
     */
    static HttpServer httpServer() {
        return SharedTcpChannelGroup.INSTANCE.createHttpServer();
    }

    /**
     * Create the HTTP server. It shares global tcp channel group.
     *
     * @param httpConfig The HTTP config.
     * @return The HTTP server.
     */
    static HttpServer httpServer(HttpConfig httpConfig) {
        return SharedTcpChannelGroup.INSTANCE.createHttpServer(httpConfig);
    }

    /**
     * Create a new HTTP server.
     *
     * @return The HTTP server.
     */
    static HttpServer createHttpServer() {
        return HttpServerFactory.create();
    }

    /**
     * Create a new HTTP server.
     *
     * @param httpConfig The HTTP config.
     * @return The HTTP server.
     */
    static HttpServer createHttpServer(HttpConfig httpConfig) {
        return HttpServerFactory.create(httpConfig);
    }

    /**
     * Create the TCP client. It shares global tcp channel group.
     *
     * @return The TCP client.
     */
    static TcpClient tcpClient() {
        return SharedTcpChannelGroup.INSTANCE.createTcpClient();
    }

    /**
     * Create the TCP client. It shares global tcp channel group.
     *
     * @param tcpConfig The TCP config.
     * @return The TCP client.
     */
    static TcpClient tcpClient(TcpConfig tcpConfig) {
        return SharedTcpChannelGroup.INSTANCE.createTcpClient(tcpConfig);
    }

    /**
     * Create a new TCP client.
     *
     * @return The TCP client.
     */
    static TcpClient createTcpClient() {
        return TcpClientFactory.create();
    }

    /**
     * Create a new TCP client.
     *
     * @param tcpConfig The TCP config.
     * @return The TCP client.
     */
    static TcpClient createTcpClient(TcpConfig tcpConfig) {
        return TcpClientFactory.create(tcpConfig);
    }

    /**
     * Create the TCP server. It shares global tcp channel group.
     *
     * @return The TCP server.
     */
    static TcpServer tcpServer() {
        return SharedTcpChannelGroup.INSTANCE.createTcpServer();
    }

    /**
     * Create the TCP server. It shares global tcp channel group.
     *
     * @param tcpConfig The TCP config.
     * @return The TCP server.
     */
    static TcpServer tcpServer(TcpConfig tcpConfig) {
        return SharedTcpChannelGroup.INSTANCE.createTcpServer(tcpConfig);
    }

    /**
     * Create a new TCP server.
     *
     * @return The TCP server.
     */
    static TcpServer createTcpServer() {
        return TcpServerFactory.create();
    }

    /**
     * Create a new TCP server.
     *
     * @param tcpConfig The TCP config.
     * @return The TCP server.
     */
    static TcpServer createTcpServer(TcpConfig tcpConfig) {
        return TcpServerFactory.create(tcpConfig);
    }
}
