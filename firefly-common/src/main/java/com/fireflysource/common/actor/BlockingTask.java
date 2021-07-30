package com.fireflysource.common.actor;

import com.fireflysource.common.func.Callback;
import com.fireflysource.common.slf4j.LazyLogger;
import com.fireflysource.common.sys.Result;
import com.fireflysource.common.sys.SystemLogger;

import java.util.concurrent.Callable;
import java.util.concurrent.ForkJoinPool;

import static com.fireflysource.common.sys.Result.runCaching;

public class BlockingTask<T> implements ForkJoinPool.ManagedBlocker {

    private static final LazyLogger log = SystemLogger.create(BlockingTask.class);

    private final Callable<T> callable;
    private final Callable<Result<T>> tryCallable;
    private T result;
    private boolean done = false;

    public BlockingTask(Callable<T> callable, Callable<Result<T>> tryCallable) {
        this.callable = callable;
        this.tryCallable = tryCallable;
    }

    @Override
    public boolean block() throws InterruptedException {
        try {
            result = callable.call();
        } catch (Exception e) {
            log.error("run blocking task exception.", e);
        }
        done = true;
        return true;
    }

    @Override
    public boolean isReleasable() {
        try {
            Result<T> r = tryCallable.call();
            done = r.isSuccess();
            if (done) {
                result = r.getValue();
            }
        } catch (Exception e) {
            done = false;
        }
        return done;
    }

    public static void runBlockingTask(Callback callback) {
        runBlockingTask(() -> {
            callback.call();
            return null;
        });
    }

    public static <T> Result<T> runBlockingTask(Callable<T> callable) {
        return runBlockingTask(callable, null);
    }

    public static <T> Result<T> runBlockingTask(Callable<T> callable, Callable<Result<T>> tryCallable) {
        return runCaching(() -> {
            BlockingTask<T> task = new BlockingTask<>(callable, tryCallable);
            ForkJoinPool.managedBlock(task);
            return task.result;
        });
    }

    public static void sleep(long millisecond) {
        runBlockingTask(() -> Thread.sleep(millisecond));
    }
}
