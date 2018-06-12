package com.firefly.net;

import com.firefly.utils.concurrent.Callback;
import com.firefly.utils.io.BufferUtils;

import java.nio.ByteBuffer;
import java.util.List;

import static com.firefly.net.OutputEntryType.MERGED_BUFFER;

/**
 * @author Pengtao Qiu
 */
public class MergedOutputEntry extends AbstractByteBufferArrayOutputEntry {

    public MergedOutputEntry(List<Callback> callbackList, List<ByteBuffer> byteBufferList) {
        super(new Callback() {
            @Override
            public void succeeded() {
                callbackList.forEach(Callback::succeeded);
            }

            @Override
            public void failed(Throwable x) {
                callbackList.forEach(c -> c.failed(x));
            }
        }, byteBufferList.toArray(BufferUtils.EMPTY_BYTE_BUFFER_ARRAY));
    }

    @Override
    public OutputEntryType getOutputEntryType() {
        return MERGED_BUFFER;
    }

    @Override
    public long remaining() {
        return BufferUtils.remaining(data);
    }
}
