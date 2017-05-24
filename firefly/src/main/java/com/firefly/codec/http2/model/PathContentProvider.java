package com.firefly.codec.http2.model;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Closeable;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SeekableByteChannel;
import java.nio.file.*;
import java.util.Iterator;
import java.util.NoSuchElementException;

/**
 * <p>A {@link ContentProvider} for files using JDK 7's {@code java.nio.file} APIs.</p>
 * <p>It is possible to specify, at the constructor, a buffer size used to read
 * content from the stream, by default 4096 bytes.</p>
 */
public class PathContentProvider extends AbstractTypedContentProvider {

    private static final Logger LOG = LoggerFactory.getLogger("firefly-system");

    private final Path filePath;
    private final long fileSize;
    private final int bufferSize;

    public PathContentProvider(Path filePath) throws IOException {
        this(filePath, 4096);
    }

    public PathContentProvider(Path filePath, int bufferSize) throws IOException {
        this("application/octet-stream", filePath, bufferSize);
    }

    public PathContentProvider(String contentType, Path filePath) throws IOException {
        this(contentType, filePath, 4096);
    }

    public PathContentProvider(String contentType, Path filePath, int bufferSize) throws IOException {
        super(contentType);
        if (!Files.isRegularFile(filePath))
            throw new NoSuchFileException(filePath.toString());
        if (!Files.isReadable(filePath))
            throw new AccessDeniedException(filePath.toString());
        this.filePath = filePath;
        this.fileSize = Files.size(filePath);
        this.bufferSize = bufferSize;
    }

    @Override
    public long getLength() {
        return fileSize;
    }

    @Override
    public Iterator<ByteBuffer> iterator() {
        return new PathIterator();
    }

    private class PathIterator implements Iterator<ByteBuffer>, Closeable {
        private SeekableByteChannel channel;
        private long position;

        @Override
        public boolean hasNext() {
            return position < getLength();
        }

        @Override
        public ByteBuffer next() {
            try {
                if (channel == null) {
                    channel = Files.newByteChannel(filePath, StandardOpenOption.READ);
                    if (LOG.isDebugEnabled())
                        LOG.debug("Opened file {}", filePath);
                }

                ByteBuffer buffer = ByteBuffer.allocate(bufferSize);
                int read = channel.read(buffer);
                if (read < 0)
                    throw new NoSuchElementException();

                if (LOG.isDebugEnabled())
                    LOG.debug("Read {} bytes from {}", read, filePath);

                position += read;

                buffer.flip();
                return buffer;
            } catch (NoSuchElementException x) {
                close();
                throw x;
            } catch (Throwable x) {
                close();
                throw (NoSuchElementException) new NoSuchElementException().initCause(x);
            }
        }

        @Override
        public void close() {
            try {
                if (channel != null)
                    channel.close();
            } catch (Throwable x) {
                LOG.error("channel close error", x);
            }
        }
    }
}
