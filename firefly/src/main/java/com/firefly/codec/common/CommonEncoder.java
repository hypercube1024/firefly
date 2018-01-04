package com.firefly.codec.common;

import com.firefly.net.ByteBufferArrayOutputEntry;
import com.firefly.net.ByteBufferOutputEntry;
import com.firefly.net.EncoderChain;
import com.firefly.net.Session;
import com.firefly.utils.concurrent.Callback;

import java.nio.ByteBuffer;

/**
 * @author Pengtao Qiu
 */
public class CommonEncoder extends EncoderChain {

    @Override
    public void encode(Object message, Session session) throws Throwable {
        Object attachment = session.getAttachment();
        if (attachment instanceof AbstractConnection) {
            AbstractConnection connection = (AbstractConnection) attachment;
            if (connection.isEncrypted()) {
                if (message instanceof ByteBuffer) {
                    connection.encrypt((ByteBuffer) message);
                } else if (message instanceof ByteBuffer[]) {
                    connection.encrypt((ByteBuffer[]) message);
                } else if (message instanceof ByteBufferOutputEntry) {
                    connection.encrypt((ByteBufferOutputEntry) message);
                } else if (message instanceof ByteBufferArrayOutputEntry) {
                    connection.encrypt((ByteBufferArrayOutputEntry) message);
                } else {
                    throw new IllegalArgumentException("the encoder object type error " + message.getClass());
                }
            } else {
                if (message instanceof ByteBuffer) {
                    session.write((ByteBuffer) message, Callback.NOOP);
                } else if (message instanceof ByteBuffer[]) {
                    session.write((ByteBuffer[]) message, Callback.NOOP);
                } else if (message instanceof ByteBufferOutputEntry) {
                    session.write((ByteBufferOutputEntry) message);
                } else if (message instanceof ByteBufferArrayOutputEntry) {
                    session.write((ByteBufferArrayOutputEntry) message);
                } else {
                    throw new IllegalArgumentException("the encoder object type error " + message.getClass());
                }
            }
        }
    }
}
