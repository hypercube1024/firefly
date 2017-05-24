package com.firefly.server.http2.router;

/**
 * @author Pengtao Qiu
 */
public interface Handler {

    void handle(RoutingContext routingContext);

}
