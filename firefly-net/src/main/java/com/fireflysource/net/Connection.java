package com.fireflysource.net;

import java.io.Closeable;
import java.net.InetSocketAddress;

/**
 * @author Pengtao Qiu
 */
public interface Connection extends Closeable {

    Object getAttachment();

    void setAttachment(Object object);

    int getId();

    long getOpenTime();

    long getCloseTime();

    long getDuration();

    long getLastReadTime();

    long getLastWrittenTime();

    long getLastActiveTime();

    long getReadBytes();

    long getWrittenBytes();

    long getIdleTimeout();

    long getMaxIdleTimeout();

    boolean isClosed();

    InetSocketAddress getLocalAddress();

    InetSocketAddress getRemoteAddress();

}
