package com.fireflysource.net.http.common.exception;

/**
 * <p>
 * Exception thrown to indicate a Bad HTTP Message has either been received or
 * attempted to be generated. Typically these are handled with either 400 or 500
 * responses.
 * </p>
 */
public class BadMessageException extends RuntimeException {
    private static final long serialVersionUID = -4907256166019479626L;
    private final int code;
    private final String reason;

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
        this.code = code;
        this.reason = reason;
    }

    public BadMessageException(String reason, Throwable cause) {
        this(400, reason, cause);
    }

    public BadMessageException(int code, String reason, Throwable cause) {
        super(code + ": " + reason, cause);
        this.code = code;
        this.reason = reason;
    }

    public int getCode() {
        return code;
    }

    public String getReason() {
        return reason;
    }
}
