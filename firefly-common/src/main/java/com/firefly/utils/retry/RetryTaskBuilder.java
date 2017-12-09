package com.firefly.utils.retry;

import com.firefly.utils.function.Action1;

import java.util.concurrent.Callable;

/**
 * @author Pengtao Qiu
 */
public class RetryTaskBuilder<V> {

    private RetryTask<V> retryTask;

    public static <V> RetryTaskBuilder<V> newTask() {
        return new RetryTaskBuilder<>();
    }

    public RetryTaskBuilder<V> complete(Action1<TaskContext<V>> complete) {
        retryTask.setComplete(complete);
        return this;
    }

    public RetryTaskBuilder<V> exception(Action1<TaskContext<V>> exception) {
        retryTask.setException(exception);
        return this;
    }

    public RetryTaskBuilder<V> retryStrategy(Action1<TaskContext<V>> retryStrategy) {
        retryTask.setRetryStrategy(retryStrategy);
        return this;
    }

    public RetryTaskBuilder<V> waitStrategy(Action1<TaskContext<V>> waitStrategy) {
        retryTask.setWaitStrategy(waitStrategy);
        return this;
    }

    public RetryTaskBuilder<V> execute(Callable<V> taskCallable) {
        retryTask.setTaskCallable(taskCallable);
        return this;
    }

}
