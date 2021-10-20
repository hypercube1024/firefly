package com.fireflysource;

import com.fireflysource.common.concurrent.CompletableFutures;
import com.fireflysource.common.slf4j.LazyLogger;
import com.fireflysource.common.sys.Result;
import com.fireflysource.net.CommonTcpChannelGroup;
import com.fireflysource.net.http.client.HttpClient;
import com.fireflysource.net.http.client.HttpClientFactory;
import com.fireflysource.net.http.common.HttpConfig;
import com.fireflysource.net.http.server.HttpProxy;
import com.fireflysource.net.http.server.HttpServer;
import com.fireflysource.net.http.server.HttpServerFactory;
import com.fireflysource.net.tcp.TcpClient;
import com.fireflysource.net.tcp.TcpClientFactory;
import com.fireflysource.net.tcp.TcpServer;
import com.fireflysource.net.tcp.TcpServerFactory;
import com.fireflysource.net.tcp.aio.TcpConfig;
import com.fireflysource.net.websocket.client.WebSocketClientConnectionBuilder;

import java.util.concurrent.CompletableFuture;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * The Firefly functions start from here.
 *
 * @author Pengtao Qiu
 */
public interface $ {

    /**
     * Get the HTTP client. It is singleton. The client uses the common tcp channel group.
     *
     * @return The HTTP client.
     */
    static HttpClient httpClient() {
        return CommonTcpChannelGroup.INSTANCE.getHttpClient();
    }

    /**
     * Create a websocket client connection builder.
     *
     * @param url The websocket url.
     * @return The websocket client connection builder.
     */
    static WebSocketClientConnectionBuilder websocket(String url) {
        return httpClient().websocket(url);
    }

    /**
     * Create a websocket client connection builder.
     *
     * @return The websocket client connection builder.
     */
    static WebSocketClientConnectionBuilder websocket() {
        return httpClient().websocket();
    }

    /**
     * Create the HTTP client. The client uses the common tcp channel group.
     *
     * @param httpConfig The HTTP config.
     * @return The HTTP client.
     */
    static HttpClient httpClient(HttpConfig httpConfig) {
        return CommonTcpChannelGroup.INSTANCE.createHttpClient(httpConfig);
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
     * Create the HTTP server. It uses the common tcp channel group.
     *
     * @return The HTTP server.
     */
    static HttpServer httpServer() {
        return CommonTcpChannelGroup.INSTANCE.createHttpServer();
    }

    /**
     * Create the HTTP server. It uses the common tcp channel group.
     *
     * @param httpConfig The HTTP config.
     * @return The HTTP server.
     */
    static HttpServer httpServer(HttpConfig httpConfig) {
        return CommonTcpChannelGroup.INSTANCE.createHttpServer(httpConfig);
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
     * Create a new HTTP proxy.
     *
     * @return The HTTP proxy.
     */
    static HttpProxy createHttpProxy() {
        return HttpServerFactory.createHttpProxy();
    }

    /**
     * Create a new HTTP proxy.
     *
     * @param httpConfig The HTTP config.
     * @return The HTTP proxy.
     */
    static HttpProxy createHttpProxy(HttpConfig httpConfig) {
        return HttpServerFactory.createHttpProxy(httpConfig);
    }

    /**
     * Create the TCP client. It uses the common tcp channel group.
     *
     * @return The TCP client.
     */
    static TcpClient tcpClient() {
        return CommonTcpChannelGroup.INSTANCE.createTcpClient();
    }

    /**
     * Create the TCP client. It uses the common tcp channel group.
     *
     * @param tcpConfig The TCP config.
     * @return The TCP client.
     */
    static TcpClient tcpClient(TcpConfig tcpConfig) {
        return CommonTcpChannelGroup.INSTANCE.createTcpClient(tcpConfig);
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
     * Create the TCP server. It uses the common tcp channel group.
     *
     * @return The TCP server.
     */
    static TcpServer tcpServer() {
        return CommonTcpChannelGroup.INSTANCE.createTcpServer();
    }

    /**
     * Create the TCP server. It uses the common tcp channel group.
     *
     * @param tcpConfig The TCP config.
     * @return The TCP server.
     */
    static TcpServer tcpServer(TcpConfig tcpConfig) {
        return CommonTcpChannelGroup.INSTANCE.createTcpServer(tcpConfig);
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

    /**
     * The logger functions.
     */
    interface logger {
        /**
         * Create a lazy logger.
         *
         * @param name The logger name.
         * @return The lazy logger.
         */
        static LazyLogger create(String name) {
            return LazyLogger.create(name);
        }

        /**
         * Create a lazy logger.
         *
         * @param clazz The class name as the logger name.
         * @return The lazy logger.
         */
        static LazyLogger create(Class<?> clazz) {
            return LazyLogger.create(clazz);
        }
    }

    /**
     * The future functions.
     */
    interface future {

        /**
         * Done future.
         *
         * @return The done future.
         */
        static CompletableFuture<Void> done() {
            return Result.DONE;
        }

        /**
         * Done future.
         *
         * @param future The future.
         */
        static void done(CompletableFuture<Void> future) {
            Result.done(future);
        }

        /**
         * Create a failed future.
         *
         * @param t   The exception.
         * @param <T> The future item type.
         * @return The future.
         */
        static <T> CompletableFuture<T> failedFuture(Throwable t) {
            return CompletableFutures.failedFuture(t);
        }

        /**
         * Retry the async operation.
         *
         * @param retryCount   The max retry times.
         * @param supplier     The async operation function.
         * @param prepareRetry The callback before retries async operation.
         * @param <T>          The future result type.
         * @return The operation result future.
         */
        static <T> CompletableFuture<T> retry(int retryCount, Supplier<CompletableFuture<T>> supplier, BiConsumer<Throwable, Integer> prepareRetry) {
            return CompletableFutures.retry(retryCount, supplier, prepareRetry);
        }

    }

    /**
     * The consumer functions.
     */
    interface consumer {

        /**
         * Discard the result.
         *
         * @param <T> The result type.
         * @return The consumer that discards the result.
         */
        static <T> Consumer<Result<T>> discard() {
            return Result.discard();
        }

        /**
         * Convert future to the result consumer.
         *
         * @param future The future.
         * @param <T>    The result type.
         * @return The result consumer.
         */
        static <T> Consumer<Result<T>> futureToConsumer(CompletableFuture<T> future) {
            return Result.futureToConsumer(future);
        }

        /**
         * The empty consumer.
         *
         * @param <T> The consumer item type.
         * @return The empty consumer.
         */
        static <T> Consumer<T> emptyConsumer() {
            return Result.emptyConsumer();
        }

        /**
         * Create the failed result.
         *
         * @param t The exception.
         * @return The failed result.
         */
        static Result<Void> createFailedResult(Throwable t) {
            return Result.createFailedResult(t);
        }

        /**
         * The success result.
         *
         * @return The success result.
         */
        static Result<Void> success() {
            return Result.SUCCESS;
        }
    }
}
