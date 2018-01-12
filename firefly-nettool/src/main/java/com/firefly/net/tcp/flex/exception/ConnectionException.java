package com.firefly.net.tcp.flex.exception;

/**
 * @author Pengtao Qiu
 */
public class ConnectionException extends RuntimeException {

    public ConnectionException(String msg) {
        super(msg);
    }

    public ConnectionException(String msg, Throwable e) {
        super(msg, e);
    }
}
