package com.fireflysource.net.http.common.v2.stream;

import com.fireflysource.common.sys.Result;
import com.fireflysource.net.http.common.v2.frame.DataFrame;
import com.fireflysource.net.http.common.v2.frame.HeadersFrame;
import com.fireflysource.net.http.common.v2.frame.PushPromiseFrame;
import com.fireflysource.net.http.common.v2.frame.ResetFrame;

import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

/**
 * <p>A {@link Stream} represents a bidirectional exchange of data on top of a {@link Http2Connection}.</p>
 * <p>Differently from socket streams, where the input and output streams permanently associated
 * with the socket (and hence with the connection that the socket represents), there can be multiple
 * HTTP/2 streams present concurrent for an HTTP/2 session.</p>
 * <p>A {@link Stream} maps to an HTTP request/response cycle, and after the request/response cycle completed,
 * the stream closed and removed from the session.</p>
 * <p>Like {@link Http2Connection}, {@link Stream} is the active part and by calling its API applications
 * can generate events on the stream; conversely, {@link Stream.Listener} is the passive part, and
 * its results invoked when events happen on the stream.</p>
 *
 * @see Stream.Listener
 */
public interface Stream {
    /**
     * @return the stream's unique id
     */
    int getId();

    /**
     * @return the HTTP 2 connection this stream associated to
     */
    Http2Connection getHttp2Connection();

    /**
     * <p>Sends the given HEADERS {@code frame} representing an HTTP response.</p>
     *
     * @param frame  The HEADERS frame to send
     * @param result The result that gets notified when the frame has been sent
     */
    void headers(HeadersFrame frame, Consumer<Result<Void>> result);

    /**
     * <p>Sends the given PUSH_PROMISE {@code frame}.</p>
     *
     * @param frame    The PUSH_PROMISE frame to send
     * @param promise  The promise that gets notified of the pushed stream creation
     * @param listener The listener that gets notified of stream events
     */
    void push(PushPromiseFrame frame, Consumer<Result<Stream>> promise, Listener listener);

    /**
     * <p>Sends the given PUSH_PROMISE {@code frame}.</p>
     *
     * @param frame    he PUSH_PROMISE frame to send
     * @param listener The listener that gets notified of stream events
     * @return The future which gets notified of the pushed stream creation
     */
    default CompletableFuture<Stream> push(PushPromiseFrame frame, Listener listener) {
        CompletableFuture<Stream> future = new CompletableFuture<>();
        push(frame, Result.futureToConsumer(future), listener);
        return future;
    }

    /**
     * <p>Sends the given DATA {@code frame}.</p>
     *
     * @param frame  The DATA frame to send
     * @param result The result that gets notified when the frame has been sent
     */
    void data(DataFrame frame, Consumer<Result<Void>> result);

    /**
     * <p>Sends the given DATA {@code frame}.</p>
     *
     * @param frame The DATA frame to send
     * @return The result that gets notified when the frame has been sent
     */
    default CompletableFuture<Void> data(DataFrame frame) {
        CompletableFuture<Void> future = new CompletableFuture<>();
        data(frame, Result.futureToConsumer(future));
        return future;
    }

    /**
     * <p>Sends the given RST_STREAM {@code frame}.</p>
     *
     * @param frame  The RST_FRAME to send
     * @param result The result that gets notified when the frame has been sent
     */
    void reset(ResetFrame frame, Consumer<Result<Void>> result);

    /**
     * @param key the attribute key
     * @return An arbitrary object associated with the given key to this stream
     * or null if no object can be found for the given key.
     * @see #setAttribute(String, Object)
     */
    Object getAttribute(String key);

    /**
     * @param key   The attribute key
     * @param value An arbitrary object to associate with the given key to this stream
     * @see #getAttribute(String)
     * @see #removeAttribute(String)
     */
    void setAttribute(String key, Object value);

    /**
     * @param key The attribute key
     * @return The arbitrary object associated with the given key to this stream
     * @see #setAttribute(String, Object)
     */
    Object removeAttribute(String key);

    /**
     * @return If true this stream has been reset
     */
    boolean isReset();

    /**
     * @return If true this stream closed, both locally and remotely.
     */
    boolean isClosed();

    /**
     * @return The stream idle timeout
     * @see #setIdleTimeout(long)
     */
    long getIdleTimeout();

    /**
     * @param idleTimeout The stream idle timeout
     * @see #getIdleTimeout()
     * @see Stream.Listener#onIdleTimeout(Stream, Throwable)
     */
    void setIdleTimeout(long idleTimeout);

    /**
     * <p>A {@link Stream.Listener} is the passive counterpart of a {@link Stream} and receives
     * events happening on an HTTP/2 stream.</p>
     *
     * @see Stream
     */
    interface Listener {
        /**
         * <p>Callback method invoked when a HEADERS frame representing the HTTP response has been received.</p>
         *
         * @param stream The stream
         * @param frame  The HEADERS frame received
         */
        void onHeaders(Stream stream, HeadersFrame frame);

        /**
         * <p>Callback method invoked when a PUSH_PROMISE frame has been received.</p>
         *
         * @param stream The stream
         * @param frame  The PUSH_PROMISE frame received
         * @return A Stream.Listener that will be notified of pushed stream events
         */
        Listener onPush(Stream stream, PushPromiseFrame frame);

        /**
         * <p>Callback method invoked when a DATA frame has been received.</p>
         *
         * @param stream The stream
         * @param frame  The DATA frame received
         * @param result The result to complete when the bytes of the DATA frame have been consumed
         */
        void onData(Stream stream, DataFrame frame, Consumer<Result<Void>> result);

        /**
         * <p>Callback method invoked when an RST_STREAM frame has been received for this stream.</p>
         *
         * @param stream The stream
         * @param frame  The RST_FRAME received
         * @param result The result to complete when the reset has been handled
         */
        default void onReset(Stream stream, ResetFrame frame, Consumer<Result<Void>> result) {
            try {
                onReset(stream, frame);
                result.accept(Result.SUCCESS);
            } catch (Throwable x) {
                result.accept(Result.createFailedResult(x));
            }
        }

        /**
         * <p>Callback method invoked when an RST_STREAM frame has been received for this stream.</p>
         *
         * @param stream The stream
         * @param frame  The RST_FRAME received
         * @see Http2Connection.Listener#onReset(Http2Connection, ResetFrame)
         */
        default void onReset(Stream stream, ResetFrame frame) {
        }


        /**
         * <p>Callback method invoked when the stream exceeds its idle timeout.</p>
         *
         * @param stream The stream
         * @param x      The timeout failure
         * @return If true to reset the stream, false to ignore the idle timeout
         * @see #getIdleTimeout()
         */
        default boolean onIdleTimeout(Stream stream, Throwable x) {
            return true;
        }

        /**
         * <p>Callback method invoked when the stream failed.</p>
         *
         * @param stream The stream
         * @param error  The error code
         * @param reason The error reason, or null
         * @param result The result to complete when the failure has been handled
         */
        default void onFailure(Stream stream, int error, String reason, Consumer<Result<Void>> result) {
            result.accept(Result.SUCCESS);
        }

        /**
         * <p>Callback method invoked after the stream has been closed.</p>
         *
         * @param stream The stream
         */
        default void onClosed(Stream stream) {
        }

        /**
         * <p>Empty implementation of {@link Listener}</p>
         */
        class Adapter implements Listener {
            @Override
            public void onHeaders(Stream stream, HeadersFrame frame) {
            }

            @Override
            public Listener onPush(Stream stream, PushPromiseFrame frame) {
                return null;
            }

            @Override
            public void onData(Stream stream, DataFrame frame, Consumer<Result<Void>> result) {
                result.accept(Result.SUCCESS);
            }

            @Override
            public void onReset(Stream stream, ResetFrame frame) {
            }

            @Override
            public boolean onIdleTimeout(Stream stream, Throwable x) {
                return true;
            }
        }
    }

}
