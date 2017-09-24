package com.firefly.example.reactive.coffee.store.dao;

import com.firefly.example.reactive.coffee.store.model.Inventory;
import com.firefly.example.reactive.coffee.store.vo.InventoryOperator;
import com.firefly.example.reactive.coffee.store.vo.InventoryUpdate;
import reactor.core.publisher.Mono;

import java.util.List;

/**
 * @author Pengtao Qiu
 */
public interface InventoryDao {

    Mono<int[]> updateBatch(List<InventoryUpdate> list, InventoryOperator operator);

    Mono<List<Inventory>> listByProductId(List<Long> productIdList);

}
