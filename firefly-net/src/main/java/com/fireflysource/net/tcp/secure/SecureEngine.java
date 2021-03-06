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
    void beginHandshake(Consumer<Result<HandshakeResult>> result);

    /**
     * Begin the TLS handshake.
     *
     * @return The future for consuming the TLS handshake result.
     */
    default CompletableFuture<HandshakeResult> beginHandshake() {
        CompletableFuture<HandshakeResult> future = new CompletableFuture<>();
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
    SecureEngine onHandshakeWrite(Function<ByteBuffer, CompletableFuture<Integer>> function);

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
     * @return The cipher text byte buffer.
     */
    ByteBuffer encrypt(ByteBuffer byteBuffer);

    /**
     * Encrypt the plain text to the cipher text.
     *
     * @param byteBuffers The plain text data.
     * @param offset      The offset within the buffer array of the first buffer into which
     *                    bytes are to be transferred; must be non-negative and no larger than
     *                    byteBuffers.length.
     * @param length      The maximum number of buffers to be accessed; must be non-negative
     *                    and no larger than byteBuffers.length - offset.
     * @return The cipher text byte buffer.
     */
    ByteBuffer encrypt(ByteBuffer[] byteBuffers, int offset, int length);

    /**
     * Encrypt the plain text to the cipher text.
     *
     * @param byteBuffers The plain text data.
     * @param offset      The offset within the buffer array of the first buffer into which
     *                    bytes are to be transferred; must be non-negative and no larger than
     *                    byteBuffers.length.
     * @param length      The maximum number of buffers to be accessed; must be non-negative
     *                    and no larger than byteBuffers.length - offset.
     * @return The cipher text byte buffer.
     */
    ByteBuffer encrypt(List<ByteBuffer> byteBuffers, int offset, int length);

}
