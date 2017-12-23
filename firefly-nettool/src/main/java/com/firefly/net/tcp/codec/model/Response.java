package com.firefly.net.tcp.codec.model;

/**
 * @author Pengtao Qiu
 */
public class Response extends MetaInfo {

    protected int code;

    public int getCode() {
        return code;
    }

    public void setCode(int code) {
        this.code = code;
    }

}
