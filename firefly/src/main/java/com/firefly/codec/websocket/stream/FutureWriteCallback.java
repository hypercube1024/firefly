package com.firefly.codec.websocket.stream;


import com.firefly.codec.websocket.model.WriteCallback;
import com.firefly.utils.concurrent.FutureCallback;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.Future;

/**
 * Allows events to a {@link WriteCallback} to drive a {@link Future} for the user.
 */
public class FutureWriteCallback extends FutureCallback implements WriteCallback {
    private static Logger LOG = LoggerFactory.getLogger("firefly-system");

    @Override
    public void writeFailed(Throwable cause) {
        if (LOG.isDebugEnabled())
            LOG.debug(".writeFailed", cause);
        failed(cause);
    }

    @Override
    public void writeSuccess() {
        if (LOG.isDebugEnabled())
            LOG.debug(".writeSuccess");
        succeeded();
    }
}
