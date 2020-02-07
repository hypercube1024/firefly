package com.fireflysource.net.tcp.secure;

import com.fireflysource.common.sys.Result;

import java.io.Closeable;
import java.nio.ByteBuffer;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

import static com.fireflysource.common.sys.Result.futureToConsumer;

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
     * If return true, the TLS handshake stage is complete.
     *
     * @return If return true, the TLS handshake stage is complete.
     */
    boolean isHandshakeComplete();

    /**
     * Begin the TLS handshake.
     *
     * @param result The TLS handshake result.
     */
    void beginHandshake(Consumer<Result<Void>> result);

    /**
     * Begin the TLS handshake.
     *
     * @return The future for consuming the TLS handshake result.
     */
    default CompletableFuture<Void> beginHandshake() {
        CompletableFuture<Void> future = new CompletableFuture<>();
        beginHandshake(futureToConsumer(future));
        return future;
    }

    /**
     * Need read data in the handshake process.
     *
     * @param supplier The data supplier.
     * @return The secure engine.
     */
    SecureEngine onHandshakeRead(Supplier<CompletableFuture<ByteBuffer>> supplier);

    /**
     * Need write data in the handshake process.
     *
     * @param function The write function.
     * @return The secure engine.
     */
    SecureEngine onHandshakeWrite(Function<List<ByteBuffer>, CompletableFuture<Long>> function);

    /**
     * Decrypt the cipher text to the plain text.
     *
     * @param byteBuffer The cipher text data.
     * @return The cipher text byte buffer.
     */
    ByteBuffer decrypt(ByteBuffer byteBuffer);

    /**
     * Encrypt the plain text to the cipher text.
     *
     * @param byteBuffer The plain text data.
     * @return The cipher text byte buffer list.
     */
    List<ByteBuffer> encrypt(ByteBuffer byteBuffer);

    List<ByteBuffer> encrypt(ByteBuffer[] byteBuffers, int offset, int length);

    List<ByteBuffer> encrypt(List<ByteBuffer> byteBuffers, int offset, int length);

}
