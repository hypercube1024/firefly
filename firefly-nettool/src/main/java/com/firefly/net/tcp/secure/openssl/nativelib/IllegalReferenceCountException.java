package com.firefly.net.tcp.secure.openssl.nativelib;

/**
 * An {@link IllegalStateException} which is raised when a user attempts to access a {@link ReferenceCounted} whose
 * reference count has been decreased to 0 (and consequently freed).
 */
public class IllegalReferenceCountException extends IllegalStateException {

    private static final long serialVersionUID = -2507492394288153468L;

    public IllegalReferenceCountException() { }

    public IllegalReferenceCountException(int refCnt) {
        this("refCnt: " + refCnt);
    }

    public IllegalReferenceCountException(int refCnt, int increment) {
        this("refCnt: " + refCnt + ", " + (increment > 0? "increment: " + increment : "decrement: " + -increment));
    }

    public IllegalReferenceCountException(String message) {
        super(message);
    }

    public IllegalReferenceCountException(String message, Throwable cause) {
        super(message, cause);
    }

    public IllegalReferenceCountException(Throwable cause) {
        super(cause);
    }
}
