package com.firefly.utils.retry;

import com.firefly.utils.function.Action1;

import java.util.ArrayList;
import java.util.concurrent.Callable;
import java.util.function.Predicate;

/**
 * @author Pengtao Qiu
 */
public class RetryTaskBuilder<V> implements Callable<V> {

    private RetryTask<V> retryTask;

    private RetryTaskBuilder() {
        retryTask = new RetryTask<>();
        retryTask.setRetryStrategies(new ArrayList<>());
        retryTask.setStopStrategies(new ArrayList<>());
    }

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

    public RetryTaskBuilder<V> retry(Predicate<TaskContext<V>> retryStrategy) {
        retryTask.getRetryStrategies().add(retryStrategy);
        return this;
    }

    public RetryTaskBuilder<V> stop(Predicate<TaskContext<V>> stopStrategy) {
        retryTask.getStopStrategies().add(stopStrategy);
        return this;
    }

    public RetryTaskBuilder<V> wait(Action1<TaskContext<V>> waitStrategy) {
        retryTask.setWaitStrategy(waitStrategy);
        return this;
    }

    public RetryTaskBuilder<V> execute(Callable<V> taskCallable) {
        retryTask.setTaskCallable(taskCallable);
        return this;
    }

    public RetryTaskBuilder<V> finish(Action1<TaskContext<V>> finish) {
        retryTask.setFinish(finish);
        return this;
    }

    @Override
    public V call() {
        return retryTask.call();
    }

}
