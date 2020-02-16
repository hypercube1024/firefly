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


    HttpServerConnection setListener(Listener listener);

    /**
     * Begin to listen the http request.
     */
    void begin();

    HttpServerOutputChannel createHttpServerOutputChannel();

    interface Listener {
        CompletableFuture<Void> onHeaderComplete(RoutingContext context);

        CompletableFuture<Void> onHttpRequestComplete(RoutingContext context);

        CompletableFuture<Void> onException(RoutingContext context, Exception exception);

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
