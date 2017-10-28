package com.firefly.db;

/**
 * @author Pengtao Qiu
 */
public class RecordNotFound extends RuntimeException {

    public RecordNotFound() {

    }

    public RecordNotFound(String message) {
        super(message);
    }
}
