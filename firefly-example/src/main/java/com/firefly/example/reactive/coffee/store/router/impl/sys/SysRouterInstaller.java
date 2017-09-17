package com.firefly.example.reactive.coffee.store.router.impl.sys;

import com.firefly.annotation.Component;
import com.firefly.annotation.Inject;
import com.firefly.example.reactive.coffee.store.router.RouterInstaller;
import com.firefly.server.http2.HTTP2ServerBuilder;
import com.firefly.server.http2.router.handler.file.StaticFileHandler;
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

    @Override
    public void install() {
        // global handler
        server.router().path("*").handler(globalHandler);

        // HTTP transaction manager
        server.router().methods(transactionalHandler.getMethods()).handler(transactionalHandler);

        // static file
        try {
            Path path = Paths.get(SysRouterInstaller.class.getResource("/").toURI());
            server.router().get("/static/*").handler(new StaticFileHandler(path.toAbsolutePath().toString()));
        } catch (URISyntaxException e) {
            logger.error(() -> "setup static file router exception", e);
        }
    }

    @Override
    public Integer order() {
        return 0;
    }
}
