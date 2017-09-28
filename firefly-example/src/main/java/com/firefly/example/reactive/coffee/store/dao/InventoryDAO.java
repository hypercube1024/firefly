package com.firefly.example.reactive.coffee.store.dao;

import com.firefly.example.reactive.coffee.store.model.Inventory;
import com.firefly.example.reactive.coffee.store.vo.InventoryOperator;
import com.firefly.example.reactive.coffee.store.vo.InventoryUpdate;
import com.firefly.reactive.adapter.db.ReactiveSQLConnection;
import reactor.core.publisher.Mono;

import java.util.List;

/**
 * @author Pengtao Qiu
 */
public interface InventoryDAO {

    Mono<int[]> updateBatch(List<InventoryUpdate> list, InventoryOperator operator, ReactiveSQLConnection connection);

    Mono<List<Inventory>> listByProductId(List<Long> productIdList);

}
