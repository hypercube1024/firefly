package com.firefly.example.reactive.coffee.store;

import com.firefly.$;
import com.firefly.example.reactive.coffee.store.dao.ProductDAO;
import com.firefly.example.reactive.coffee.store.model.Product;
import com.firefly.example.reactive.coffee.store.utils.DBUtils;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import reactor.test.StepVerifier;

import static org.hamcrest.Matchers.is;

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
        ProductDAO productDAO = $.getBean(ProductDAO.class);
        StepVerifier.create(productDAO.get(1L).doOnSuccess(System.out::println).map(Product::getId))
                    .expectNext(1L)
                    .expectComplete()
                    .verify();
    }
}
