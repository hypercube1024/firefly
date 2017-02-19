package com.firefly.server.http2.router.handler.template;

import com.firefly.server.http2.router.Handler;
import com.firefly.server.http2.router.RoutingContext;
import com.github.mustachejava.DefaultMustacheFactory;
import com.github.mustachejava.MustacheFactory;

/**
 * @author Pengtao Qiu
 */
public class MustacheTemplateHandler implements Handler {

    private final MustacheFactory mustacheFactory;

    public MustacheTemplateHandler() {
        mustacheFactory = new DefaultMustacheFactory();
    }

    public MustacheTemplateHandler(String resourceRoot) {
        mustacheFactory = new DefaultMustacheFactory(resourceRoot);
    }

    @Override
    public void handle(RoutingContext routingContext) {
        routingContext.setTemplateHandlerSPI(new MustacheTemplateHandlerSPIImpl(mustacheFactory, routingContext));
        routingContext.next();
    }

}
