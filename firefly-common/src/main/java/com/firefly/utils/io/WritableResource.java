package com.firefly.utils.io;

import java.io.IOException;
import java.io.OutputStream;

/**
 * Extended interface for a resource that supports writing to it.
 * Provides an {@link #getOutputStream() OutputStream accessor}.
 *
 * @see java.io.OutputStream
 */
public interface WritableResource extends Resource {

    /**
     * Return whether the contents of this resource can be modified,
     * e.g. via {@link #getOutputStream()} or {@link #getFile()}.
     * <p>Will be {@code true} for typical resource descriptors;
     * note that actual content writing may still fail when attempted.
     * However, a value of {@code false} is a definitive indication
     * that the resource content cannot be modified.
     *
     * @see #getOutputStream()
     * @see #isReadable()
     */
    boolean isWritable();

    /**
     * Return an {@link OutputStream} for the underlying resource,
     * allowing to (over-)write its content.
     *
     * @throws IOException if the stream could not be opened
     * @see #getInputStream()
     */
    OutputStream getOutputStream() throws IOException;

}
