package com.firefly.net.tcp;

import com.firefly.net.Connection;
import com.firefly.net.Session;
import com.firefly.net.buffer.FileRegion;
import com.firefly.utils.function.Action0;
import com.firefly.utils.function.Action1;

import java.nio.ByteBuffer;
import java.util.Collection;
import java.util.concurrent.CompletableFuture;

public interface TcpConnection extends Connection {

    TcpConnection receive(Action1<ByteBuffer> buffer);

    CompletableFuture<Void> writeAndWait(ByteBuffer byteBuffer);

    CompletableFuture<Void> writeAndWait(ByteBuffer[] byteBuffer);

    CompletableFuture<Void> writeAndWait(Collection<ByteBuffer> byteBuffer);

    CompletableFuture<Void> writeAndWait(String message);

    CompletableFuture<Void> writeAndWait(String message, String charset);

    CompletableFuture<Void> writeAndWait(FileRegion file);

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

    TcpConnection close(Action0 closeCallback);

    TcpConnection exception(Action1<Throwable> exception);

    void closeNow();

    void shutdownOutput();

    void shutdownInput();

    Session.State getState();

}
