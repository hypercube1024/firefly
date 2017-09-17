package com.firefly.example.reactive.coffee.store.router.impl.sys;

import com.firefly.annotation.Component;
import com.firefly.annotation.Inject;
import com.firefly.example.reactive.coffee.store.router.RouterInstaller;
import com.firefly.server.http2.HTTP2ServerBuilder;

/**
 * @author Pengtao Qiu
 */
@Component("sysRouterInstaller")
public class SysRouterInstaller implements RouterInstaller {

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
    }

    @Override
    public Integer order() {
        return 0;
    }
}
