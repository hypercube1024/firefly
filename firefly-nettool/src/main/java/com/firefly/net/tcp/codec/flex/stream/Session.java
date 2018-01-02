package com.firefly.net.tcp.codec.flex.stream;

import com.firefly.net.tcp.codec.flex.protocol.ControlFrame;
import com.firefly.net.tcp.codec.flex.protocol.DisconnectionFrame;
import com.firefly.net.tcp.codec.flex.protocol.Frame;
import com.firefly.net.tcp.codec.flex.protocol.PingFrame;
import com.firefly.utils.concurrent.Callback;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * @author Pengtao Qiu
 */
public interface Session extends ContextAttribute {

    Stream getStream(int streamId);

    Map<Integer, Stream> getAllStreams();

    /**
     * Create a new stream and send the first control frame to the remote endpoint.
     *
     * @param controlFrame The first control frame of the stream.
     * @param callback     The callback of sending the first control frame
     * @param listener     The stream listener.
     * @return The new stream.
     */
    Stream newStream(ControlFrame controlFrame, Callback callback, Stream.Listener listener);

    void setListener(Listener listener);

    CompletableFuture<Boolean> ping(PingFrame pingFrame);

    CompletableFuture<Boolean> disconnect(DisconnectionFrame disconnectionFrame);

    CompletableFuture<Boolean> sendFrame(Frame frame);

    CompletableFuture<Boolean> sendFrames(List<Frame> frames);

    void sendFrame(Frame frame, Callback callback);

    void sendFrames(List<Frame> frames, Callback callback);

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
