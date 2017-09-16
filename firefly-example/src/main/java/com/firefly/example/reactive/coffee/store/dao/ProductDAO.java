package com.firefly.example.reactive.coffee.store.dao;

import com.firefly.example.reactive.coffee.store.model.Product;
import com.firefly.example.reactive.coffee.store.vo.Page;
import com.firefly.example.reactive.coffee.store.vo.ProductQuery;
import reactor.core.publisher.Mono;

/**
 * @author Pengtao Qiu
 */
public interface ProductDAO {

    Mono<Page<Product>> listByStatus(ProductQuery query);

    Mono<Product> get(Long id);

    Mono<Long> insert(Product product);

    Mono<Integer> update(Product product);
}
