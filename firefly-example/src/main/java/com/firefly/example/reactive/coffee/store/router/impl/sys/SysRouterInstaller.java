package com.firefly.example.reactive.coffee.store.router.impl.sys;

import com.firefly.annotation.Component;
import com.firefly.annotation.Inject;
import com.firefly.codec.http2.model.HttpMethod;
import com.firefly.example.reactive.coffee.store.router.RouterInstaller;
import com.firefly.server.http2.HTTP2ServerBuilder;
import com.firefly.server.http2.router.handler.session.LocalHTTPSessionHandler;
import com.firefly.utils.log.slf4j.ext.LazyLogger;

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
    private LocalHTTPSessionHandler localHTTPSessionHandler;

    @Inject
    private LoginHandler loginHandler;

    @Inject
    private StaticResourceHandler staticResourceHandler;

    @Override
    public void install() {
        server.router().path("*").handler(globalHandler)
              .router().path("*").handler(localHTTPSessionHandler)
              .router().path("*").handler(loginHandler)
              .router().method(HttpMethod.GET).paths(staticResourceHandler.getStaticResources()).handler(staticResourceHandler);
    }

    @Override
    public Integer order() {
        return 0;
    }
}
