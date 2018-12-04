package com.fireflysource.net.tcp;

import com.fireflysource.common.string.StringUtils;

import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

/**
 * @author Pengtao Qiu
 */
public class Result<T> {

    public static final Result<Void> SUCCESS = new Result<>(true, null, null);
    public static final Consumer<Result<Void>> EMPTY_CONSUMER_RESULT = result -> {
    };

    private final boolean success;
    private final T value;
    private final Throwable throwable;

    public Result(boolean success, T value, Throwable throwable) {
        this.success = success;
        this.value = value;
        this.throwable = throwable;
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

    public static <T> Consumer<Result<T>> futureToConsumer(CompletableFuture<T> future) {
        return result -> {
            if (result.isSuccess()) {
                future.complete(result.getValue());
            } else {
                future.completeExceptionally(result.getThrowable());
            }
        };
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
