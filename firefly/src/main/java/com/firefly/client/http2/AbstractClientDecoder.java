package com.firefly.client.http2;

import com.firefly.codec.common.AbstractConnection;
import com.firefly.codec.common.ConnectionType;
import com.firefly.net.Decoder;
import com.firefly.net.Session;

import java.nio.ByteBuffer;

/**
 * @author Pengtao Qiu
 */
public class AbstractClientDecoder implements Decoder {

    public ConnectionType getConnectionType(Session session) {
        Object attachment = session.getAttachment();
        if (attachment instanceof AbstractConnection) {
            return ((AbstractConnection) attachment).getConnectionType();
        } else {
            return null;
        }
    }

    @Override
    public void decode(ByteBuffer buf, Session session) throws Throwable {

    }
}
