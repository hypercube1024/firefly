package com.firefly.utils.json.exception;

public class JsonException extends RuntimeException {

    private static final long serialVersionUID = -6018684860739376818L;

    public JsonException(String msg) {
        super(msg);
    }

    public JsonException(Throwable throwable) {
        super(throwable);
    }
}
