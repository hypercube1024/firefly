package com.firefly.net.tcp;

import com.firefly.net.buffer.FileRegion;
import com.firefly.utils.function.Action0;
import com.firefly.utils.function.Action1;

import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.util.Collection;

public interface TcpConnection {

    TcpConnection receive(Action1<ByteBuffer> buffer);

    TcpConnection exception(Action1<Throwable> exception);

    TcpConnection write(ByteBuffer byteBuffer, Action0 succeeded, Action1<Throwable> failed);

    TcpConnection write(ByteBuffer[] byteBuffer, Action0 succeeded, Action1<Throwable> failed);

    TcpConnection write(Collection<ByteBuffer> byteBuffer, Action0 succeeded, Action1<Throwable> failed);

    TcpConnection write(String message, Action0 succeeded, Action1<Throwable> failed);

    TcpConnection write(String message, String charset, Action0 succeeded, Action1<Throwable> failed);

    TcpConnection write(FileRegion file, Action0 succeeded, Action1<Throwable> failed);

    TcpConnection write(ByteBuffer byteBuffer, Action0 succeeded);

    TcpConnection write(ByteBuffer[] byteBuffer, Action0 succeeded);

    TcpConnection write(Collection<ByteBuffer> byteBuffer, Action0 succeeded);

    TcpConnection write(String message, Action0 succeeded);

    TcpConnection write(String message, String charset, Action0 succeeded);

    TcpConnection write(FileRegion file, Action0 succeeded);

    TcpConnection write(ByteBuffer byteBuffer);

    TcpConnection write(ByteBuffer[] byteBuffer);

    TcpConnection write(Collection<ByteBuffer> byteBuffer);

    TcpConnection write(String message);

    TcpConnection write(String message, String charset);

    TcpConnection write(FileRegion file);

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

    TcpConnection close(Action0 closeCallback);

    void close();

    void closeNow();

    void shutdownOutput();

    void shutdownInput();

    int getState();

    boolean isOpen();

    InetSocketAddress getLocalAddress();

    InetSocketAddress getRemoteAddress();

    long getIdleTimeout();

}
