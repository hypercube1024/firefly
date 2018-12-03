package com.fireflysource.net.tcp;

import com.fireflysource.common.func.Callback;
import com.fireflysource.net.Connection;

import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

/**
 * @author Pengtao Qiu
 */
public interface TcpConnection extends Connection {

    ByteBuffer[] EMPTY = new ByteBuffer[0];
    Charset DEFAULT_CHARSET = StandardCharsets.UTF_8;

    TcpConnection onReceive(Consumer<ByteBuffer> data);

    TcpConnection onClose(Callback callback);

    TcpConnection onException(Consumer<Throwable> exception);

    CompletableFuture<Void> write(ByteBuffer byteBuffer);

    CompletableFuture<Void> write(ByteBuffer[] byteBuffers);

    default CompletableFuture<Void> write(Collection<ByteBuffer> byteBuffers) {
        return write(byteBuffers.toArray(EMPTY));
    }

    default CompletableFuture<Void> write(byte[] data) {
        return write(ByteBuffer.wrap(data));
    }

    default CompletableFuture<Void> write(String data) {
        return write(ByteBuffer.wrap(data.getBytes(DEFAULT_CHARSET)));
    }

}
