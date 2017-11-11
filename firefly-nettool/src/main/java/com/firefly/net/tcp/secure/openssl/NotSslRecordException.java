package com.firefly.net.tcp.secure.openssl;

import javax.net.ssl.SSLException;

/**
 * Special {@link SSLException} which will get thrown if a packet is
 * received that not looks like a TLS/SSL record. A user can check for
 * this {@link NotSslRecordException} and so detect if one peer tries to
 * use secure and the other plain connection.
 */
public class NotSslRecordException extends SSLException {

    private static final long serialVersionUID = -4316784434770656841L;

    public NotSslRecordException() {
        super("");
    }

    public NotSslRecordException(String message) {
        super(message);
    }

    public NotSslRecordException(Throwable cause) {
        super(cause);
    }

    public NotSslRecordException(String message, Throwable cause) {
        super(message, cause);
    }

}
