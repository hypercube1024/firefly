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
 * The TLS engine. It can encrypt or decrypt the message.
 *
 * @author Pengtao Qiu
 */
public interface SecureEngine extends Closeable, ApplicationProtocolSelector {

    /**
     * If return true, the TLS engine is client mode.
     *
     * @return If return true, the TLS engine is client mode.
     */
    boolean isClientMode();

    /**
     * If return true, the TLS handshake stage is finished.
     *
     * @return If return true, the TLS handshake stage is finished.
     */
    boolean isHandshakeFinished();

    /**
     * Begin the TLS handshake.
     *
     * @param result The handler for consuming TLS handshake result.
     */
    void beginHandshake(Consumer<Result<Void>> result);

    /**
     * Begin the TLS handshake.ø
     *
     * @return The future for consuming the TLS handshake result.
     */
    default CompletableFuture<Void> beginHandshake() {
        CompletableFuture<Void> future = new CompletableFuture<>();
        beginHandshake(futureToConsumer(future));
        return future;
    }

    /**
     * Decrypt the cipher text to the plain text.
     *
     * @param byteBuffer The cipher text data.
     * @return The cipher text byte buffer.
     * @throws IOException The exception of the encoding.
     */
    ByteBuffer decode(ByteBuffer byteBuffer) throws IOException;

    /**
     * Encrypt the plain text to the cipher text.
     *
     * @param byteBuffer The plain text data.
     * @return The cipher text byte buffer list.
     * @throws IOException The exception of the encoding.
     */
    List<ByteBuffer> encode(ByteBuffer byteBuffer) throws IOException;

}
