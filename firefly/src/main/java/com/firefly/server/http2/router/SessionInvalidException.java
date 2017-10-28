package com.firefly.server.http2.router;

/**
 * @author Pengtao Qiu
 */
public class SessionInvalidException extends RuntimeException {
    public SessionInvalidException() {

    }

    public SessionInvalidException(String msg) {
        super(msg);
    }
}
