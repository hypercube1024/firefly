package com.firefly.net.tcp.codec.ffsocks.model;

/**
 * @author Pengtao Qiu
 */
public class ClientRequest<T> extends Request {

    protected T data;

    public T getData() {
        return data;
    }

    public void setData(T data) {
        this.data = data;
    }

    @Override
    public String toString() {
        return "ClientRequest{" +
                "data=" + data +
                ", path='" + path + '\'' +
                ", fields=" + fields +
                '}';
    }
}
