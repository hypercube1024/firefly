package com.firefly.codec.http2.stream;

import java.io.IOException;
import java.io.OutputStream;

/**
 * @author Pengtao Qiu
 */
public class BufferedHTTPOutputStream extends OutputStream {

    private int bufferSize;
    private HTTPOutputStream output;
    private byte[] buf;
    private int count;

    public BufferedHTTPOutputStream(HTTPOutputStream output, int bufferSize) {
        this.bufferSize = bufferSize;
        this.output = output;
        if (bufferSize > 1024) {
            this.buf = new byte[bufferSize];
        } else {
            this.buf = new byte[1024];
        }
    }

    @Override
    public synchronized void write(int b) throws IOException {
        if (count >= buf.length) {
            flush();
        }
        buf[count++] = (byte) b;
    }

    @Override
    public synchronized void write(byte[] array, int offset, int length) throws IOException {
        if (array == null || array.length == 0 || length <= 0) {
            return;
        }

        if (offset < 0) {
            throw new IllegalArgumentException("the offset is less than 0");
        }

        if (length >= buf.length) {
            flush();
            output.write(array, offset, length);
            return;
        }
        if (length > buf.length - count) {
            flush();
        }
        System.arraycopy(array, offset, buf, count, length);
        count += length;
    }

    @Override
    public synchronized void flush() throws IOException {
        if (count > 0) {
            output.write(buf, 0, count);
            count = 0;
            buf = new byte[bufferSize];
        }
    }

    @Override
    public synchronized void close() throws IOException {
        flush();
        output.close();
    }
}
