package com.firefly.net.tcp.codec.ffsocks.model;

/**
 * @author Pengtao Qiu
 */
public class ClientResponse {

    protected Response response;
    protected byte[] data;

    public Response getResponse() {
        return response;
    }

    public void setResponse(Response response) {
        this.response = response;
    }

    public byte[] getData() {
        return data;
    }

    public void setData(byte[] data) {
        this.data = data;
    }
}
