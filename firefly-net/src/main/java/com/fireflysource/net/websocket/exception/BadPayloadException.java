package com.fireflysource.net.websocket.exception;

import com.fireflysource.net.websocket.model.StatusCode;

/**
 * Exception to terminate the connection because it has received data within a frame payload that was not consistent with the requirements of that frame
 * payload. (eg: not UTF-8 in a text frame, or a unexpected data seen by an extension)
 *
 * @see StatusCode#BAD_PAYLOAD
 */
@SuppressWarnings("serial")
public class BadPayloadException extends CloseException {
    public BadPayloadException(String message) {
        super(StatusCode.BAD_PAYLOAD, message);
    }

    public BadPayloadException(String message, Throwable t) {
        super(StatusCode.BAD_PAYLOAD, message, t);
    }

    public BadPayloadException(Throwable t) {
        super(StatusCode.BAD_PAYLOAD, t);
    }
}
