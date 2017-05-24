package com.firefly.codec.http2.model;

import java.nio.ByteBuffer;
import java.util.Iterator;
import java.util.NoSuchElementException;

/**
 * A {@link ContentProvider} for byte arrays.
 */
public class BytesContentProvider extends AbstractTypedContentProvider {
    private final byte[][] bytes;
    private final long length;

    public BytesContentProvider(byte[]... bytes) {
        this("application/octet-stream", bytes);
    }

    public BytesContentProvider(String contentType, byte[]... bytes) {
        super(contentType);
        this.bytes = bytes;
        long length = 0;
        for (byte[] buffer : bytes)
            length += buffer.length;
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
                return index < bytes.length;
            }

            @Override
            public ByteBuffer next() {
                try {
                    return ByteBuffer.wrap(bytes[index++]);
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
