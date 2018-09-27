package com.firefly.example.reactive.coffee.store.service;

import com.firefly.example.reactive.coffee.store.model.Product;
import com.firefly.example.reactive.coffee.store.vo.Page;
import com.firefly.example.reactive.coffee.store.vo.ProductQuery;
import reactor.core.publisher.Mono;

/**
 * @author Pengtao Qiu
 */
public interface ProductService {

    Mono<Page<Product>> list(ProductQuery query);

}
