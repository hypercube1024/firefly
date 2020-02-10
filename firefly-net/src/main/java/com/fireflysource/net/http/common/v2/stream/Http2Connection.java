package com.fireflysource.net.http.common.v2.stream;

import com.fireflysource.common.sys.Result;
import com.fireflysource.net.http.common.HttpConnection;
import com.fireflysource.net.http.common.v2.frame.*;

import java.util.Collection;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

/**
 * <p>A {@link Http2Connection} represents the client-side endpoint of an HTTP/2 connection to a single origin server.</p>
 * <p>Once a {@link Http2Connection} has been obtained, it can be used to open HTTP/2 streams:</p>
 * <p>A {@link Http2Connection} is the active part of the endpoint, and by calling its API applications can generate
 * events on the connection; conversely {@link Http2Connection.Listener} is the passive part of the endpoint, and
 * has results that are invoked when events happen on the connection.</p>
 *
 * @see Http2Connection.Listener
 */
public interface Http2Connection extends HttpConnection {

    /**
     * <p>Sends the given HEADERS {@code frame} to create a new {@link Stream}.</p>
     *
     * @param frame    The HEADERS frame containing the HTTP headers
     * @param promise  The promise that gets notified of the stream creation
     * @param listener The listener that gets notified of stream events
     */
    void newStream(HeadersFrame frame, Consumer<Result<Stream>> promise, Stream.Listener listener);

    /**
     * <p>Sends the given HEADERS {@code frame} to create a new {@link Stream}.</p>
     *
     * @param frame    The HEADERS frame containing the HTTP headers.
     * @param listener The listener that gets notified of stream events.
     * @return The future which gets notified of the stream creation.
     */
    default CompletableFuture<Stream> newStream(HeadersFrame frame, Stream.Listener listener) {
        CompletableFuture<Stream> future = new CompletableFuture<>();
        newStream(frame, Result.futureToConsumer(future), listener);
        return future;
    }

    /**
     * <p>Sends the given PRIORITY {@code frame}.</p>
     * <p>If the {@code frame} references a {@code streamId} that does not exist
     * (for example {@code 0}), then a new {@code streamId} will be allocated, to
     * support <em>unused anchor streams</em> that act as parent for other streams.</p>
     *
     * @param frame  The PRIORITY frame to send
     * @param result The result that gets notified when the frame has been sent
     * @return The new stream id generated by the PRIORITY frame, or the stream id
     * that it is already referencing
     */
    int priority(PriorityFrame frame, Consumer<Result<Void>> result);

    /**
     * <p>Sends the given SETTINGS {@code frame} to configure the http2Connection.</p>
     *
     * @param frame  The SETTINGS frame to send
     * @param result The result that gets notified when the frame has been sent
     */
    void settings(SettingsFrame frame, Consumer<Result<Void>> result);

    /**
     * <p>Sends the given PING {@code frame}.</p>
     * <p>PING frames may use to test the connection integrity and to measure
     * round-trip time.</p>
     *
     * @param frame  The PING frame to send
     * @param result The result that gets notified when the frame has been sent
     */
    void ping(PingFrame frame, Consumer<Result<Void>> result);

    /**
     * <p>Closes the http2Connection by sending a GOAWAY frame with the given error code
     * and payload.</p>
     * <p>The GOAWAY frame is sent only once; subsequent or concurrent attempts to
     * close the http2Connection will have no effect.</p>
     *
     * @param error   The error code
     * @param payload An optional payload (maybe null)
     * @param result  The result that gets notified when the frame has been sent
     * @return True if the frame sent, false if the http2Connection was already closed
     */
    boolean close(int error, String payload, Consumer<Result<Void>> result);

    /**
     * @return Whether the http2Connection is not open
     */
    boolean isClosed();

    /**
     * @return A snapshot of all the streams currently belonging to this http2Connection
     */
    Collection<Stream> getStreams();

    /**
     * <p>Retrieves the stream with the given {@code streamId}.</p>
     *
     * @param streamId The stream id of the stream looked for
     * @return The stream with the given id, or null if no such stream exist
     */
    Stream getStream(int streamId);

    /**
     * <p>A {@link Listener} is the passive counterpart of a {@link Http2Connection} and
     * receives events happening on an HTTP/2 connection.</p>
     *
     * @see Http2Connection
     */
    interface Listener {
        /**
         * <p>Consumer<Result<Void>> method invoked:</p>
         * <ul>
         * <li>for clients, just before the preface sent, to gather the
         * SETTINGS configuration options the client wants to send to the server;</li>
         * <li>for servers, just after having received the preface, to gather
         * the SETTINGS configuration options the server wants to send to the
         * client.</li>
         * </ul>
         *
         * @param http2Connection The http2Connection
         * @return A (possibly empty or null) map containing SETTINGS configuration
         * options to send.
         */
        Map<Integer, Integer> onPreface(Http2Connection http2Connection);

        /**
         * <p>Consumer<Result<Void>> method invoked when a new stream created upon
         * receiving a HEADERS frame representing an HTTP request.</p>
         * <p>Applications should implement this method to process HTTP requests,
         * typically providing an HTTP response via
         * {@link Stream#headers(HeadersFrame, Consumer<Result<Void>>)}.</p>
         * <p>Applications can detect whether request DATA frames will be arriving
         * by testing {@link HeadersFrame#isEndStream()}. If the application is
         * interested in processing the DATA frames, it must return a
         * {@link Stream.Listener} implementation that overrides
         * {@link Stream.Listener#onData(Stream, DataFrame, Consumer<Result<Void>>)}.</p>
         *
         * @param stream The newly created stream
         * @param frame  The HEADERS frame received
         * @return A {@link Stream.Listener} that will be notified of stream events
         */
        Stream.Listener onNewStream(Stream stream, HeadersFrame frame);

        /**
         * <p>Consumer<Result<Void>> method invoked when a SETTINGS frame has been received.</p>
         *
         * @param http2Connection The http2Connection
         * @param frame           The SETTINGS frame received
         */
        void onSettings(Http2Connection http2Connection, SettingsFrame frame);

        /**
         * <p>Consumer<Result<Void>> method invoked when a PING frame has been received.</p>
         *
         * @param http2Connection The http2Connection
         * @param frame           The PING frame received
         */
        void onPing(Http2Connection http2Connection, PingFrame frame);

        /**
         * <p>Consumer<Result<Void>> method invoked when an RST_STREAM frame has been received for an unknown stream.</p>
         *
         * @param http2Connection The http2Connection
         * @param frame           The RST_STREAM frame received
         * @see Stream.Listener#onReset(Stream, ResetFrame)
         */
        void onReset(Http2Connection http2Connection, ResetFrame frame);

        /**
         * <p>Consumer<Result<Void>> method invoked when a GOAWAY frame has been received.</p>
         *
         * @param http2Connection The http2Connection
         * @param frame           The GOAWAY frame received
         * @param result          The result to notify of the GOAWAY processing
         */
        default void onClose(Http2Connection http2Connection, GoAwayFrame frame, Consumer<Result<Void>> result) {
            try {
                onClose(http2Connection, frame);
                result.accept(Result.SUCCESS);
            } catch (Throwable x) {
                result.accept(Result.createFailedResult(x));
            }
        }

        void onClose(Http2Connection http2Connection, GoAwayFrame frame);

        /**
         * <p>Consumer<Result<Void>> method invoked when the idle timeout expired.</p>
         *
         * @param http2Connection The http2Connection
         * @return Whether the http2Connection should be closed
         */
        boolean onIdleTimeout(Http2Connection http2Connection);

        /**
         * <p>Consumer<Result<Void>> method invoked when a failure has been detected for this http2Connection.</p>
         *
         * @param http2Connection The http2Connection
         * @param failure         The failure
         * @param result          The result to notify of failure processing
         */
        default void onFailure(Http2Connection http2Connection, Throwable failure, Consumer<Result<Void>> result) {
            try {
                onFailure(http2Connection, failure);
                result.accept(Result.SUCCESS);
            } catch (Throwable x) {
                result.accept(Result.createFailedResult(x));
            }
        }

        void onFailure(Http2Connection http2Connection, Throwable failure);

        /**
         * <p>Empty implementation of {@link Stream.Listener}.</p>
         */
        class Adapter implements Http2Connection.Listener {
            @Override
            public Map<Integer, Integer> onPreface(Http2Connection http2Connection) {
                return null;
            }

            @Override
            public Stream.Listener onNewStream(Stream stream, HeadersFrame frame) {
                return null;
            }

            @Override
            public void onSettings(Http2Connection http2Connection, SettingsFrame frame) {
            }

            @Override
            public void onPing(Http2Connection http2Connection, PingFrame frame) {
            }

            @Override
            public void onReset(Http2Connection http2Connection, ResetFrame frame) {
            }

            @Override
            public void onClose(Http2Connection http2Connection, GoAwayFrame frame) {
            }

            @Override
            public boolean onIdleTimeout(Http2Connection http2Connection) {
                return true;
            }

            @Override
            public void onFailure(Http2Connection http2Connection, Throwable failure) {
            }
        }
    }
}
