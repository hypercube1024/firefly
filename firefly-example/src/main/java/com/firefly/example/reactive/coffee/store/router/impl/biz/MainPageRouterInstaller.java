package com.firefly.example.reactive.coffee.store.router.impl.biz;

import com.firefly.annotation.Component;
import com.firefly.annotation.Inject;
import com.firefly.codec.http2.model.HttpHeader;
import com.firefly.codec.http2.model.MimeTypes;
import com.firefly.example.reactive.coffee.store.ProjectConfig;
import com.firefly.example.reactive.coffee.store.router.RouterInstaller;
import com.firefly.example.reactive.coffee.store.service.ProductService;
import com.firefly.example.reactive.coffee.store.vo.MainPage;
import com.firefly.example.reactive.coffee.store.vo.ProductQuery;
import com.firefly.example.reactive.coffee.store.vo.ProductStatus;
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

    @Inject
    private ProductService productService;

    @Override
    public void install() {
        // main page
        server.router().get("/").asyncHandler(ctx -> {
            Integer type = ctx.getParamOpt("type").map(Integer::parseInt).orElse(0);
            Integer pageNumber = ctx.getParamOpt("pageNumber").map(Integer::parseInt).orElse(1);
            Integer pageSize = ctx.getParamOpt("pageSize").map(Integer::parseInt).orElse(5);
            ProductQuery query = new ProductQuery(ProductStatus.ENABLE.getValue(), type, pageNumber, pageSize);
            productService.list(query).subscribe(productPage -> {
                MainPage mainPage = new MainPage();
                mainPage.setProducts(productPage);
                ctx.put(HttpHeader.CONTENT_TYPE, MimeTypes.Type.TEXT_HTML.asString())
                   .renderTemplate(config.getTemplateRoot() + "/index.mustache", mainPage);
                ctx.succeed(true);
            }, ctx::fail);
        });
    }
}
