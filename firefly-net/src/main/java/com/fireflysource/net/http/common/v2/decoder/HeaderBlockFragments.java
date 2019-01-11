package com.fireflysource.net.http.common.v2.decoder;

import com.fireflysource.net.http.common.v2.frame.PriorityFrame;

import java.nio.ByteBuffer;

public class HeaderBlockFragments {
    private PriorityFrame priorityFrame;
    private boolean endStream;
    private int streamId;
    private ByteBuffer storage;

    public void storeFragment(ByteBuffer fragment, int length, boolean last) {
        if (storage == null) {
            int space = last ? length : length * 2;
            storage = ByteBuffer.allocate(space);
        }

        // Grow the storage if necessary.
        if (storage.remaining() < length) {
            int space = last ? length : length * 2;
            int capacity = storage.position() + space;
            ByteBuffer newStorage = ByteBuffer.allocate(capacity);
            storage.flip();
            newStorage.put(storage);
            storage = newStorage;
        }

        // Copy the fragment into the storage.
        int limit = fragment.limit();
        fragment.limit(fragment.position() + length);
        storage.put(fragment);
        fragment.limit(limit);
    }

    public PriorityFrame getPriorityFrame() {
        return priorityFrame;
    }

    public void setPriorityFrame(PriorityFrame priorityFrame) {
        this.priorityFrame = priorityFrame;
    }

    public boolean isEndStream() {
        return endStream;
    }

    public void setEndStream(boolean endStream) {
        this.endStream = endStream;
    }

    public ByteBuffer complete() {
        ByteBuffer result = storage;
        storage = null;
        result.flip();
        return result;
    }

    public int getStreamId() {
        return streamId;
    }

    public void setStreamId(int streamId) {
        this.streamId = streamId;
    }
}
