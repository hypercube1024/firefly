package com.firefly.utils.io;

import java.io.*;
import java.util.UUID;

public class FilePipedStream implements PipedStream {

    private OutputStream out;
    private InputStream in;
    private File temp;

    public FilePipedStream(String tempdir) {
        temp = new File(tempdir, UUID.randomUUID().toString());
        temp.deleteOnExit();
    }

    public FilePipedStream(File file) {
        temp = file;
    }

    @Override
    public void close() throws IOException {
        if (temp == null)
            return;

        try {
            temp.delete();
        } finally {
            if (in != null)
                in.close();

            if (out != null)
                out.close();
        }

        in = null;
        out = null;
        temp = null;
    }

    @Override
    public InputStream getInputStream() throws IOException {
        if (in == null) {
            in = new BufferedInputStream(new FileInputStream(temp));
        }
        return in;
    }

    @Override
    public OutputStream getOutputStream() throws IOException {
        if (out == null) {
            out = new BufferedOutputStream(new FileOutputStream(temp));
        }
        return out;
    }
}
