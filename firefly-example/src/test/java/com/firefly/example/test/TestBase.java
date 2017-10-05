package com.firefly.example.test;

import com.firefly.example.reactive.coffee.store.utils.DBUtils;
import org.junit.Before;

import static com.firefly.example.reactive.coffee.store.AppMain.javaCtx;

/**
 * @author Pengtao Qiu
 */
public class TestBase {

    @Before
    public void before() {
        DBUtils dbUtils = javaCtx.getBean(DBUtils.class);
        dbUtils.createTables();
        dbUtils.initializeData();
        System.out.println("init test data");
    }
}
