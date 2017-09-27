package com.firefly.example.reactive.coffee.store.dao.impl;

import com.firefly.annotation.Component;
import com.firefly.annotation.Inject;
import com.firefly.example.reactive.coffee.store.dao.OrderDAO;
import com.firefly.example.reactive.coffee.store.model.Order;
import com.firefly.reactive.adapter.db.ReactiveTransactionalManager;
import reactor.core.publisher.Mono;

import java.util.List;

/**
 * @author Pengtao Qiu
 */
@Component
public class OrderDAOImpl implements OrderDAO {

    @Inject
    private ReactiveTransactionalManager db;

    @Override
    public Mono<List<Long>> insertBatch(List<Order> list) {
        return db.execSQL(c -> c.insertObjectBatch(list, Order.class));
    }
}
