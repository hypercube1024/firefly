package com.firefly.server.http2.router.handler.error;

import com.firefly.utils.ServiceUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
        handler = ServiceUtils.loadService(AbstractErrorResponseHandler.class, new DefaultErrorResponseHandler());
        log.info("load AbstractErrorResponseHandler, selected -> {}", handler.getClass().getName());
    }

    public AbstractErrorResponseHandler getHandler() {
        return handler;
    }
}
