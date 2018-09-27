package com.firefly.example.reactive.coffee.store.router.impl.biz;

import com.firefly.$;
import com.firefly.annotation.Component;
import com.firefly.annotation.Inject;
import com.firefly.codec.http2.model.HttpHeader;
import com.firefly.codec.http2.model.MimeTypes;
import com.firefly.example.reactive.coffee.store.ProjectConfig;
import com.firefly.example.reactive.coffee.store.service.ProductService;
import com.firefly.example.reactive.coffee.store.vo.MainPage;
import com.firefly.example.reactive.coffee.store.vo.ProductQuery;
import com.firefly.example.reactive.coffee.store.vo.ProductStatus;
import com.firefly.example.reactive.coffee.store.vo.UserInfo;
import com.firefly.server.http2.router.Handler;
import com.firefly.server.http2.router.RoutingContext;
import reactor.core.publisher.Mono;

/**
 * @author Pengtao Qiu
 */
@Component("mainPageHandler")
public class MainPageHandler implements Handler {

    @Inject
    private ProjectConfig config;

    @Inject
    private ProductService productService;

    @Override
    public void handle(RoutingContext ctx) {
        listProducts(ctx).subscribe(mainPage -> {
            mainPage.setUserInfo((UserInfo) ctx.getAttribute(config.getLoginUserKey()));
            ctx.put(HttpHeader.CONTENT_TYPE, MimeTypes.Type.TEXT_HTML.asString())
               .renderTemplate(config.getTemplateRoot() + "/index.mustache", mainPage);
            ctx.succeed(true);
        }, ctx::fail);
    }

    public ProductQuery toProductQuery(RoutingContext ctx) {
        String searchKey = ctx.getParameter("searchKey");
        Integer type = ctx.getParamOpt("type").filter($.string::hasText).map(Integer::parseInt).orElse(0);
        Integer pageNumber = ctx.getParamOpt("pageNumber").filter($.string::hasText).map(Integer::parseInt).orElse(1);
        Integer pageSize = ctx.getParamOpt("pageSize").filter($.string::hasText).map(Integer::parseInt).orElse(5);
        return new ProductQuery(searchKey, ProductStatus.ENABLE.getValue(), type, pageNumber, pageSize);
    }

    public Mono<MainPage> listProducts(RoutingContext ctx) {
        ProductQuery query = toProductQuery(ctx);
        return productService.list(query).map(productPage -> {
            MainPage mainPage = new MainPage();
            mainPage.setProducts(productPage);
            mainPage.setSearchKey(query.getSearchKey());
            mainPage.setType(query.getType());
            mainPage.setUserInfo((UserInfo) ctx.getAttribute(config.getLoginUserKey()));
            return mainPage;
        });
    }
}
