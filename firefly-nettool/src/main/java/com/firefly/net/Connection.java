package com.firefly.net;

import com.firefly.utils.function.Action0;
import com.firefly.utils.function.Action1;

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

    InetSocketAddress getLocalAddress();

    InetSocketAddress getRemoteAddress();

}
