package com.firefly.net;

import java.io.Closeable;
import java.net.InetSocketAddress;

/**
 * @author Pengtao Qiu
 */
public interface Connection extends Closeable {

    Object getAttachment();

    void setAttachment(Object object);

    int getSessionId();

    long getOpenTime();

    long getCloseTime();

    long getDuration();

    long getLastReadTime();

    long getLastWrittenTime();

    long getLastActiveTime();

    long getReadBytes();

    long getWrittenBytes();

    long getIdleTimeout();

    boolean isOpen();

    boolean isClosed();

    InetSocketAddress getLocalAddress();

    InetSocketAddress getRemoteAddress();

}
