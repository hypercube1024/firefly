package com.firefly.net.tcp.codec.stream;

import com.firefly.net.tcp.codec.protocol.ControlFrame;
import com.firefly.net.tcp.codec.protocol.DisconnectionFrame;
import com.firefly.net.tcp.codec.protocol.PingFrame;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * @author Pengtao Qiu
 */
public interface Session {

    Stream getStream(int streamId);

    Map<Integer, Stream> getAllStreams();

    /**
     * Create a new stream and send the first control frame to the remote endpoint.
     *
     * @param controlFrame The first control frame of the stream.
     * @param listener     The stream listener.
     * @return If the control frame send completely, it returns the new stream.
     */
    CompletableFuture<Stream> newStream(ControlFrame controlFrame, Stream.Listener listener);

    void setListener(Listener listener);

    CompletableFuture<Boolean> ping(PingFrame pingFrame);

    CompletableFuture<Boolean> disconnect(DisconnectionFrame disconnectionFrame);

    Map<String, Object> getAttibutes();

    void setAttribute(String key, Object value);

    Object getAttribute(String key);

    interface Listener {

        /**
         * Receive a new stream that is created by remote endpoint.
         *
         * @param stream       The new stream that is created by remote endpoint.
         * @param controlFrame The first control frame of the stream.
         * @return The stream listener.
         */
        Stream.Listener onNewStream(Stream stream, ControlFrame controlFrame);

        void onPing(Session session, PingFrame pingFrame);

        void onDisconnect(Session session, DisconnectionFrame disconnectionFrame);
    }
}
