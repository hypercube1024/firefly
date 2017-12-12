package com.firefly.utils.retry;

/**
 * @author Pengtao Qiu
 */
public class TaskContext<T> {
    private long startTime;
    private int executedCount;
    private Exception exception;
    private T result;
    private Object attachment;

    public long getStartTime() {
        return startTime;
    }

    void setStartTime(long startTime) {
        this.startTime = startTime;
    }

    public Exception getException() {
        return exception;
    }

    public void setException(Exception exception) {
        this.exception = exception;
    }

    public int getExecutedCount() {
        return executedCount;
    }

    void setExecutedCount(int executedCount) {
        this.executedCount = executedCount;
    }

    public T getResult() {
        return result;
    }

    public void setResult(T result) {
        this.result = result;
    }

    public Object getAttachment() {
        return attachment;
    }

    public void setAttachment(Object attachment) {
        this.attachment = attachment;
    }
}
