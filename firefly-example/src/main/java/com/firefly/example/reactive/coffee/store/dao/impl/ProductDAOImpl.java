package com.firefly.example.reactive.coffee.store.dao.impl;

import com.firefly.annotation.Component;
import com.firefly.annotation.Inject;
import com.firefly.example.reactive.coffee.store.dao.ProductDAO;
import com.firefly.example.reactive.coffee.store.model.Product;
import com.firefly.example.reactive.coffee.store.vo.Page;
import com.firefly.example.reactive.coffee.store.vo.ProductQuery;
import com.firefly.reactive.adapter.db.ReactiveTransactionalManager;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * @author Pengtao Qiu
 */
@Component
public class ProductDAOImpl implements ProductDAO {

    @Inject
    private ReactiveTransactionalManager db;

    @Override
    public Mono<Page<Product>> list(ProductQuery query) {
        List<Object> params = new ArrayList<>();
        StringBuilder sql = new StringBuilder("select * from coffee_store.product where 1=1 ");

        Optional.ofNullable(query.getProductStatus())
                .filter(status -> status > 0)
                .ifPresent(status -> {
                    sql.append(" and `status` = ?");
                    params.add(status);
                });

        Optional.ofNullable(query.getProductType())
                .filter(type -> type > 0)
                .ifPresent(type -> {
                    sql.append(" and `type` = ?");
                    params.add(type);
                });

        sql.append(" order by id desc ").append(Page.getPageSQLWithoutCount(query.getPageNumber(), query.getPageSize()));
        return db.execSQL(c -> c.queryForList(sql.toString(), Product.class, params.toArray())
                                .map(r -> new Page<>(r, query.getPageNumber(), query.getPageSize())));
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
