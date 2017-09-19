package com.firefly.example.reactive.coffee.store.router.impl.sys;

import com.firefly.annotation.Component;
import com.firefly.annotation.Inject;
import com.firefly.codec.http2.model.HttpHeader;
import com.firefly.example.reactive.coffee.store.router.RouterInstaller;
import com.firefly.server.http2.HTTP2ServerBuilder;
import com.firefly.server.http2.router.handler.file.StaticFileHandler;
import com.firefly.server.http2.router.handler.session.LocalHTTPSessionHandler;
import com.firefly.utils.log.slf4j.ext.LazyLogger;

import java.net.URISyntaxException;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * @author Pengtao Qiu
 */
@Component("sysRouterInstaller")
public class SysRouterInstaller implements RouterInstaller {

    private static final LazyLogger logger = LazyLogger.create();

    @Inject
    private HTTP2ServerBuilder server;

    @Inject
    private GlobalHandler globalHandler;

    @Inject
    private TransactionalHandler transactionalHandler;

    @Inject
    private LocalHTTPSessionHandler localHTTPSessionHandler;

    @Override
    public void install() {
        // global handler
        server.router().path("*").handler(globalHandler);

        // local session
        server.router().path("*").handler(localHTTPSessionHandler);

        // HTTP transaction manager
        server.router().methods(transactionalHandler.getMethods()).handler(transactionalHandler);

        // static file
        try {
            Path path = Paths.get(SysRouterInstaller.class.getResource("/").toURI());
            StaticFileHandler staticFileHandler = new StaticFileHandler(path.toAbsolutePath().toString());
            server.router().get("/static/*").handler(ctx -> {
                ctx.put(HttpHeader.CACHE_CONTROL, "max-age=86400");
                staticFileHandler.handle(ctx);
            });
        } catch (URISyntaxException e) {
            logger.error(() -> "setup static file router exception", e);
        }
    }

    @Override
    public Integer order() {
        return 0;
    }
}
