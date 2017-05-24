package com.firefly.utils.exception;

/**
 * Firefly common module runtime exception
 * @author Pengtao Qiu
 */
public class CommonRuntimeException extends RuntimeException {

    public CommonRuntimeException() {
    }

    public CommonRuntimeException(String message) {
        super(message);
    }

    public CommonRuntimeException(Throwable cause) {
        super(cause);
    }

    public CommonRuntimeException(String message, Throwable cause) {
        super(message, cause);
    }
}
