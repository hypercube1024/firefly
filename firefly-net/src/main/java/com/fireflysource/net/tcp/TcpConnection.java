package com.fireflysource.net.tcp;

import com.fireflysource.common.func.Callback;
import com.fireflysource.net.Connection;
import com.fireflysource.net.tcp.secure.ApplicationProtocolSelector;
import com.fireflysource.net.tcp.secure.SecureEngine;
import kotlinx.coroutines.channels.Channel;

import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

import static com.fireflysource.net.tcp.Result.futureToConsumer;

/**
 * @author Pengtao Qiu
 */
public interface TcpConnection extends Connection, ApplicationProtocolSelector {

    Charset DEFAULT_CHARSET = StandardCharsets.UTF_8;

    /**
     * If you start automatic message reading, the net framework sends the received the data to the message consumer callback automatically.
     *
     * @param messageConsumer Received message.
     * @return The current tcp connection.
     */
    TcpConnection onRead(Consumer<ByteBuffer> messageConsumer);

    /**
     * If you start automatic message reading, the net framework sends the received the data to the message consumer callback automatically.
     * The default message consumer will send data to the channel.
     *
     * @return The receiving data channel
     */
    Channel<ByteBuffer> getInputChannel();

    /**
     * Start automatic message reading.
     *
     * @return The current tcp connection.
     */
    TcpConnection startReading();

    /**
     * If return true, the tcp connection has started automatic message reading.
     *
     * @return If return true, the tcp connection has started automatic message reading.
     */
    boolean isStartReading();

    TcpConnection onClose(Callback callback);

    TcpConnection onException(Consumer<Throwable> exception);

    TcpConnection close(Consumer<Result<Void>> result);

    TcpConnection closeNow();

    boolean isShutdownInput();

    boolean isShutdownOutput();

    TcpConnection write(ByteBuffer byteBuffer, Consumer<Result<Integer>> result);

    TcpConnection write(ByteBuffer[] byteBuffers, int offset, int length, Consumer<Result<Long>> result);

    TcpConnection write(List<ByteBuffer> byteBufferList, int offset, int length, Consumer<Result<Long>> result);

    default CompletableFuture<Integer> write(ByteBuffer byteBuffer) {
        CompletableFuture<Integer> future = new CompletableFuture<>();
        write(byteBuffer, futureToConsumer(future));
        return future;
    }

    default CompletableFuture<Long> write(ByteBuffer[] byteBuffers, int offset, int length) {
        CompletableFuture<Long> future = new CompletableFuture<>();
        write(byteBuffers, offset, length, futureToConsumer(future));
        return future;
    }

    default CompletableFuture<Long> write(List<ByteBuffer> byteBufferList, int offset, int length) {
        CompletableFuture<Long> future = new CompletableFuture<>();
        write(byteBufferList, offset, length, futureToConsumer(future));
        return future;
    }

    default TcpConnection write(byte[] data, Consumer<Result<Integer>> result) {
        return write(ByteBuffer.wrap(data), result);
    }

    default TcpConnection write(String data, Consumer<Result<Integer>> result) {
        return write(ByteBuffer.wrap(data.getBytes(DEFAULT_CHARSET)), result);
    }

    boolean isSecureConnection();

    boolean isClientMode();

    boolean isHandshakeFinished();

    void beginHandshake(Consumer<Result<Void>> result);

    default CompletableFuture<Void> beginHandshake() {
        CompletableFuture<Void> future = new CompletableFuture<>();
        beginHandshake(futureToConsumer(future));
        return future;
    }
}
