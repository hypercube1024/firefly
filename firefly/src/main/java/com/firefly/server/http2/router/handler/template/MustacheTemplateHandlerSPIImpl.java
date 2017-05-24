package com.firefly.server.http2.router.handler.template;

import com.firefly.server.http2.router.RoutingContext;
import com.firefly.server.http2.router.spi.TemplateHandlerSPI;
import com.github.mustachejava.DefaultMustacheFactory;
import com.github.mustachejava.Mustache;
import com.github.mustachejava.MustacheFactory;

import java.io.PrintWriter;
import java.util.List;

/**
 * @author Pengtao Qiu
 */
public class MustacheTemplateHandlerSPIImpl implements TemplateHandlerSPI {

    private final MustacheFactory mustacheFactory;

    public MustacheTemplateHandlerSPIImpl() {
        this.mustacheFactory = new DefaultMustacheFactory();
    }

    public MustacheTemplateHandlerSPIImpl(MustacheFactory mustacheFactory) {
        this.mustacheFactory = mustacheFactory;
    }

    @Override
    public void renderTemplate(RoutingContext routingContext, String resourceName, Object scope) {
        Mustache mustache = mustacheFactory.compile(resourceName);
        try (PrintWriter writer = routingContext.getResponse().getPrintWriter()) {
            mustache.execute(writer, scope);
        }
    }

    @Override
    public void renderTemplate(RoutingContext routingContext, String resourceName, Object[] scopes) {
        Mustache mustache = mustacheFactory.compile(resourceName);
        try (PrintWriter writer = routingContext.getResponse().getPrintWriter()) {
            mustache.execute(writer, scopes);
        }
    }

    @Override
    public void renderTemplate(RoutingContext routingContext, String resourceName, List<Object> scopes) {
        Mustache mustache = mustacheFactory.compile(resourceName);
        try (PrintWriter writer = routingContext.getResponse().getPrintWriter()) {
            mustache.execute(writer, scopes);
        }
    }
}
