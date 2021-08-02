package com.fireflysource.common.actor;

import java.util.concurrent.Executor;
import java.util.concurrent.ForkJoinPool;

public class DispatcherFactory {

    public static Dispatcher createDispatcher() {
        return createDispatcher(ForkJoinPool.commonPool());
    }

    public static Dispatcher createDispatcher(Executor executor) {
        return new AbstractActor.DispatcherImpl(executor);
    }
}
