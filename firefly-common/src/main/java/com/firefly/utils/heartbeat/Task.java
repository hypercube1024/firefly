package com.firefly.utils.heartbeat;

import com.firefly.utils.function.Action3;
import com.firefly.utils.function.Func0;

import java.util.concurrent.CompletableFuture;

/**
 * @author Pengtao Qiu
 */
public class Task {
    protected String name;
    protected Func0<CompletableFuture<Result>> task;
    protected Action3<String, Result, Throwable> resultListener;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Func0<CompletableFuture<Result>> getTask() {
        return task;
    }

    public void setTask(Func0<CompletableFuture<Result>> task) {
        this.task = task;
    }

    public Action3<String, Result, Throwable> getResultListener() {
        return resultListener;
    }

    public void setResultListener(Action3<String, Result, Throwable> resultListener) {
        this.resultListener = resultListener;
    }
}
