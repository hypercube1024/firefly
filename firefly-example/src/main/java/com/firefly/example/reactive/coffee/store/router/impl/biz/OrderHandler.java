package com.firefly.example.reactive.coffee.store.router.impl.biz;

import com.firefly.annotation.Component;
import com.firefly.annotation.Inject;
import com.firefly.example.reactive.coffee.store.ProjectConfig;
import com.firefly.example.reactive.coffee.store.service.OrderService;
import com.firefly.example.reactive.coffee.store.vo.ProductBuyRequest;
import com.firefly.example.reactive.coffee.store.vo.Response;
import com.firefly.example.reactive.coffee.store.vo.ResponseStatus;
import com.firefly.example.reactive.coffee.store.vo.UserInfo;
import com.firefly.server.http2.router.Handler;
import com.firefly.server.http2.router.RoutingContext;

/**
 * @author Pengtao Qiu
 */
@Component("orderHandler")
public class OrderHandler implements Handler {

    @Inject
    private ProjectConfig config;

    @Inject
    private OrderService orderService;

    @Override
    public void handle(RoutingContext ctx) {
        UserInfo userInfo = (UserInfo) ctx.getAttribute(config.getLoginUserKey());
        if (userInfo == null) {
            ctx.fail(new IllegalStateException("The user does not login"));
            return;
        }

        ProductBuyRequest request = ctx.getJsonBody(ProductBuyRequest.class);
        if (request == null) {
            ctx.fail(new IllegalArgumentException("Buy request must be not null"));
            return;
        }

        request.setUserId(userInfo.getId());
        orderService.buy(request).subscribe(ret -> {
            Response<Boolean> response = new Response<>();
            response.setStatus(ResponseStatus.OK.getValue());
            response.setData(ret);
            response.setMessage(ResponseStatus.OK.getDescription());
            ctx.writeJson(response).end().succeed(ret);
        }, ctx::fail);
    }
}
