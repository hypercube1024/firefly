package com.firefly.example.reactive.coffee.store;

import com.firefly.$;
import com.firefly.example.reactive.coffee.store.router.GlobalHandler;
import com.firefly.example.reactive.coffee.store.router.TransactionalHandler;
import com.firefly.example.reactive.coffee.store.service.ProductService;
import com.firefly.example.reactive.coffee.store.utils.DBUtils;
import com.firefly.example.reactive.coffee.store.vo.ProductQuery;
import com.firefly.example.reactive.coffee.store.vo.ProductStatus;
import com.firefly.server.http2.HTTP2ServerBuilder;

import java.util.Collections;
import java.util.Optional;

/**
 * @author Pengtao Qiu
 */
public class AppMain {


    private static final String root = "template/coffeeStore";

    public static final HTTP2ServerBuilder s = $.httpServer();

    public static void initData() {
        DBUtils dbUtils = $.getBean(DBUtils.class);
        dbUtils.createTables();
        dbUtils.initializeData();
    }

    public static void setupRouters() {
        // global handler
        s.router().path("/*").handler($.getBean(GlobalHandler.class));

        // HTTP transaction manager
        TransactionalHandler transactionalHandler = $.getBean(TransactionalHandler.class);
        s.router().methods(transactionalHandler.getMethods()).handler(transactionalHandler);

        // index page
        s.router().get("/").asyncHandler(ctx -> {
            ctx.renderTemplate(root + "/index.mustache", Collections.emptyList());
            ctx.succeed(true);
        });

        ProductService productService = $.getBean(ProductService.class);
        // list products
        s.router().get("/products").asyncHandler(ctx -> {
            Integer type = Optional.ofNullable(ctx.getParameter("type"))
                                   .map(Integer::parseInt)
                                   .orElse(0);
            Integer pageNumber = Optional.ofNullable(ctx.getParameter("pageNumber"))
                                         .map(Integer::parseInt)
                                         .orElse(1);
            Integer pageSize = Optional.ofNullable(ctx.getParameter("pageSize"))
                                       .map(Integer::parseInt)
                                       .orElse(5);
            ProductQuery query = new ProductQuery(ProductStatus.ENABLE.getValue(), type, pageNumber, pageSize);
            productService.list(query).subscribe(productPage -> ctx.writeJson(productPage).succeed(true), ctx::fail);
        });
    }

    public static void main(String[] args) {
        initData();
        setupRouters();
        s.listen("localhost", 8080);
    }
}
