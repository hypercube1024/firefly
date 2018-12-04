package com.fireflysource.net.tcp;

import com.fireflysource.common.func.Callback;
import com.fireflysource.net.Connection;
import com.fireflysource.net.tcp.secure.ApplicationProtocolSelector;

import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

/**
 * @author Pengtao Qiu
 */
public interface TcpConnection extends Connection, ApplicationProtocolSelector {

    ByteBuffer[] BYTE_BUFFERS = new ByteBuffer[0];
    Charset DEFAULT_CHARSET = StandardCharsets.UTF_8;

    TcpConnection onRead(Consumer<ByteBuffer> data);

    TcpConnection readAutomatically();

    TcpConnection onClose(Callback callback);

    TcpConnection onException(Consumer<Throwable> exception);

    TcpConnection write(ByteBuffer byteBuffer, Consumer<Result<Void>> result);

    TcpConnection write(ByteBuffer[] byteBuffers, Consumer<Result<Void>> result);

    default TcpConnection write(Collection<ByteBuffer> byteBuffers, Consumer<Result<Void>> result) {
        return write(byteBuffers.toArray(BYTE_BUFFERS), result);
    }

    default TcpConnection write(byte[] data, Consumer<Result<Void>> result) {
        return write(ByteBuffer.wrap(data), result);
    }

    default TcpConnection write(String data, Consumer<Result<Void>> result) {
        return write(ByteBuffer.wrap(data.getBytes(DEFAULT_CHARSET)), result);
    }

    CompletableFuture<Void> write(ByteBuffer byteBuffer);

    CompletableFuture<Void> write(ByteBuffer[] byteBuffers);

    default CompletableFuture<Void> write(Collection<ByteBuffer> byteBuffers) {
        return write(byteBuffers.toArray(BYTE_BUFFERS));
    }

    default CompletableFuture<Void> write(byte[] data) {
        return write(ByteBuffer.wrap(data));
    }

    default CompletableFuture<Void> write(String data) {
        return write(ByteBuffer.wrap(data.getBytes(DEFAULT_CHARSET)));
    }

    boolean isSecureConnection();

    boolean isClientMode();

    boolean isHandshakeFinished();
}
