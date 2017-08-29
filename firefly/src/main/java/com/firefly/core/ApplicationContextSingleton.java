package com.firefly.core;

import com.firefly.utils.lang.AbstractLifeCycle;

/**
 * @author Pengtao Qiu
 */
public class ApplicationContextSingleton extends AbstractLifeCycle {

    private static ApplicationContextSingleton ourInstance = new ApplicationContextSingleton();

    public static ApplicationContextSingleton getInstance() {
        return ourInstance;
    }

    private ApplicationContextSingleton() {

    }

    private ApplicationContext applicationContext;

    public ApplicationContext getApplicationContext() {
        start();
        return applicationContext;
    }

    @Override
    protected void init() {
        applicationContext = new XmlApplicationContext();
    }

    @Override
    protected void destroy() {
        applicationContext = null;
    }
}
