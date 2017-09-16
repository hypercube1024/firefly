package com.firefly.example.reactive.coffee.store.dao.impl;

import com.firefly.annotation.Component;
import com.firefly.annotation.Inject;
import com.firefly.example.reactive.coffee.store.dao.ProductDAO;
import com.firefly.example.reactive.coffee.store.model.Product;
import com.firefly.example.reactive.coffee.store.vo.Page;
import com.firefly.example.reactive.coffee.store.vo.ProductQuery;
import com.firefly.example.reactive.coffee.store.vo.ProductStatus;
import com.firefly.reactive.adapter.db.ReactiveTransactionalManager;
import reactor.core.publisher.Mono;

import java.util.Optional;

/**
 * @author Pengtao Qiu
 */
@Component
public class ProductDAOImpl implements ProductDAO {

    @Inject
    private ReactiveTransactionalManager db;

    @Override
    public Mono<Page<Product>> listByStatus(ProductQuery query) {
        StringBuilder sql = new StringBuilder("select * from coffee_store.product where 1=1 ");
        Optional.ofNullable(query.getProductStatus())
                .filter(status -> status > 0)
                .ifPresent(status -> sql.append(" `status` = ?"));
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
