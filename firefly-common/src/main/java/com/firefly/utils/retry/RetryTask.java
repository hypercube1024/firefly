package com.firefly.utils.retry;

import com.firefly.utils.Assert;
import com.firefly.utils.function.Action1;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.function.Predicate;

/**
 * @author Pengtao Qiu
 */
public class RetryTask<V> implements Callable<V> {

    private Action1<TaskContext<V>> complete;
    private Action1<TaskContext<V>> exception;
    private Action1<TaskContext<V>> waitStrategy;
    private List<Predicate<TaskContext<V>>> retryStrategies;
    private List<Predicate<TaskContext<V>>> stopStrategies;
    private Callable<V> taskCallable;
    private Action1<TaskContext<V>> finish;

    public V call() {
        Assert.notEmpty(retryStrategies, "The retry strategies must be not empty");
        Assert.notEmpty(stopStrategies, "The stop strategies must be not empty");
        Assert.notNull(waitStrategy, "The wait strategy must be not null");
        Assert.notNull(taskCallable, "The task must be not null");

        TaskContext<V> ctx = new TaskContext<>();
        ctx.setStartTime(System.currentTimeMillis());
        while (true) {
            try {
                ctx.setResult(taskCallable.call());
            } catch (Exception e) {
                ctx.setException(e);
                Optional.ofNullable(exception).ifPresent(c -> c.call(ctx));
            }
            ctx.setExecutedCount(ctx.getExecutedCount() + 1);
            Optional.ofNullable(complete).ifPresent(c -> c.call(ctx));

            boolean stop = stopStrategies.stream().anyMatch(p -> p.test(ctx));
            boolean retry = retryStrategies.stream().anyMatch(p -> p.test(ctx));
            if (stop || !retry) {
                Optional.ofNullable(finish).ifPresent(c -> c.call(ctx));
                return ctx.getResult();
            } else {
                waitStrategy.call(ctx);
            }
        }
    }

    public Action1<TaskContext<V>> getComplete() {
        return complete;
    }

    public void setComplete(Action1<TaskContext<V>> complete) {
        this.complete = complete;
    }

    public Action1<TaskContext<V>> getException() {
        return exception;
    }

    public void setException(Action1<TaskContext<V>> exception) {
        this.exception = exception;
    }

    public Action1<TaskContext<V>> getWaitStrategy() {
        return waitStrategy;
    }

    public void setWaitStrategy(Action1<TaskContext<V>> waitStrategy) {
        this.waitStrategy = waitStrategy;
    }

    public List<Predicate<TaskContext<V>>> getRetryStrategies() {
        return retryStrategies;
    }

    public void setRetryStrategies(List<Predicate<TaskContext<V>>> retryStrategies) {
        this.retryStrategies = retryStrategies;
    }

    public List<Predicate<TaskContext<V>>> getStopStrategies() {
        return stopStrategies;
    }

    public void setStopStrategies(List<Predicate<TaskContext<V>>> stopStrategies) {
        this.stopStrategies = stopStrategies;
    }

    public Callable<V> getTaskCallable() {
        return taskCallable;
    }

    public void setTaskCallable(Callable<V> taskCallable) {
        this.taskCallable = taskCallable;
    }

    public Action1<TaskContext<V>> getFinish() {
        return finish;
    }

    public void setFinish(Action1<TaskContext<V>> finish) {
        this.finish = finish;
    }

}
