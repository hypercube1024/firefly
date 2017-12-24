package com.firefly.net.tcp.codec.ffsocks.model;

/**
 * @author Pengtao Qiu
 */
public class ClientRequest {

    protected Request request;
    protected byte[] data;

    public Request getRequest() {
        return request;
    }

    public void setRequest(Request request) {
        this.request = request;
    }

    public byte[] getData() {
        return data;
    }

    public void setData(byte[] data) {
        this.data = data;
    }
}
