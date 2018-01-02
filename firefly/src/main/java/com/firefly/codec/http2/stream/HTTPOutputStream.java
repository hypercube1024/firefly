package com.firefly.codec.http2.stream;

import com.firefly.codec.http2.model.HttpHeader;
import com.firefly.codec.http2.model.MetaData;
import com.firefly.utils.Assert;
import com.firefly.utils.io.BufferUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;

public abstract class HTTPOutputStream extends OutputStream {

    protected static final Logger log = LoggerFactory.getLogger("firefly-system");

    protected final boolean clientMode;
    protected final MetaData info;
    protected boolean closed;
    protected boolean committed;

    public HTTPOutputStream(MetaData info, boolean clientMode) {
        this.info = info;
        this.clientMode = clientMode;
    }

    public synchronized boolean isClosed() {
        return closed;
    }

    public synchronized boolean isCommitted() {
        return committed;
    }

    @Override
    public void write(int b) throws IOException {
        write(new byte[]{(byte) b});
    }

    @Override
    public void write(byte[] array, int offset, int length) throws IOException {
        Assert.notNull(array, "The data must be not null");
        write(ByteBuffer.wrap(array, offset, length));
    }

    public synchronized void writeWithContentLength(Collection<ByteBuffer> data) throws IOException {
        if (closed) {
            return;
        }

        try {
            if (!committed) {
                long contentLength = BufferUtils.remaining(data);
                info.getFields().put(HttpHeader.CONTENT_LENGTH, String.valueOf(contentLength));
            }
            for (ByteBuffer buf : data) {
                write(buf);
            }
        } finally {
            close();
        }
    }

    public void writeWithContentLength(ByteBuffer[] data) throws IOException {
        writeWithContentLength(Arrays.asList(data));
    }

    public void writeWithContentLength(ByteBuffer data) throws IOException {
        writeWithContentLength(Collections.singleton(data));
    }

    abstract public void commit() throws IOException;

    abstract public void write(ByteBuffer data) throws IOException;
}
