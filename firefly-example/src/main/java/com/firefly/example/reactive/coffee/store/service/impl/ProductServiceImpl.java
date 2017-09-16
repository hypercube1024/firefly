package com.firefly.example.reactive.coffee.store.service.impl;

import com.firefly.annotation.Component;
import com.firefly.annotation.Inject;
import com.firefly.example.reactive.coffee.store.dao.ProductDAO;
import com.firefly.example.reactive.coffee.store.model.Product;
import com.firefly.example.reactive.coffee.store.service.ProductService;
import com.firefly.example.reactive.coffee.store.vo.Page;
import com.firefly.example.reactive.coffee.store.vo.ProductQuery;
import reactor.core.publisher.Mono;

/**
 * @author Pengtao Qiu
 */
@Component
public class ProductServiceImpl implements ProductService {

    @Inject
    private ProductDAO productDAO;

    @Override
    public Mono<Page<Product>> list(ProductQuery query) {
        return productDAO.list(query);
    }
}
