package com.fireflysource.common.sys;

import com.fireflysource.common.string.StringUtils;

import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

import static com.fireflysource.common.func.FunctionInterfaceUtils.createEmptyConsumer;

/**
 * @author Pengtao Qiu
 */
public class Result<T> {

    public static final CompletableFuture<Void> DONE = doneFuture();
    public static final Result<Void> SUCCESS = new Result<>(true, null, null);
    @SuppressWarnings("rawtypes")
    private static final Consumer EMPTY = createEmptyConsumer();

    private final boolean success;
    private final T value;
    private final Throwable throwable;

    public Result(boolean success, T value, Throwable throwable) {
        this.success = success;
        this.value = value;
        this.throwable = throwable;
    }

    public static void done(CompletableFuture<Void> future) {
        future.complete(null);
    }

    public static CompletableFuture<Void> doneFuture() {
        return CompletableFuture.completedFuture(null);
    }

    @SuppressWarnings("unchecked")
    public static <T> Consumer<T> emptyConsumer() {
        return EMPTY;
    }

    public static <T> Result<T> createFailedResult(T value, Throwable throwable) {
        return new Result<>(false, value, throwable);
    }

    public static Result<Void> createFailedResult(Throwable throwable) {
        return new Result<>(false, null, throwable);
    }

    public static Result<Void> createSuccessResult() {
        return new Result<>(true, null, null);
    }

    @SuppressWarnings("unchecked")
    public static <T> Consumer<Result<T>> discard() {
        return EMPTY;
    }

    public static <T> Consumer<Result<T>> futureToConsumer(CompletableFuture<T> future) {
        return result -> {
            if (result.isSuccess()) {
                future.complete(result.getValue());
            } else {
                future.completeExceptionally(result.getThrowable());
            }
        };
    }

    public static <T> Result<T> runCaching(Callable<T> callable) {
        try {
            T t = callable.call();
            return new Result<>(true, t, null);
        } catch (Throwable t) {
            return new Result<>(false, null, t);
        }
    }

    public boolean isSuccess() {
        return success;
    }

    public T getValue() {
        return value;
    }

    public Throwable getThrowable() {
        return throwable;
    }

    @Override
    public String toString() {
        return "Result{" +
                "success=" + success +
                ", value=" + value +
                ", throwable=" + (throwable != null && StringUtils.hasText(throwable.getMessage()) ? throwable.getMessage() : "null") +
                '}';
    }
}
