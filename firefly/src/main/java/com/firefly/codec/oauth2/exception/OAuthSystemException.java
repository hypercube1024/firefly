package com.firefly.codec.oauth2.exception;

public class OAuthSystemException extends RuntimeException {

    public OAuthSystemException() {
    }

    public OAuthSystemException(String s) {
        super(s);
    }

    public OAuthSystemException(Throwable throwable) {
        super(throwable);
    }

    public OAuthSystemException(String s, Throwable throwable) {
        super(s, throwable);
    }
}
