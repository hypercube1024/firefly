package com.fireflysource.common.actor;

import com.fireflysource.common.coroutine.CoroutineDispatchers;

import java.util.concurrent.Executor;

public class DispatcherFactory {

    public static Dispatcher createDispatcher() {
        return createDispatcher(CoroutineDispatchers.INSTANCE.getComputationThreadPool());
    }

    public static Dispatcher createDispatcher(Executor executor) {
        return new AbstractActor.DispatcherImpl(executor);
    }
}
