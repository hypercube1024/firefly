package com.firefly.net.tcp;

import com.firefly.net.ApplicationProtocolSelector;
import com.firefly.net.Connection;
import com.firefly.net.Session;
import com.firefly.net.buffer.FileRegion;
import com.firefly.utils.function.Action0;
import com.firefly.utils.function.Action1;

import java.nio.ByteBuffer;
import java.util.Collection;
import java.util.concurrent.CompletableFuture;

public interface TcpConnection extends Connection, ApplicationProtocolSelector {

    TcpConnection receive(Action1<ByteBuffer> buffer);

    CompletableFuture<Boolean> writeToFuture(ByteBuffer byteBuffer);

    CompletableFuture<Boolean> writeToFuture(ByteBuffer[] byteBuffer);

    CompletableFuture<Boolean> writeToFuture(Collection<ByteBuffer> byteBuffer);

    CompletableFuture<Boolean> writeToFuture(String message);

    CompletableFuture<Boolean> writeToFuture(String message, String charset);

    CompletableFuture<Boolean> writeToFuture(FileRegion file);

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

    TcpConnection onClose(Action0 closeCallback);

    TcpConnection onException(Action1<Throwable> exception);

    void closeNow();

    void shutdownOutput();

    void shutdownInput();

    boolean isShutdownOutput();

    boolean isShutdownInput();

    boolean isWaitingForClose();

    boolean isSecureConnection();

}
