package com.fireflysource.net;

import com.fireflysource.common.io.AsyncCloseable;

import java.net.InetSocketAddress;

/**
 * @author Pengtao Qiu
 */
public interface Connection extends AsyncCloseable {

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

    long getIdleTime();

    long getMaxIdleTime();

    boolean isClosed();

    InetSocketAddress getLocalAddress();

    InetSocketAddress getRemoteAddress();

}
