package com.firefly.example.reactive.coffee.store.dao;

import com.firefly.example.reactive.coffee.store.model.Order;
import com.firefly.reactive.adapter.db.ReactiveSQLConnection;
import reactor.core.publisher.Mono;

import java.util.List;

/**
 * @author Pengtao Qiu
 */
public interface OrderDAO {

    Mono<List<Long>> insertBatch(List<Order> list, ReactiveSQLConnection connection);

}
