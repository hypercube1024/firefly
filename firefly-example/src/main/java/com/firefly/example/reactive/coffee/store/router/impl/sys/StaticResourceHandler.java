package com.firefly.example.reactive.coffee.store.router.impl.sys;

import com.firefly.annotation.Component;
import com.firefly.codec.http2.model.HttpHeader;
import com.firefly.server.http2.router.Handler;
import com.firefly.server.http2.router.RoutingContext;
import com.firefly.server.http2.router.handler.file.StaticFileHandler;
import com.firefly.utils.exception.CommonRuntimeException;
import com.firefly.utils.log.slf4j.ext.LazyLogger;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;

/**
 * @author Pengtao Qiu
 */
@Component("staticResourceHandler")
public class StaticResourceHandler implements Handler {

    public static final LazyLogger log = LazyLogger.create();

    private final List<String> staticResources = Arrays.asList("/favicon.ico", "/static/*");
    private final StaticFileHandler staticFileHandler;

    public StaticResourceHandler() {
        try {
            Path path = Paths.get(SysRouterInstaller.class.getResource("/").toURI());
            staticFileHandler = new StaticFileHandler(path.toAbsolutePath().toString());
        } catch (Exception e) {
            throw new CommonRuntimeException(e);
        }
    }

    @Override
    public void handle(RoutingContext ctx) {
        log.info(() -> "static file request -> " + ctx.getURI());
        ctx.put(HttpHeader.CACHE_CONTROL, "max-age=86400");
        staticFileHandler.handle(ctx);
    }

    public List<String> getStaticResources() {
        return staticResources;
    }

    public StaticFileHandler getStaticFileHandler() {
        return staticFileHandler;
    }
}
