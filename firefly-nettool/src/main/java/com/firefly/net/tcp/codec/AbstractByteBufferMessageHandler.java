package com.firefly.net.tcp.codec;

import java.nio.ByteBuffer;

/**
 * @author Pengtao Qiu
 */
abstract public class AbstractByteBufferMessageHandler<T> extends AbstractMessageHandler<ByteBuffer, T> {

    protected ByteBuffer buffer;

    public ByteBuffer getBuffer() {
        return buffer;
    }

    @Override
    public void parse(ByteBuffer buf) {
        try {
            merge(buf);
            parse();
        } catch (Throwable t) {
            exception.call(t);
        }
    }

    protected void merge(ByteBuffer buf) {
        if (buffer != null) {
            if (buffer.hasRemaining()) {
                ByteBuffer tmp = ByteBuffer.allocate(buffer.remaining() + buf.remaining());
                tmp.put(buffer).put(buf).flip();
                buffer = tmp;
            } else {
                buffer = buf;
            }
        } else {
            buffer = buf;
        }
    }

    abstract protected void parse();
}
