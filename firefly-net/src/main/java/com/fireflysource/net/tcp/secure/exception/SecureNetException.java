package com.fireflysource.net.tcp.secure.exception;

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
