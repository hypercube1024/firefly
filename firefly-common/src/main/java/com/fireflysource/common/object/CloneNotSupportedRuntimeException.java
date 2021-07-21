package com.fireflysource.common.object;

public class CloneNotSupportedRuntimeException extends RuntimeException {

    public CloneNotSupportedRuntimeException(CloneNotSupportedException e) {
        super(e);
    }
}
