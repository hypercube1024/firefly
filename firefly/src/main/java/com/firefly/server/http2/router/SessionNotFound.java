package com.firefly.server.http2.router;

/**
 * @author Pengtao Qiu
 */
public class SessionNotFound extends RuntimeException {
    public SessionNotFound() {

    }

    public SessionNotFound(String msg) {
        super(msg);
    }
}
