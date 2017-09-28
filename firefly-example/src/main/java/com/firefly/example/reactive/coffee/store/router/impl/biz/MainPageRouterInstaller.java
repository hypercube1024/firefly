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

    @Inject
    private OrderHandler orderHandler;

    @Override
    public void install() {
        // main page
        server.router().get("/").asyncHandler(mainPageHandler)
              .router().get("/hello").handler(ctx -> ctx.write("hello").succeed(true))
              .router().post("/product/buy").handler(orderHandler);

    }
}
