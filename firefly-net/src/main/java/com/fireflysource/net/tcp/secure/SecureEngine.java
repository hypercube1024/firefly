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

    ByteBuffer decode(ByteBuffer byteBuffer);

    ByteBuffer encode(ByteBuffer byteBuffer);

}
