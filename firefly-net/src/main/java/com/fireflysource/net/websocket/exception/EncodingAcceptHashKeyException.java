package com.fireflysource.net.websocket.exception;

/**
 * @author Pengtao Qiu
 */
public class EncodingAcceptHashKeyException extends RuntimeException {

    public EncodingAcceptHashKeyException(String message) {
        super(message);
    }

    public EncodingAcceptHashKeyException(String message, Throwable e) {
        super(message, e);
    }
}
