package com.firefly.example.reactive.coffee.store.router.impl.biz;

import com.firefly.annotation.Component;
import com.firefly.annotation.Inject;
import com.firefly.example.reactive.coffee.store.router.RouterInstaller;
import com.firefly.example.reactive.coffee.store.service.ProductService;
import com.firefly.example.reactive.coffee.store.vo.ProductQuery;
import com.firefly.example.reactive.coffee.store.vo.ProductStatus;
import com.firefly.server.http2.HTTP2ServerBuilder;

/**
 * @author Pengtao Qiu
 */
@Component("productRouterInstaller")
public class ProductRouterInstaller implements RouterInstaller {

    @Inject
    private HTTP2ServerBuilder server;

    @Inject
    private ProductService productService;

    @Override
    public void install() {
        // list products
        server.router().get("/products").asyncHandler(ctx -> {
            Integer type = ctx.getParamOpt("type").map(Integer::parseInt).orElse(0);
            Integer pageNumber = ctx.getParamOpt("pageNumber").map(Integer::parseInt).orElse(1);
            Integer pageSize = ctx.getParamOpt("pageSize").map(Integer::parseInt).orElse(5);
            ProductQuery query = new ProductQuery(ProductStatus.ENABLE.getValue(), type, pageNumber, pageSize);
            productService.list(query).subscribe(productPage -> ctx.writeJson(productPage).succeed(true), ctx::fail);
        });
    }
}
