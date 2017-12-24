package com.firefly.net.tcp.codec.ffsocks.model;

/**
 * @author Pengtao Qiu
 */
public class Response extends MetaInfo {

    protected int code;
    protected String message;

    public int getCode() {
        return code;
    }

    public void setCode(int code) {
        this.code = code;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    @Override
    public String toString() {
        return "Response{" +
                "code=" + code +
                ", message='" + message + '\'' +
                ", fields=" + fields +
                '}';
    }
}
