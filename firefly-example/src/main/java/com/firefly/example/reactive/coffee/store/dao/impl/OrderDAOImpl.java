package com.firefly.example.reactive.coffee.store.dao.impl;

import com.firefly.annotation.Component;
import com.firefly.example.reactive.coffee.store.dao.OrderDAO;
import com.firefly.example.reactive.coffee.store.model.Order;
import com.firefly.reactive.adapter.db.ReactiveSQLConnection;
import reactor.core.publisher.Mono;

import java.util.List;

/**
 * @author Pengtao Qiu
 */
@Component
public class OrderDAOImpl implements OrderDAO {

    @Override
    public Mono<List<Long>> insertBatch(List<Order> list, ReactiveSQLConnection connection) {
        return connection.inTransaction(c -> c.insertObjectBatch(list, Order.class));
    }
}
