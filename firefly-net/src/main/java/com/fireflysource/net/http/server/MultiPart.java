package com.fireflysource.net.http.server;

import com.fireflysource.common.io.InputChannel;
import com.fireflysource.net.http.common.model.HttpFields;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

public interface MultiPart extends InputChannel {

    /**
     * Gets the content type of this part.
     *
     * @return The content type of this part.
     */
    String getContentType();

    /**
     * Gets the name of this part
     *
     * @return The name of this part as a <tt>String</tt>
     */
    String getName();

    /**
     * Gets the file name
     *
     * @return The file name.
     */
    String getFileName();

    /**
     * Returns the size of this file.
     *
     * @return a <code>long</code> specifying the size of this part, in bytes.
     */
    long getSize();

    /**
     * Get this part headers.
     *
     * @return This part headers.
     */
    HttpFields getHttpFields();

    /**
     * Get string body. If the content length not exceeded file threshold.
     *
     * @param charset The string charset.
     * @return The string body.
     */
    String getStringBody(Charset charset);

    /**
     * Get string body. If the content length not exceeded file threshold.
     *
     * @return The string body.
     */
    default String getStringBody() {
        return getStringBody(StandardCharsets.UTF_8);
    }
}
