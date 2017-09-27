package com.firefly.example.reactive.coffee.store.service;

import com.firefly.example.reactive.coffee.store.vo.ProductBuyRequest;
import reactor.core.publisher.Mono;

/**
 * @author Pengtao Qiu
 */
public interface OrderService {

    Mono<Boolean> buy(ProductBuyRequest request);

}
