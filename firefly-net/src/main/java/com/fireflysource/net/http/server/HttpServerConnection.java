package com.fireflysource.net.http.server;

import com.fireflysource.common.sys.Result;
import com.fireflysource.net.http.common.HttpConnection;

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
     * Begin to listen the http request.
     */
    void begin();

    /**
     * Create the HTTP server output channel. It outputs the HTTP response.
     *
     * @return The HTTP server output channel.
     */
    HttpServerOutputChannel createHttpServerOutputChannel();

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
         * @param exception An exception.
         * @return The future result.
         */
        CompletableFuture<Void> onException(RoutingContext context, Exception exception);

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
            public CompletableFuture<Void> onException(RoutingContext context, Exception exception) {
                return Result.DONE;
            }
        }
    }


}
