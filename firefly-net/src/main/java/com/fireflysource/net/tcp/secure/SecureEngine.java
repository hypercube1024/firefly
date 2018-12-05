package com.fireflysource.net.tcp.secure;

import com.fireflysource.net.tcp.Result;

import java.nio.ByteBuffer;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

import static com.fireflysource.net.tcp.Result.futureToConsumer;

/**
 * @author Pengtao Qiu
 */
public interface SecureEngine extends Cloneable, ApplicationProtocolSelector {

    void beginHandshake(Consumer<Result<SecureEngine>> result);

    default CompletableFuture<SecureEngine> beginHandshake() {
        CompletableFuture<SecureEngine> future = new CompletableFuture<>();
        beginHandshake(futureToConsumer(future));
        return future;
    }

    void readHandshakeMessage(ByteBuffer byteBuffer, Consumer<Result<Boolean>> result);

    default CompletableFuture<Boolean> readHandshakeMessage(ByteBuffer byteBuffer) {
        CompletableFuture<Boolean> future = new CompletableFuture<>();
        readHandshakeMessage(byteBuffer, futureToConsumer(future));
        return future;
    }

    CodecResult decode(ByteBuffer byteBuffer);

    CodecResult encode(ByteBuffer byteBuffer);

}
