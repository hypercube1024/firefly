package com.fireflysource.net.http.common.exception;

import java.net.URISyntaxException;

public class URISyntaxRuntimeException extends RuntimeException {

    public URISyntaxRuntimeException(String message, URISyntaxException e) {
        super(message, e);
    }
}
