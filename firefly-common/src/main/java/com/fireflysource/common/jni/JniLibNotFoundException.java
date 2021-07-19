package com.fireflysource.common.jni;

public class JniLibNotFoundException extends RuntimeException {
    public JniLibNotFoundException(String message) {
        super(message);
    }
}
