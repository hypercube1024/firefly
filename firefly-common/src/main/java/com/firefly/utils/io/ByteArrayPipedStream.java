package com.firefly.utils.io;

import java.io.*;

public class ByteArrayPipedStream implements PipedStream {

    private ByteArrayOutputStream out;
    private ByteArrayInputStream in;
    private int length;

    public ByteArrayPipedStream(int length) {
        if (length >= Integer.MAX_VALUE)
            throw new IllegalArgumentException("http body length too larger");

        this.length = length;
    }

    @Override
    public void close() throws IOException {
        in = null;
        out = null;
    }

    @Override
    public InputStream getInputStream() {
        if (in == null) {
            in = new ByteArrayInputStream(out.toByteArray());
            out = null;
        }
        return in;
    }

    @Override
    public OutputStream getOutputStream() {
        if (out == null) {
            out = new ByteArrayOutputStream(length);
        }
        return out;
    }

}
