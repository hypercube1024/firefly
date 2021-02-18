package com.fireflysource.net.http.common.exception;

import java.io.IOException;

/**
 * @author Pengtao Qiu
 */
public class NotSupportContentEncoding extends IOException {

    public NotSupportContentEncoding(String message) {
        super(message);
    }
}
