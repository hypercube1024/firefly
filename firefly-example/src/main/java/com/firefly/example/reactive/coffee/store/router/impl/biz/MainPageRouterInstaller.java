package com.firefly.example.reactive.coffee.store.router.impl.biz;

import com.firefly.annotation.Component;
import com.firefly.annotation.Inject;
import com.firefly.example.reactive.coffee.store.router.RouterInstaller;
import com.firefly.server.http2.HTTP2ServerBuilder;

/**
 * @author Pengtao Qiu
 */
@Component("mainPageRouterInstaller")
public class MainPageRouterInstaller implements RouterInstaller {

    @Inject
    private HTTP2ServerBuilder server;

    @Inject
    private MainPageHandler mainPageHandler;

    @Override
    public void install() {
        // main page
        server.router().get("/").asyncHandler(mainPageHandler);
    }
}
