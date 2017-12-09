package com.firefly.utils.retry;

import com.firefly.utils.Assert;
import com.firefly.utils.function.Action1;

import java.util.Optional;
import java.util.concurrent.Callable;

/**
 * @author Pengtao Qiu
 */
public class RetryTask<V> implements Callable<V> {

    private Action1<TaskContext<V>> complete;
    private Action1<TaskContext<V>> exception;
    private Action1<TaskContext<V>> waitStrategy;
    private Action1<TaskContext<V>> retryStrategy;
    private Callable<V> taskCallable;

    public V call() {
        Assert.notNull(retryStrategy, "The retry strategy must be not null");
        Assert.notNull(waitStrategy, "The wait strategy must be not null");
        Assert.notNull(taskCallable, "The task must be not null");

        TaskContext<V> ctx = new TaskContext<>();
        ctx.setStartTime(System.currentTimeMillis());
        while (true) {
            try {
                V ret = taskCallable.call();
                ctx.setExecutedCount(ctx.getExecutedCount() + 1);
                ctx.setResult(ret);
                Optional.ofNullable(complete).ifPresent(c -> c.call(ctx));
            } catch (Exception e) {
                ctx.setException(e);
                ctx.setExecutedCount(ctx.getExecutedCount() + 1);
                Optional.ofNullable(exception).ifPresent(c -> c.call(ctx));
            }
            try {
                retryStrategy.call(ctx);
                if (ctx.getStatus() == TaskStatus.RETRY) {
                    waitStrategy.call(ctx);
                } else {
                    return ctx.getResult();
                }
            } catch (Exception e) {
                Optional.ofNullable(exception).ifPresent(c -> c.call(ctx));
                return ctx.getResult();
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

    public Action1<TaskContext<V>> getRetryStrategy() {
        return retryStrategy;
    }

    public void setRetryStrategy(Action1<TaskContext<V>> retryStrategy) {
        this.retryStrategy = retryStrategy;
    }

    public Callable<V> getTaskCallable() {
        return taskCallable;
    }

    public void setTaskCallable(Callable<V> taskCallable) {
        this.taskCallable = taskCallable;
    }
}
