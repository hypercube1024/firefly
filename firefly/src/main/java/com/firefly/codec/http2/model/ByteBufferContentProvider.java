package com.firefly.codec.http2.model;

import java.nio.ByteBuffer;
import java.util.Iterator;
import java.util.NoSuchElementException;

/**
 * A {@link ContentProvider} for {@link ByteBuffer}s.
 * <p>
 * The position and limit of the {@link ByteBuffer}s passed to the constructor are not modified,
 * and each invocation of the {@link #iterator()} method returns a {@link ByteBuffer#slice() slice}
 * of the original {@link ByteBuffer}.
 */
public class ByteBufferContentProvider extends AbstractTypedContentProvider {
    private final ByteBuffer[] buffers;
    private final int length;

    public ByteBufferContentProvider(ByteBuffer... buffers) {
        this("application/octet-stream", buffers);
    }

    public ByteBufferContentProvider(String contentType, ByteBuffer... buffers) {
        super(contentType);
        this.buffers = buffers;
        int length = 0;
        for (ByteBuffer buffer : buffers)
            length += buffer.remaining();
        this.length = length;
    }

    @Override
    public long getLength() {
        return length;
    }

    @Override
    public Iterator<ByteBuffer> iterator() {
        return new Iterator<ByteBuffer>() {
            private int index;

            @Override
            public boolean hasNext() {
                return index < buffers.length;
            }

            @Override
            public ByteBuffer next() {
                try {
                    ByteBuffer buffer = buffers[index];
                    buffers[index] = buffer.slice();
                    ++index;
                    return buffer;
                } catch (ArrayIndexOutOfBoundsException x) {
                    throw new NoSuchElementException();
                }
            }

            @Override
            public void remove() {
                throw new UnsupportedOperationException();
            }
        };
    }
}
