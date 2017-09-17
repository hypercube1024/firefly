package com.firefly.example.reactive.coffee.store.router.impl.biz;

import com.firefly.annotation.Component;
import com.firefly.annotation.Inject;
import com.firefly.codec.http2.model.HttpHeader;
import com.firefly.codec.http2.model.MimeTypes;
import com.firefly.example.reactive.coffee.store.ProjectConfig;
import com.firefly.example.reactive.coffee.store.router.RouterInstaller;
import com.firefly.server.http2.HTTP2ServerBuilder;

import java.util.Collections;

/**
 * @author Pengtao Qiu
 */
@Component("mainPageRouterInstaller")
public class MainPageRouterInstaller implements RouterInstaller {

    @Inject
    private HTTP2ServerBuilder server;

    @Inject
    private ProjectConfig config;

    @Override
    public void install() {
        // main page
        server.router().get("/").asyncHandler(ctx -> {
            ctx.put(HttpHeader.CONTENT_TYPE, MimeTypes.Type.TEXT_HTML.asString())
               .renderTemplate(config.getTemplateRoot() + "/index.mustache", Collections.emptyList());
            ctx.succeed(true);
        });
    }
}
