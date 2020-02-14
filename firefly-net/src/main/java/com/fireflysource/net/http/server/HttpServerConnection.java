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

    HttpServerConnection listen(Listener listener);

    void begin();

    interface Listener {
        CompletableFuture<Void> onHeaderComplete(RoutingContext context);

        CompletableFuture<Void> onHttpRequestComplete(RoutingContext context);

        class Adapter implements Listener {

            @Override
            public CompletableFuture<Void> onHeaderComplete(RoutingContext context) {
                return Result.DONE;
            }

            @Override
            public CompletableFuture<Void> onHttpRequestComplete(RoutingContext context) {
                return Result.DONE;
            }
        }
    }


}
