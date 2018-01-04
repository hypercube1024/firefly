package com.firefly.codec.common;

import com.firefly.net.DecoderChain;
import com.firefly.net.SecureSession;
import com.firefly.net.Session;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.ByteBuffer;

/**
 * @author Pengtao Qiu
 */
public class CommonDecoder extends DecoderChain {

    private static Logger log = LoggerFactory.getLogger("firefly-system");

    public CommonDecoder(DecoderChain next) {
        super(next);
    }

    @Override
    public void decode(ByteBuffer buf, Session session) throws Throwable {
        Object attachment = session.getAttachment();
        if (attachment instanceof AbstractConnection) {
            AbstractConnection connection = (AbstractConnection) attachment;
            if (connection.isEncrypted()) {
                ByteBuffer plaintext = connection.decrypt(buf);
                if (plaintext != null && plaintext.hasRemaining() && next != null) {
                    next.decode(plaintext, session);
                }
            } else {
                if (next != null) {
                    next.decode(buf, session);
                }
            }
        } else if (attachment instanceof SecureSession) {
            SecureSession sslSession = (SecureSession) session.getAttachment();
            ByteBuffer plaintext = sslSession.read(buf);

            if (plaintext != null && plaintext.hasRemaining()) {
                if (log.isDebugEnabled()) {
                    log.debug("The session {} handshake finished and received cleartext size {}",
                            session.getSessionId(), plaintext.remaining());
                }
                if (session.getAttachment() instanceof AbstractConnection) {
                    if (next != null) {
                        next.decode(plaintext, session);
                    }
                } else {
                    throw new IllegalStateException("the connection has not been created");
                }
            } else {
                if (log.isDebugEnabled()) {
                    if (sslSession.isHandshakeFinished()) {
                        log.debug("The ssl session {} need more data", session.getSessionId());
                    } else {
                        log.debug("The ssl session {} is shaking hand", session.getSessionId());
                    }
                }
            }
        }
    }
}
