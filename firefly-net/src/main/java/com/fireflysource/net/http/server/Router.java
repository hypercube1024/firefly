package com.fireflysource.net.http.server;

import com.fireflysource.common.coroutine.CoroutineDispatchers;
import com.fireflysource.common.sys.Result;
import com.fireflysource.net.http.common.model.HttpMethod;

import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import java.util.function.Function;

public interface Router extends Comparable<Router> {

    Handler EMPTY_HANDLER = ctx -> Result.DONE;

    /**
     * Get router id.
     *
     * @return The router id.
     */
    int getId();

    /**
     * If true, the router is enable.
     *
     * @return If true, the router is enable.
     */
    boolean isEnable();

    /**
     * Get matched types.
     *
     * @return The matched types.
     */
    Set<Matcher.MatchType> getMatchTypes();

    /**
     * Bind a URL for this router.
     *
     * @param url The URL.
     * @return router.
     */
    Router path(String url);

    /**
     * Bind some URLs for this router.
     *
     * @param urlList The URL list.
     * @return router.
     */
    Router paths(List<String> urlList);

    /**
     * Bind URL using regex.
     *
     * @param regex The URL regex.
     * @return router.
     */
    Router pathRegex(String regex);

    /**
     * Bind HTTP method.
     *
     * @param httpMethod The HTTP method.
     * @return router.
     */
    Router method(String httpMethod);

    /**
     * Bind HTTP method.
     *
     * @param httpMethod The HTTP method.
     * @return router.
     */
    Router method(HttpMethod httpMethod);

    /**
     * Bind get method and URL.
     *
     * @param url The URL.
     * @return router.
     */
    Router get(String url);

    /**
     * Bind post method and URL.
     *
     * @param url The URL.
     * @return router.
     */
    Router post(String url);

    /**
     * Bind put method and URL.
     *
     * @param url The URL.
     * @return router.
     */
    Router put(String url);

    /**
     * Bind delete method and URL.
     *
     * @param url The URL.
     * @return router.
     */
    Router delete(String url);

    /**
     * Bind the request content type.
     *
     * @param contentType The request content type.
     * @return router.
     */
    Router consumes(String contentType);

    /**
     * Bind remote accepted content type.
     *
     * @param accept The remote accepted content type.
     * @return router.
     */
    Router produces(String accept);

    /**
     * Set router handler. When the HTTP server accepted request, and the request match this router,
     * the server will call this handler to process request.
     *
     * @param handler router handler.
     * @return The HTTP server.
     */
    HttpServer handler(Handler handler);

    /**
     * Set router handler. The handler executes thread blocking task on the IO thread pool.
     *
     * @param consumer The thread blocking router handler.
     * @return The HTTP server.
     */
    default HttpServer blockingHandler(Consumer<RoutingContext> consumer) {
        return this.handler(ctx -> CompletableFuture.runAsync(() -> consumer.accept(ctx),
                CoroutineDispatchers.INSTANCE.getIoBlockingThreadPool()));
    }

    /**
     * Enable this router.
     *
     * @return router.
     */
    Router enable();

    /**
     * Disable this router.
     *
     * @return router.
     */
    Router disable();

    interface Handler extends Function<RoutingContext, CompletableFuture<Void>> {

    }
}
