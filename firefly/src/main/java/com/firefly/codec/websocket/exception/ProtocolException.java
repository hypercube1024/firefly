package com.firefly.codec.websocket.exception;

import com.firefly.codec.websocket.model.StatusCode;

/**
 * Per spec, a protocol error should result in a Close frame of status code 1002 (PROTOCOL_ERROR)
 */
public class ProtocolException extends CloseException {
    public ProtocolException(String message) {
        super(StatusCode.PROTOCOL, message);
    }

    public ProtocolException(String message, Throwable t) {
        super(StatusCode.PROTOCOL, message, t);
    }

    public ProtocolException(Throwable t) {
        super(StatusCode.PROTOCOL, t);
    }
}
