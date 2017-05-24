package com.firefly.core;

import com.firefly.utils.concurrent.Promise;
import com.firefly.utils.function.Func0;
import com.firefly.utils.lang.AbstractLifeCycle;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedTransferQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * @author Pengtao Qiu
 */
public class ApplicationContextSingleton extends AbstractLifeCycle {

    private static ApplicationContextSingleton ourInstance = new ApplicationContextSingleton();

    public static ApplicationContextSingleton getInstance() {
        return ourInstance;
    }

    private ExecutorService executorService;

    private ApplicationContextSingleton() {

    }

    private ApplicationContext applicationContext;

    public ApplicationContext getApplicationContext() {
        start();
        return applicationContext;
    }

    public <T> Promise.Completable<T> async(Func0<T> func) {
        Promise.Completable<T> c = new Promise.Completable<>();
        executorService.submit(() -> {
            try {
                c.succeeded(func.call());
            } catch (Throwable t) {
                c.failed(t);
            }
        });
        return c;
    }

    public void async(Runnable runnable) {
        executorService.submit(runnable);
    }

    @Override
    protected void init() {
        applicationContext = new XmlApplicationContext();
        executorService = new ThreadPoolExecutor(16, 256,
                30L, TimeUnit.SECONDS,
                new LinkedTransferQueue<>(),
                r -> new Thread(r, "firefly run blocking task pool"));
    }

    @Override
    protected void destroy() {
        applicationContext = null;
        if (executorService != null) {
            executorService.shutdown();
        }
    }
}
