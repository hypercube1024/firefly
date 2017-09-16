package com.firefly.example.test.dao;

import com.firefly.$;
import com.firefly.db.RecordNotFound;
import com.firefly.example.reactive.coffee.store.dao.ProductDAO;
import com.firefly.example.reactive.coffee.store.utils.DBUtils;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import reactor.test.StepVerifier;

import static org.hamcrest.Matchers.is;


/**
 * @author Pengtao Qiu
 */
public class TestProductDAO {

    private ProductDAO productDAO;

    @Before
    public void before() {
        DBUtils dbUtils = $.getBean(DBUtils.class);
        dbUtils.createTables();
        dbUtils.initializeData();

        productDAO = $.getBean(ProductDAO.class);
    }

    @Test
    public void test() {
        StepVerifier.create(productDAO.get(1L)).assertNext(product -> {
            System.out.println(product);
            Assert.assertThat(product.getId(), is(1L));
            Assert.assertThat(product.getName(), is("Cappuccino"));
        }).expectComplete().verify();

        StepVerifier.create(productDAO.get(200L))
                    .expectError(RecordNotFound.class)
                    .verify();
    }
}
