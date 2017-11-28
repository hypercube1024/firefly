package com.firefly.codec.http2.stream;

import com.firefly.codec.http2.model.HttpVersion;
import com.firefly.net.Connection;
import com.firefly.utils.function.Action1;
import com.firefly.utils.function.Action2;

import java.io.Closeable;
import java.net.InetSocketAddress;

public interface HTTPConnection extends Connection {

    HttpVersion getHttpVersion();

    boolean isEncrypted();

    ConnectionType getConnectionType();

    HTTPConnection close(Action1<HTTPConnection> closedCallback);

    HTTPConnection exception(Action2<HTTPConnection, Throwable> exceptionCallback);

}