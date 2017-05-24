package com.firefly.server.http2.router.spi;

import com.firefly.server.http2.router.RoutingContext;

import java.util.List;

/**
 * @author Pengtao Qiu
 */
public interface TemplateHandlerSPI {

    void renderTemplate(RoutingContext ctx, String resourceName, Object scope);

    void renderTemplate(RoutingContext ctx, String resourceName, Object[] scopes);

    void renderTemplate(RoutingContext ctx, String resourceName, List<Object> scopes);

}
