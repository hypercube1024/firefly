package com.firefly.utils.io;

import java.io.EOFException;

/**
 * A Firefly specialization of EOFException.
 * <p>
 * This is thrown by Firefly to distinguish between EOF received from the
 * connection, vs and EOF thrown by some application talking to some other
 * file/socket etc. The only difference in handling is that Firefly EOFs are
 * logged less verbosely.
 */
public class EofException extends EOFException {

    private static final long serialVersionUID = -3003752099703697703L;

    public EofException() {
    }

    public EofException(String reason) {
        super(reason);
    }

    public EofException(Throwable th) {
        if (th != null)
            initCause(th);
    }
}
