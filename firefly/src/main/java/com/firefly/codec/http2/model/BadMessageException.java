package com.firefly.codec.http2.model;

/**
 * <p>
 * Exception thrown to indicate a Bad HTTP Message has either been received or
 * attempted to be generated. Typically these are handled with either 400 or 500
 * responses.
 * </p>
 */
public class BadMessageException extends RuntimeException {
    private static final long serialVersionUID = -4907256166019479626L;
    public final int _code;
    public final String _reason;

    public BadMessageException() {
        this(400, null);
    }

    public BadMessageException(int code) {
        this(code, null);
    }

    public BadMessageException(String reason) {
        this(400, reason);
    }

    public BadMessageException(int code, String reason) {
        super(code + ": " + reason);
        _code = code;
        _reason = reason;
    }

    public BadMessageException(String reason, Throwable cause) {
        this(400, reason, cause);
    }

    public BadMessageException(int code, String reason, Throwable cause) {
        super(code + ": " + reason, cause);
        _code = code;
        _reason = reason;
    }

    public int getCode() {
        return _code;
    }

    public String getReason() {
        return _reason;
    }
}
