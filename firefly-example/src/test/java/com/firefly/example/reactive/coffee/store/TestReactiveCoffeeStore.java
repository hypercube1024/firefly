package com.firefly.example.reactive.coffee.store;

import com.firefly.$;
import com.firefly.example.reactive.coffee.store.model.Product;
import com.firefly.example.reactive.coffee.store.utils.DBUtils;
import com.firefly.reactive.adapter.db.ReactiveSQLClient;
import org.junit.Before;
import org.junit.Test;

/**
 * @author Pengtao Qiu
 */
public class TestReactiveCoffeeStore {

    @Before
    public void before() {
        DBUtils dbUtils = $.getBean(DBUtils.class);
        dbUtils.createTables();
        dbUtils.initializeData();
    }

    @Test
    public void test() {
        ReactiveSQLClient sqlClient = $.getBean(ReactiveSQLClient.class);
        sqlClient.newTransaction(c -> c.queryById(1L, Product.class))
                 .doOnSuccess(System.out::println)
                 .block();
    }
}
