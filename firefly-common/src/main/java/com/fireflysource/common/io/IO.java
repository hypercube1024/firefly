package com.fireflysource.common.io;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.channels.GatheringByteChannel;
import java.nio.charset.Charset;

public class IO {

    public final static String CRLF = "\015\012";

    public final static byte[] CRLF_BYTES = {(byte) '\015', (byte) '\012'};

    public static final int bufferSize = 64 * 1024;
    private static final NullOS NULL_STREAM = new NullOS();
    private static final ClosedIS CLOSED_STREAM = new ClosedIS();
    private static final NullWrite NULL_WRITER = new NullWrite();
    private static final PrintWriter NULL_PRINT_WRITER = new PrintWriter(NULL_WRITER);

    /**
     * Copy Stream in to Stream out until EOF or exception.
     *
     * @param in  the input stream to read from (until EOF)
     * @param out the output stream to write to
     * @throws IOException if unable to copy streams
     */
    public static void copy(InputStream in, OutputStream out) throws IOException {
        copy(in, out, -1);
    }

    /**
     * Copy Reader to Writer out until EOF or exception.
     *
     * @param in  the read to read from (until EOF)
     * @param out the writer to write to
     * @throws IOException if unable to copy the streams
     */
    public static void copy(Reader in, Writer out) throws IOException {
        copy(in, out, -1);
    }

    /**
     * Copy Stream in to Stream for byteCount bytes or until EOF or exception.
     *
     * @param in        the stream to read from
     * @param out       the stream to write to
     * @param byteCount the number of bytes to copy
     * @throws IOException if unable to copy the streams
     */
    public static void copy(InputStream in, OutputStream out, long byteCount) throws IOException {
        byte[] buffer = new byte[bufferSize];
        int len;

        if (byteCount >= 0) {
            while (byteCount > 0) {
                int max = byteCount < bufferSize ? (int) byteCount : bufferSize;
                len = in.read(buffer, 0, max);

                if (len == -1) {
                    break;
                }
                byteCount -= len;
                out.write(buffer, 0, len);
            }
        } else {
            while (true) {
                len = in.read(buffer, 0, bufferSize);
                if (len < 0) {
                    break;
                }
                out.write(buffer, 0, len);
            }
        }
    }

    /**
     * Copy Reader to Writer for byteCount bytes or until EOF or exception.
     *
     * @param in        the Reader to read from
     * @param out       the Writer to write to
     * @param byteCount the number of bytes to copy
     * @throws IOException if unable to copy streams
     */
    public static void copy(Reader in, Writer out, long byteCount) throws IOException {
        char[] buffer = new char[bufferSize];
        int len;

        if (byteCount >= 0) {
            while (byteCount > 0) {
                if (byteCount < bufferSize) {
                    len = in.read(buffer, 0, (int) byteCount);
                } else {
                    len = in.read(buffer, 0, bufferSize);
                }
                if (len == -1) {
                    break;
                }
                byteCount -= len;
                out.write(buffer, 0, len);
            }
        } else if (out instanceof PrintWriter) {
            PrintWriter pout = (PrintWriter) out;
            while (!pout.checkError()) {
                len = in.read(buffer, 0, bufferSize);
                if (len == -1)
                    break;
                out.write(buffer, 0, len);
            }
        } else {
            while (true) {
                len = in.read(buffer, 0, bufferSize);
                if (len == -1) {
                    break;
                }
                out.write(buffer, 0, len);
            }
        }
    }

    /**
     * Copy files or directories
     *
     * @param from the file to copy
     * @param to   the destination to copy to
     * @throws IOException if unable to copy
     */
    public static void copy(File from, File to) throws IOException {
        if (from.isDirectory()) {
            copyDir(from, to);
        } else {
            copyFile(from, to);
        }
    }

    public static void copyDir(File from, File to) throws IOException {
        if (to.exists()) {
            if (!to.isDirectory()) {
                throw new IllegalArgumentException(to.toString());
            }
        } else {
            boolean success = to.mkdirs();
            if (!success) {
                return;
            }
        }

        File[] files = from.listFiles();
        if (files != null) {
            for (File file : files) {
                String name = file.getName();
                if (".".equals(name) || "..".equals(name)) {
                    continue;
                }
                copy(file, new File(to, name));
            }
        }
    }

    public static void copyFile(File from, File to) throws IOException {
        try (InputStream in = new FileInputStream(from); OutputStream out = new FileOutputStream(to)) {
            copy(in, out);
        }
    }

    /**
     * Read input stream to string.
     *
     * @param in the stream to read from (until EOF)
     * @return the String parsed from stream (default Charset)
     * @throws IOException if unable to read the stream (or handle the charset)
     */
    public static String toString(InputStream in) throws IOException {
        return toString(in, (Charset) null);
    }

    /**
     * Read input stream to string.
     *
     * @param in       the stream to read from (until EOF)
     * @param encoding the encoding to use (can be null to use default Charset)
     * @return the String parsed from the stream
     * @throws IOException if unable to read the stream (or handle the charset)
     */
    public static String toString(InputStream in, String encoding) throws IOException {
        return toString(in, encoding == null ? null : Charset.forName(encoding));
    }

    /**
     * Read input stream to string.
     *
     * @param in       the stream to read from (until EOF)
     * @param encoding the Charset to use (can be null to use default Charset)
     * @return the String parsed from the stream
     * @throws IOException if unable to read the stream (or handle the charset)
     */
    public static String toString(InputStream in, Charset encoding) throws IOException {
        StringWriter writer = new StringWriter();
        InputStreamReader reader = encoding == null ? new InputStreamReader(in) : new InputStreamReader(in, encoding);

        copy(reader, writer);
        return writer.toString();
    }

    /**
     * Read input stream to string.
     *
     * @param in the reader to read from (until EOF)
     * @return the String parsed from the reader
     * @throws IOException if unable to read the stream (or handle the charset)
     */
    public static String toString(Reader in) throws IOException {
        StringWriter writer = new StringWriter();
        copy(in, writer);
        return writer.toString();
    }

    /**
     * Delete File. This delete will recursively delete directories - BE
     * CAREFULL
     *
     * @param file The file (or directory) to be deleted.
     * @return true if anything was deleted. (note: this does not mean that all
     * content in a directory was deleted)
     */
    public static boolean delete(File file) {
        if (!file.exists()) {
            return false;
        }
        if (file.isDirectory()) {
            File[] files = file.listFiles();
            for (int i = 0; files != null && i < files.length; i++) {
                delete(files[i]);
            }
        }
        return file.delete();
    }

    /**
     * Closes an arbitrary closable, and logs exceptions at ignore level
     *
     * @param closeable the closeable to close
     */
    public static void close(Closeable closeable) {
        try {
            if (closeable != null)
                closeable.close();
        } catch (IOException ignore) {
        }
    }

    /**
     * closes an input stream, and logs exceptions
     *
     * @param is the input stream to close
     */
    public static void close(InputStream is) {
        close((Closeable) is);
    }

    /**
     * closes an output stream, and logs exceptions
     *
     * @param os the output stream to close
     */
    public static void close(OutputStream os) {
        close((Closeable) os);
    }

    /**
     * closes a reader, and logs exceptions
     *
     * @param reader the reader to close
     */
    public static void close(Reader reader) {
        close((Closeable) reader);
    }

    /**
     * closes a writer, and logs exceptions
     *
     * @param writer the writer to close
     */
    public static void close(Writer writer) {
        close((Closeable) writer);
    }

    public static byte[] readBytes(InputStream in) throws IOException {
        ByteArrayOutputStream bout = new ByteArrayOutputStream();
        copy(in, bout);
        return bout.toByteArray();
    }

    /**
     * A gathering write utility wrapper.
     * <p>
     * This method wraps a gather write with a loop that handles the limitations
     * of some operating systems that have a limit on the number of buffers
     * written. The method loops on the write until either all the content is
     * written or no progress is made.
     *
     * @param out     The GatheringByteChannel to write to
     * @param buffers The buffers to write
     * @param offset  The offset into the buffers array
     * @param length  The length in buffers to write
     * @return The total bytes written
     * @throws IOException if unable write to the GatheringByteChannel
     */
    public static long write(GatheringByteChannel out, ByteBuffer[] buffers, int offset, int length)
            throws IOException {
        long total = 0;
        write:
        while (length > 0) {
            // Write as much as we can
            long wrote = out.write(buffers, offset, length);

            // If we can't write any more, give up
            if (wrote == 0)
                break;

            // count the total
            total += wrote;

            // Look for unwritten content
            for (int i = offset; i < buffers.length; i++) {
                if (buffers[i].hasRemaining()) {
                    // loop with new offset and length;
                    length = length - (i - offset);
                    offset = i;
                    continue write;
                }
            }
            length = 0;
        }

        return total;
    }

    /**
     * @return An outputstream to nowhere
     */
    public static OutputStream getNullStream() {
        return NULL_STREAM;
    }

    /**
     * @return An outputstream to nowhere
     */
    public static InputStream getClosedStream() {
        return CLOSED_STREAM;
    }

    /**
     * @return An writer to nowhere
     */
    public static Writer getNullWriter() {
        return NULL_WRITER;
    }

    /**
     * @return An writer to nowhere
     */
    public static PrintWriter getNullPrintWriter() {
        return NULL_PRINT_WRITER;
    }

    private static class NullOS extends OutputStream {
        @Override
        public void close() {
        }

        @Override
        public void flush() {
        }

        @Override
        public void write(byte[] b) {
        }

        @Override
        public void write(byte[] b, int i, int l) {
        }

        @Override
        public void write(int b) {
        }
    }

    private static class ClosedIS extends InputStream {
        @Override
        public int read() throws IOException {
            return -1;
        }
    }

    private static class NullWrite extends Writer {
        @Override
        public void close() {
        }

        @Override
        public void flush() {
        }

        @Override
        public void write(char[] b) {
        }

        @Override
        public void write(char[] b, int o, int l) {
        }

        @Override
        public void write(int b) {
        }

        @Override
        public void write(String s) {
        }

        @Override
        public void write(String s, int o, int l) {
        }
    }

}
