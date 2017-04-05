package com.firefly.server.http2.router.handler.error;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ServiceLoader;

/**
 * @author Pengtao Qiu
 */
public class DefaultErrorResponseHandlerLoader {

    private static final Logger log = LoggerFactory.getLogger("firefly-system");

    private static DefaultErrorResponseHandlerLoader ourInstance = new DefaultErrorResponseHandlerLoader();

    public static DefaultErrorResponseHandlerLoader getInstance() {
        return ourInstance;
    }

    private AbstractErrorResponseHandler handler;

    private DefaultErrorResponseHandlerLoader() {
        ServiceLoader<AbstractErrorResponseHandler> serviceLoader = ServiceLoader.load(AbstractErrorResponseHandler.class);
        for (AbstractErrorResponseHandler h : serviceLoader) {
            handler = h;
            log.info("load AbstractErrorResponseHandler, implement class -> {}", h.getClass().getName());
        }
        if (handler == null) {
            handler = new DefaultErrorResponseHandler();
        }
        log.info("load AbstractErrorResponseHandler, selected -> {}", handler.getClass().getName());
    }

    public AbstractErrorResponseHandler getHandler() {
        return handler;
    }
}
