package com.fireflysource.net.tcp.secure;

import com.fireflysource.net.tcp.Result;

import java.io.Closeable;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

import static com.fireflysource.net.tcp.Result.futureToConsumer;

/**
 * @author Pengtao Qiu
 */
public interface SecureEngine extends Closeable, ApplicationProtocolSelector {

    boolean isClientMode();

    boolean isHandshakeFinished();

    void beginHandshake(Consumer<Result<Void>> result);

    default CompletableFuture<Void> beginHandshake() {
        CompletableFuture<Void> future = new CompletableFuture<>();
        beginHandshake(futureToConsumer(future));
        return future;
    }

    ByteBuffer decode(ByteBuffer byteBuffer) throws IOException;

    List<ByteBuffer> encode(ByteBuffer byteBuffer) throws IOException;

}
