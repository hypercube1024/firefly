package com.fireflysource.net.tcp;

/**
 * @author Pengtao Qiu
 */
public class Result<T> {

    public static final Result<Void> SUCCESS = new Result<>(true, null, null);

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
}
