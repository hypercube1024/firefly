package com.firefly.example.test.dao;

import com.firefly.$;
import com.firefly.db.RecordNotFound;
import com.firefly.example.reactive.coffee.store.dao.ProductDAO;
import com.firefly.example.reactive.coffee.store.utils.DBUtils;
import com.firefly.example.reactive.coffee.store.vo.ProductQuery;
import com.firefly.example.reactive.coffee.store.vo.ProductStatus;
import com.firefly.example.reactive.coffee.store.vo.ProductType;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import reactor.test.StepVerifier;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.lessThanOrEqualTo;


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
    public void testGet() {
        StepVerifier.create(productDAO.get(1L)).assertNext(product -> {
            System.out.println(product);
            Assert.assertThat(product.getId(), is(1L));
            Assert.assertThat(product.getName(), is("Cappuccino"));
        }).expectComplete().verify();

        StepVerifier.create(productDAO.get(200L))
                    .expectError(RecordNotFound.class)
                    .verify();
    }

    @Test
    public void testList() {
        ProductQuery query = new ProductQuery(ProductStatus.ENABLE.getValue(), null, 1, 5);
        StepVerifier.create(productDAO.list(query)).assertNext(productPage -> {
            System.out.println(productPage);
            Assert.assertTrue(productPage.isNext());
            Assert.assertThat(productPage.getRecord().size(), is(5));
        }).expectComplete().verify();

        query = new ProductQuery(ProductStatus.ENABLE.getValue(), null, 2, 5);
        StepVerifier.create(productDAO.list(query)).assertNext(productPage -> {
            System.out.println(productPage);
            Assert.assertFalse(productPage.isNext());
            Assert.assertThat(productPage.getRecord().size(), lessThanOrEqualTo(5));
        }).expectComplete().verify();

        query = new ProductQuery(ProductStatus.ENABLE.getValue(), ProductType.DESSERT.getValue(), 1, 5);
        StepVerifier.create(productDAO.list(query)).assertNext(productPage -> {
            System.out.println(productPage);
            Assert.assertFalse(productPage.isNext());
            Assert.assertThat(productPage.getRecord().size(), lessThanOrEqualTo(1));
        }).expectComplete().verify();
    }
}
