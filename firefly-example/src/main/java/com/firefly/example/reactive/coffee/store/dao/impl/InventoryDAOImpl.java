package com.firefly.example.reactive.coffee.store.dao.impl;

import com.firefly.annotation.Component;
import com.firefly.annotation.Inject;
import com.firefly.example.reactive.coffee.store.dao.InventoryDAO;
import com.firefly.example.reactive.coffee.store.model.Inventory;
import com.firefly.example.reactive.coffee.store.vo.InventoryOperator;
import com.firefly.example.reactive.coffee.store.vo.InventoryUpdate;
import com.firefly.reactive.adapter.db.ReactiveSQLClient;
import com.firefly.reactive.adapter.db.ReactiveSQLConnection;
import com.firefly.utils.CollectionUtils;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import static com.firefly.example.reactive.coffee.store.utils.DBUtils.toWildcard;

/**
 * @author Pengtao Qiu
 */
@Component
public class InventoryDAOImpl implements InventoryDAO {

    @Inject
    private ReactiveSQLClient db;

    @Override
    public Mono<int[]> updateBatch(List<InventoryUpdate> list, InventoryOperator operator, ReactiveSQLConnection connection) {
        if (CollectionUtils.isEmpty(list)) {
            return Mono.error(new IllegalArgumentException("The inventory update request must be not empty"));
        }

        if (operator == null) {
            return Mono.error(new IllegalArgumentException("The inventory update operator must be not null"));
        }

        if (list.parallelStream().anyMatch(i -> i.getAmount() == null || i.getProductId() == null)) {
            return Mono.error(new IllegalArgumentException("The inventory update field amount or productId must be not null"));
        }

        return connection.inTransaction(c -> {
            String sql = "update `coffee_store`.`inventory` set `amount` = `amount` " + operator.getValue() + " ?  where `product_id` = ? ";
            if (operator == InventoryOperator.SUB) {
                sql += " and `amount` >= ? ";
            }
            Object[][] params = list.parallelStream().map(u -> {
                List<Object> p = new ArrayList<>();
                p.add(u.getAmount());
                p.add(u.getProductId());
                if (operator == InventoryOperator.SUB) {
                    p.add(u.getAmount());
                }
                return p.toArray();
            }).toArray(Object[][]::new);
            return c.executeBatch(sql, params);
        });
    }

    @Override
    public Mono<List<Inventory>> listByProductId(List<Long> productIdList) {
        if (CollectionUtils.isEmpty(productIdList)) {
            return Mono.just(Collections.emptyList());
        }

        String sql = "select * from `coffee_store`.`inventory` where `product_id` in ( " + toWildcard(productIdList) + " )";
        return db.newTransaction(c -> c.queryForList(sql, Inventory.class, productIdList.toArray()));
    }

}
