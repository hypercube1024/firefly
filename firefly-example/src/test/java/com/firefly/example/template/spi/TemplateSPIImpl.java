package com.firefly.example.template.spi;

import com.firefly.server.http2.router.RoutingContext;
import com.firefly.server.http2.router.spi.TemplateHandlerSPI;

import java.util.List;

/**
 * @author Pengtao Qiu
 */
public class TemplateSPIImpl implements TemplateHandlerSPI {
    @Override
    public void renderTemplate(RoutingContext ctx, String resourceName, Object scope) {
        ctx.end("test template spi demo");
    }

    @Override
    public void renderTemplate(RoutingContext ctx, String resourceName, Object[] scopes) {
        ctx.end("test template spi demo");
    }

    @Override
    public void renderTemplate(RoutingContext ctx, String resourceName, List<Object> scopes) {
        ctx.end("test template spi demo");
    }
}
