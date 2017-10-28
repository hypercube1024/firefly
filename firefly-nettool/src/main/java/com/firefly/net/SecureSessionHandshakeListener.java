package com.firefly.net;

/**
 * @author Pengtao Qiu
 */
@FunctionalInterface
public interface SecureSessionHandshakeListener {

    void complete(SecureSession secureSession);

}
