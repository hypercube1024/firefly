package com.firefly.server.http2.router.handler.template;

import com.firefly.server.http2.router.RoutingContext;
import com.firefly.server.http2.router.spi.TemplateHandlerSPI;
import com.github.mustachejava.Mustache;
import com.github.mustachejava.MustacheFactory;

import java.io.PrintWriter;
import java.util.List;

/**
 * @author Pengtao Qiu
 */
public class MustacheTemplateHandlerSPIImpl implements TemplateHandlerSPI {

    private final MustacheFactory mustacheFactory;
    private final RoutingContext routingContext;

    public MustacheTemplateHandlerSPIImpl(MustacheFactory mustacheFactory, RoutingContext routingContext) {
        this.mustacheFactory = mustacheFactory;
        this.routingContext = routingContext;
    }

    @Override
    public void renderTemplate(String resourceName, Object scope) {
        Mustache mustache = mustacheFactory.compile(resourceName);
        try (PrintWriter writer = routingContext.getResponse().getPrintWriter()) {
            mustache.execute(writer, scope);
        }
    }

    @Override
    public void renderTemplate(String resourceName, Object[] scopes) {
        Mustache mustache = mustacheFactory.compile(resourceName);
        try (PrintWriter writer = routingContext.getResponse().getPrintWriter()) {
            mustache.execute(writer, scopes);
        }
    }

    @Override
    public void renderTemplate(String resourceName, List<Object> scopes) {
        Mustache mustache = mustacheFactory.compile(resourceName);
        try (PrintWriter writer = routingContext.getResponse().getPrintWriter()) {
            mustache.execute(writer, scopes);
        }
    }
}
