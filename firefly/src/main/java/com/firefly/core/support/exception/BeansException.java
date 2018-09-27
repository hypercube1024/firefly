package com.firefly.core.support.exception;

@SuppressWarnings("serial")
public abstract class BeansException extends RuntimeException {

    public BeansException(String msg) {
        super(msg);
    }

    public BeansException(String msg, Throwable cause) {
        super(msg, cause);
    }
}
