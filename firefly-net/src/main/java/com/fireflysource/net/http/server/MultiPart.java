package com.fireflysource.net.http.server;

import com.fireflysource.net.http.common.model.HttpFields;

public interface MultiPart {

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
     * Gets the file name specified by the client
     *
     * @return the submitted file name
     */
    String getSubmittedFileName();

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
}
