package com.firefly.server.http2;

import com.firefly.net.DecoderChain;
import com.firefly.net.Session;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.ByteBuffer;

import static com.firefly.utils.io.BufferUtils.toHeapBuffer;

public class HTTP2ServerDecoder extends DecoderChain {

    private static Logger log = LoggerFactory.getLogger("firefly-system");

    @Override
    public void decode(ByteBuffer buffer, Session session) throws Throwable {
        if (!buffer.hasRemaining()) {
            return;
        }

        if (log.isDebugEnabled()) {
            log.debug("the server session {} received the {} bytes", session.getSessionId(), buffer.remaining());
        }

        HTTP2ServerConnection connection = (HTTP2ServerConnection) session.getAttachment();
        connection.getParser().parse(toHeapBuffer(buffer));
    }

}
