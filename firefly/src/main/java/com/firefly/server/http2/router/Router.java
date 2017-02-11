package com.firefly.server.http2.router;

import com.firefly.codec.http2.model.HttpMethod;
import com.firefly.utils.function.Action1;

/**
 * @author Pengtao Qiu
 */
public interface Router {

    enum HTTPEvent {
        HEADER_COMPLETE, CONTENT_COMPLETE, MESSAGE_COMPLETE
    }

    Router path(String url);

    Router pathRegex(String regex);

    Router method(HttpMethod httpMethod);

    Router get(String url);

    Router post(String url);

    Router put(String url);

    Router delete(String url);

    Router consumes(String contentType);

    Router produces(String contentType);

    Router handler(Action1<RoutingContext> context);

    Router handler(Action1<RoutingContext> context, HTTPEvent... event);

    Router interest(HTTPEvent event);

    Router interest(HTTPEvent... event);
}
