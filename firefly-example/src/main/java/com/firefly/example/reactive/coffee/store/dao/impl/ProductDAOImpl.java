package com.firefly.example.reactive.coffee.store.dao.impl;

import com.firefly.annotation.Component;
import com.firefly.annotation.Inject;
import com.firefly.example.reactive.coffee.store.dao.ProductDAO;
import com.firefly.example.reactive.coffee.store.model.Product;
import com.firefly.example.reactive.coffee.store.vo.Page;
import com.firefly.example.reactive.coffee.store.vo.ProductStatus;
import com.firefly.reactive.adapter.db.ReactiveTransactionalManager;
import reactor.core.publisher.Mono;

/**
 * @author Pengtao Qiu
 */
@Component
public class ProductDAOImpl implements ProductDAO {

    @Inject
    private ReactiveTransactionalManager db;

    @Override
    public Mono<Page<Product>> listByStatus(ProductStatus status, int pageNumber, int pageSize) {
        return null;
    }

    @Override
    public Mono<Product> get(Long id) {
        return db.execSQL(c -> c.queryById(id, Product.class));
    }

    @Override
    public Mono<Long> insert(Product product) {
        return db.execSQL(c -> c.insertObject(product));
    }

    @Override
    public Mono<Integer> update(Product product) {
        return db.execSQL(c -> c.updateObject(product));
    }
}
