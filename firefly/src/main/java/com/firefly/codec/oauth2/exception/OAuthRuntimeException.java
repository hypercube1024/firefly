package com.firefly.codec.oauth2.exception;

public class OAuthRuntimeException extends RuntimeException {

    public OAuthRuntimeException() {
    }

    public OAuthRuntimeException(String message) {
        super(message);
    }

    public OAuthRuntimeException(String message, Throwable cause) {
        super(message, cause);
    }

    public OAuthRuntimeException(Throwable cause) {
        super(cause);
    }
}
