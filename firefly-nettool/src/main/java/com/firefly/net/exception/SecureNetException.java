package com.firefly.net.exception;

/**
 * @author Pengtao Qiu
 */
public class SecureNetException extends RuntimeException {
    public SecureNetException(String msg) {
        super(msg);
    }

    public SecureNetException(String msg, Throwable t) {
        super(msg, t);
    }
}
