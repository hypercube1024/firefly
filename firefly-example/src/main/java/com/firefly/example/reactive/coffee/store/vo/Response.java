package com.firefly.example.reactive.coffee.store.vo;

/**
 * @author Pengtao Qiu
 */
public class Response<T> {
    private T data;
    private String message;
    private int status;

    public T getData() {
        return data;
    }

    public void setData(T data) {
        this.data = data;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public int getStatus() {
        return status;
    }

    public void setStatus(int status) {
        this.status = status;
    }
}
