package com.fireflysource.net.websocket.extension.compress;

import com.fireflysource.common.io.BufferUtils;
import com.fireflysource.net.websocket.exception.MessageTooLargeException;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

public class ByteAccumulator {
    private final List<byte[]> chunks = new ArrayList<>();
    private final int maxSize;
    private int length = 0;

    public ByteAccumulator(int maxOverallBufferSize) {
        this.maxSize = maxOverallBufferSize;
    }

    public void copyChunk(byte[] buf, int offset, int length) {
        if (this.length + length > maxSize) {
            String err = String.format("Resulting message size [%,d] is too large for configured max of [%,d]", this.length + length, maxSize);
            throw new MessageTooLargeException(err);
        }

        byte[] copy = new byte[length - offset];
        System.arraycopy(buf, offset, copy, 0, length);

        chunks.add(copy);
        this.length += length;
    }

    public int getLength() {
        return length;
    }

    public void transferTo(ByteBuffer buffer) {
        if (buffer.remaining() < length) {
            throw new IllegalArgumentException(String.format("Not enough space in ByteBuffer remaining [%d] for accumulated buffers length [%d]",
                    buffer.remaining(), length));
        }

        int position = buffer.position();
        for (byte[] chunk : chunks) {
            buffer.put(chunk, 0, chunk.length);
        }
        BufferUtils.flipToFlush(buffer, position);
    }
}
