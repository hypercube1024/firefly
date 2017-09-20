package com.firefly.example.test.dao;

import com.firefly.$;
import com.firefly.example.reactive.coffee.store.utils.DBUtils;
import org.junit.Before;

/**
 * @author Pengtao Qiu
 */
public class TestBase {

    @Before
    public void before() {
        DBUtils dbUtils = $.getBean(DBUtils.class);
        dbUtils.createTables();
        dbUtils.initializeData();
    }
}
