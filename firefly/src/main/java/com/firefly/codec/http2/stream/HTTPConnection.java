package com.firefly.codec.http2.stream;

import com.firefly.codec.http2.model.HttpVersion;

import java.io.Closeable;
import java.net.InetSocketAddress;

public interface HTTPConnection extends Closeable {

    HttpVersion getHttpVersion();

    Object getAttachment();

    void setAttachment(Object attachment);

    boolean isOpen();

    boolean isEncrypted();

    boolean isTunnel();

    void switchToTunnelConnection();

    boolean isReadyToSwitchTunnelConnection();

    HTTPTunnelConnection getHTTPTunnelConnection();

    int getSessionId();

    long getReadBytes();

    long getWrittenBytes();

    InetSocketAddress getLocalAddress();

    InetSocketAddress getRemoteAddress();

}