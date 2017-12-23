package com.firefly.net.tcp.codec.model;

/**
 * @author Pengtao Qiu
 */
public class ClientResponse<T> extends Response {

    protected T data;

    public T getData() {
        return data;
    }

    public void setData(T data) {
        this.data = data;
    }
}
