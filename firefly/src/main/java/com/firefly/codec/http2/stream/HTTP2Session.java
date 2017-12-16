package com.firefly.codec.http2.stream;

import com.firefly.codec.http2.decode.Parser;
import com.firefly.codec.http2.encode.Generator;
import com.firefly.codec.http2.frame.*;
import com.firefly.utils.concurrent.*;
import com.firefly.utils.io.BufferUtils;
import com.firefly.utils.lang.Pair;
import com.firefly.utils.time.Millisecond100Clock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.ClosedChannelException;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

public abstract class HTTP2Session implements SessionSPI, Parser.Listener {

    private static final Logger log = LoggerFactory.getLogger("firefly-system");

    private final ConcurrentMap<Integer, StreamSPI> streams = new ConcurrentHashMap<>();
    private final AtomicInteger streamIds = new AtomicInteger();
    private final AtomicInteger lastStreamId = new AtomicInteger();
    private final AtomicInteger localStreamCount = new AtomicInteger();
    private final AtomicInteger remoteStreamCount = new AtomicInteger();
    private final AtomicInteger sendWindow = new AtomicInteger();
    private final AtomicInteger recvWindow = new AtomicInteger();
    private final AtomicReference<CloseState> closed = new AtomicReference<>(CloseState.NOT_CLOSED);
    private final AtomicLong bytesWritten = new AtomicLong();
    private final Scheduler scheduler;
    private final com.firefly.net.Session endPoint;
    private final Generator generator;
    private final Session.Listener listener;
    private final FlowControlStrategy flowControl;
    private final HTTP2Flusher flusher;
    private int maxLocalStreams;
    private int maxRemoteStreams;
    private long streamIdleTimeout;
    private int initialSessionRecvWindow;
    private boolean pushEnabled;
    private long idleTime;

    public HTTP2Session(Scheduler scheduler, com.firefly.net.Session endPoint, Generator generator,
                        Session.Listener listener, FlowControlStrategy flowControl,
                        int initialStreamId, int streamIdleTimeout) {
        this.scheduler = scheduler;
        this.endPoint = endPoint;
        this.generator = generator;
        this.listener = listener;
        this.flowControl = flowControl;
        this.flusher = new HTTP2Flusher(this);
        this.maxLocalStreams = -1;
        this.maxRemoteStreams = -1;
        this.streamIds.set(initialStreamId);
        this.streamIdleTimeout = streamIdleTimeout > 0 ? streamIdleTimeout : endPoint.getMaxIdleTimeout();
        this.sendWindow.set(FlowControlStrategy.DEFAULT_WINDOW_SIZE);
        this.recvWindow.set(FlowControlStrategy.DEFAULT_WINDOW_SIZE);
        this.pushEnabled = true; // SPEC: by default, push is enabled.
        this.idleTime = Millisecond100Clock.currentTimeMillis();
    }

    public FlowControlStrategy getFlowControlStrategy() {
        return flowControl;
    }

    public int getMaxLocalStreams() {
        return maxLocalStreams;
    }

    public void setMaxLocalStreams(int maxLocalStreams) {
        this.maxLocalStreams = maxLocalStreams;
    }

    public int getMaxRemoteStreams() {
        return maxRemoteStreams;
    }

    public void setMaxRemoteStreams(int maxRemoteStreams) {
        this.maxRemoteStreams = maxRemoteStreams;
    }

    public long getStreamIdleTimeout() {
        return streamIdleTimeout;
    }

    public void setStreamIdleTimeout(long streamIdleTimeout) {
        this.streamIdleTimeout = streamIdleTimeout;
    }

    public int getInitialSessionRecvWindow() {
        return initialSessionRecvWindow;
    }

    public void setInitialSessionRecvWindow(int initialSessionRecvWindow) {
        this.initialSessionRecvWindow = initialSessionRecvWindow;
    }

    public com.firefly.net.Session getEndPoint() {
        return endPoint;
    }

    public Generator getGenerator() {
        return generator;
    }

    @Override
    public long getBytesWritten() {
        return bytesWritten.get();
    }

    @Override
    public void onData(final DataFrame frame) {
        if (log.isDebugEnabled())
            log.debug("Received {}", frame);

        int streamId = frame.getStreamId();
        final StreamSPI stream = getStream(streamId);

        // SPEC: the session window must be updated even if the stream is null.
        // The flow control length includes the padding bytes.
        final int flowControlLength = frame.remaining() + frame.padding();
        flowControl.onDataReceived(this, stream, flowControlLength);

        if (stream != null) {
            if (getRecvWindow() < 0) {
                close(ErrorCode.FLOW_CONTROL_ERROR.code, "session_window_exceeded", Callback.NOOP);
            } else {
                stream.process(frame, new Callback() {
                    @Override
                    public void succeeded() {
                        complete();
                    }

                    @Override
                    public void failed(Throwable x) {
                        // Consume also in case of failures, to free the
                        // session flow control window for other streams.
                        complete();
                    }

                    private void complete() {
                        notIdle();
                        stream.notIdle();
                        flowControl.onDataConsumed(HTTP2Session.this, stream, flowControlLength);
                    }
                });
            }
        } else {
            if (log.isDebugEnabled())
                log.debug("Ignoring {}, stream #{} not found", frame, streamId);
            // We must enlarge the session flow control window,
            // otherwise other requests will be stalled.
            flowControl.onDataConsumed(this, null, flowControlLength);
        }
    }

    @Override
    public abstract void onHeaders(HeadersFrame frame);

    @Override
    public void onPriority(PriorityFrame frame) {
        if (log.isDebugEnabled())
            log.debug("Received {}", frame);
    }

    @Override
    public void onReset(ResetFrame frame) {
        if (log.isDebugEnabled())
            log.debug("Received {}", frame);

        StreamSPI stream = getStream(frame.getStreamId());
        if (stream != null)
            stream.process(frame, new ResetCallback());
        else
            notifyReset(this, frame);
    }

    @Override
    public void onSettings(SettingsFrame frame) {
        // SPEC: SETTINGS frame MUST be replied.
        onSettings(frame, true);
    }

    public void onSettings(SettingsFrame frame, boolean reply) {
        if (log.isDebugEnabled())
            log.debug("Received {}", frame);

        if (frame.isReply())
            return;

        // Iterate over all settings
        for (Map.Entry<Integer, Integer> entry : frame.getSettings().entrySet()) {
            int key = entry.getKey();
            int value = entry.getValue();
            switch (key) {
                case SettingsFrame.HEADER_TABLE_SIZE: {
                    if (log.isDebugEnabled())
                        log.debug("Update HPACK header table size to {} for {}", value, this);
                    generator.setHeaderTableSize(value);
                    break;
                }
                case SettingsFrame.ENABLE_PUSH: {
                    // SPEC: check the value is sane.
                    if (value != 0 && value != 1) {
                        onConnectionFailure(ErrorCode.PROTOCOL_ERROR.code, "invalid_settings_enable_push");
                        return;
                    }
                    pushEnabled = value == 1;
                    if (log.isDebugEnabled())
                        log.debug("{} push for {}", pushEnabled ? "Enable" : "Disable", this);
                    break;
                }
                case SettingsFrame.MAX_CONCURRENT_STREAMS: {
                    maxLocalStreams = value;
                    if (log.isDebugEnabled())
                        log.debug("Update max local concurrent streams to {} for {}", maxLocalStreams, this);
                    break;
                }
                case SettingsFrame.INITIAL_WINDOW_SIZE: {
                    if (log.isDebugEnabled())
                        log.debug("Update initial window size to {} for {}", value, this);
                    flowControl.updateInitialStreamWindow(this, value, false);
                    break;
                }
                case SettingsFrame.MAX_FRAME_SIZE: {
                    if (log.isDebugEnabled())
                        log.debug("Update max frame size to {} for {}", value, this);
                    // SPEC: check the max frame size is sane.
                    if (value < Frame.DEFAULT_MAX_LENGTH || value > Frame.MAX_MAX_LENGTH) {
                        onConnectionFailure(ErrorCode.PROTOCOL_ERROR.code, "invalid_settings_max_frame_size");
                        return;
                    }
                    generator.setMaxFrameSize(value);
                    break;
                }
                case SettingsFrame.MAX_HEADER_LIST_SIZE: {
                    if (log.isDebugEnabled())
                        log.debug("Update max header list size to {} for {}", value, this);
                    generator.setMaxHeaderListSize(value);
                    break;
                }
                default: {
                    if (log.isDebugEnabled())
                        log.debug("Unknown setting {}:{} for {}", key, value, this);
                    break;
                }
            }
        }
        notifySettings(this, frame);

        if (reply) {
            SettingsFrame replyFrame = new SettingsFrame(Collections.emptyMap(), true);
            settings(replyFrame, Callback.NOOP);
        }
    }

    @Override
    public void onPing(PingFrame frame) {
        if (log.isDebugEnabled())
            log.debug("Received {}", frame);

        if (frame.isReply()) {
            notifyPing(this, frame);
        } else {
            PingFrame reply = new PingFrame(frame.getPayload(), true);
            control(null, Callback.NOOP, reply);
        }
    }

    /**
     * This method is called when receiving a GO_AWAY from the other peer.
     * We check the close state to act appropriately:
     * <ul>
     * <li>NOT_CLOSED: we move to REMOTELY_CLOSED and queue a disconnect, so
     * that the content of the queue is written, and then the connection
     * closed. We notify the application after being terminated.
     * See <code>HTTP2Session.ControlEntry#succeeded()</code></li>
     * <li>In all other cases, we do nothing since other methods are already
     * performing their actions.</li>
     * </ul>
     *
     * @param frame the GO_AWAY frame that has been received.
     * @see #close(int, String, Callback)
     * @see #onShutdown()
     * @see #onIdleTimeout()
     */
    @Override
    public void onGoAway(final GoAwayFrame frame) {
        if (log.isDebugEnabled())
            log.debug("Received {}", frame);

        while (true) {
            CloseState current = closed.get();
            switch (current) {
                case NOT_CLOSED: {
                    if (closed.compareAndSet(current, CloseState.REMOTELY_CLOSED)) {
                        // We received a GO_AWAY, so try to write
                        // what's in the queue and then disconnect.
                        notifyClose(this, frame, new DisconnectCallback());
                        return;
                    }
                    break;
                }
                default: {
                    if (log.isDebugEnabled())
                        log.debug("Ignored {}, already closed", frame);
                    return;
                }
            }
        }
    }

    @Override
    public void onWindowUpdate(WindowUpdateFrame frame) {
        if (log.isDebugEnabled())
            log.debug("Received {}", frame);

        int streamId = frame.getStreamId();
        if (streamId > 0) {
            StreamSPI stream = getStream(streamId);
            if (stream != null) {
                stream.process(frame, Callback.NOOP);
                onWindowUpdate(stream, frame);
            }
        } else {
            onWindowUpdate(null, frame);
        }
    }

    @Override
    public void onConnectionFailure(int error, String reason) {
        notifyFailure(this, new IOException(String.format("%d/%s", error, reason)), new CloseCallback(error, reason));
    }

    @Override
    public void newStream(HeadersFrame frame, Promise<Stream> promise, Stream.Listener listener) {
        // Synchronization is necessary to atomically create
        // the stream id and enqueue the frame to be sent.
        boolean queued;
        synchronized (this) {
            int streamId = frame.getStreamId();
            if (streamId <= 0) {
                streamId = streamIds.getAndAdd(2);
                PriorityFrame priority = frame.getPriority();
                priority = priority == null ? null : new PriorityFrame(streamId, priority.getParentStreamId(),
                        priority.getWeight(), priority.isExclusive());
                frame = new HeadersFrame(streamId, frame.getMetaData(), priority, frame.isEndStream());
            }
            final StreamSPI stream = createLocalStream(streamId, promise);
            if (stream == null)
                return;
            stream.setListener(listener);

            ControlEntry entry = new ControlEntry(frame, stream, new PromiseCallback<>(promise, stream));
            queued = flusher.append(entry);
        }
        // Iterate outside the synchronized block.
        if (queued)
            flusher.iterate();
    }

    @Override
    public int priority(PriorityFrame frame, Callback callback) {
        int streamId = frame.getStreamId();
        StreamSPI stream = streams.get(streamId);
        if (stream == null) {
            streamId = streamIds.getAndAdd(2);
            frame = new PriorityFrame(streamId, frame.getParentStreamId(),
                    frame.getWeight(), frame.isExclusive());
        }
        control(stream, callback, frame);
        return streamId;
    }

    @Override
    public void push(StreamSPI stream, Promise<Stream> promise, PushPromiseFrame frame, Stream.Listener listener) {
        // Synchronization is necessary to atomically create
        // the stream id and enqueue the frame to be sent.
        boolean queued;
        synchronized (this) {
            int streamId = streamIds.getAndAdd(2);
            frame = new PushPromiseFrame(frame.getStreamId(), streamId, frame.getMetaData());

            final StreamSPI pushStream = createLocalStream(streamId, promise);
            if (pushStream == null)
                return;
            pushStream.setListener(listener);

            ControlEntry entry = new ControlEntry(frame, pushStream, new PromiseCallback<>(promise, pushStream));
            queued = flusher.append(entry);
        }
        // Iterate outside the synchronized block.
        if (queued)
            flusher.iterate();
    }

    @Override
    public void settings(SettingsFrame frame, Callback callback) {
        control(null, callback, frame);
    }

    @Override
    public void ping(PingFrame frame, Callback callback) {
        if (frame.isReply())
            callback.failed(new IllegalArgumentException());
        else
            control(null, callback, frame);
    }

    protected void reset(ResetFrame frame, Callback callback) {
        control(getStream(frame.getStreamId()), callback, frame);
    }

    /**
     * Invoked internally and by applications to send a GO_AWAY frame to the
     * other peer. We check the close state to act appropriately:
     * <ul>
     * <li>NOT_CLOSED: we move to LOCALLY_CLOSED and queue a GO_AWAY. When the
     * GO_AWAY has been written, it will only cause the output to be shut
     * down (not the connection closed), so that the application can still
     * read frames arriving from the other peer.
     * Ideally the other peer will notice the GO_AWAY and close the connection.
     * When that happen, we close the connection from {@link #onShutdown()}.
     * Otherwise, the idle timeout mechanism will close the connection, see
     * {@link #onIdleTimeout()}.</li>
     * <li>In all other cases, we do nothing since other methods are already
     * performing their actions.</li>
     * </ul>
     *
     * @param error    the error code
     * @param reason   the reason
     * @param callback the callback to invoke when the operation is complete
     * @see #onGoAway(GoAwayFrame)
     * @see #onShutdown()
     * @see #onIdleTimeout()
     */
    @Override
    public boolean close(int error, String reason, Callback callback) {
        while (true) {
            CloseState current = closed.get();
            switch (current) {
                case NOT_CLOSED: {
                    if (closed.compareAndSet(current, CloseState.LOCALLY_CLOSED)) {
                        byte[] payload = null;
                        if (reason != null) {
                            // Trim the reason to avoid attack vectors.
                            reason = reason.substring(0, Math.min(reason.length(), 32));
                            payload = reason.getBytes(StandardCharsets.UTF_8);
                        }
                        GoAwayFrame frame = new GoAwayFrame(lastStreamId.get(), error, payload);
                        control(null, callback, frame);
                        return true;
                    }
                    break;
                }
                default: {
                    if (log.isDebugEnabled())
                        log.debug("Ignoring close {}/{}, already closed", error, reason);
                    callback.succeeded();
                    return false;
                }
            }
        }
    }

    @Override
    public boolean isClosed() {
        return closed.get() != CloseState.NOT_CLOSED;
    }

    private void control(StreamSPI stream, Callback callback, Frame frame) {
        frames(stream, callback, frame, Frame.EMPTY_ARRAY);
    }

    @Override
    public void frames(StreamSPI stream, Callback callback, Frame frame, Frame... frames) {
        // We want to generate as late as possible to allow re-prioritization;
        // generation will happen while processing the entries.

        // The callback needs to be notified only when the last frame completes.

        int length = frames.length;
        if (length == 0) {
            frame(new ControlEntry(frame, stream, callback), true);
        } else {
            callback = new CountingCallback(callback, 1 + length);
            frame(new ControlEntry(frame, stream, callback), false);
            for (int i = 1; i <= length; ++i)
                frame(new ControlEntry(frames[i - 1], stream, callback), i == length);
        }
    }

    @Override
    public void data(StreamSPI stream, Callback callback, DataFrame frame) {
        // We want to generate as late as possible to allow re-prioritization.
        frame(new DataEntry(frame, stream, callback), true);
    }

    private void frame(HTTP2Flusher.Entry entry, boolean flush) {
        if (log.isDebugEnabled())
            log.debug("{} {}", flush ? "Sending" : "Queueing", entry.frame);
        // Ping frames are prepended to process them as soon as possible.
        boolean queued = entry.frame.getType() == FrameType.PING ? flusher.prepend(entry) : flusher.append(entry);
        if (queued && flush) {
            if (entry.stream != null)
                entry.stream.notIdle();
            flusher.iterate();
        }
    }

    protected StreamSPI createLocalStream(int streamId, Promise<Stream> promise) {
        while (true) {
            int localCount = localStreamCount.get();
            int maxCount = getMaxLocalStreams();
            if (maxCount >= 0 && localCount >= maxCount) {
                promise.failed(new IllegalStateException("Max local stream count " + maxCount + " exceeded"));
                return null;
            }
            if (localStreamCount.compareAndSet(localCount, localCount + 1))
                break;
        }

        StreamSPI stream = newStream(streamId, true);
        if (streams.putIfAbsent(streamId, stream) == null) {
            stream.setIdleTimeout(getStreamIdleTimeout());
            flowControl.onStreamCreated(stream);
            if (log.isDebugEnabled())
                log.debug("Created local {}", stream);
            return stream;
        } else {
            promise.failed(new IllegalStateException("Duplicate stream " + streamId));
            return null;
        }
    }

    protected StreamSPI createRemoteStream(int streamId) {
        // SPEC: exceeding max concurrent streams is treated as stream error.
        while (true) {
            int remoteCount = remoteStreamCount.get();
            int maxCount = getMaxRemoteStreams();
            if (maxCount >= 0 && remoteCount >= maxCount) {
                reset(new ResetFrame(streamId, ErrorCode.REFUSED_STREAM_ERROR.code), Callback.NOOP);
                return null;
            }
            if (remoteStreamCount.compareAndSet(remoteCount, remoteCount + 1))
                break;
        }

        StreamSPI stream = newStream(streamId, false);

        // SPEC: duplicate stream is treated as connection error.
        if (streams.putIfAbsent(streamId, stream) == null) {
            updateLastStreamId(streamId);
            stream.setIdleTimeout(getStreamIdleTimeout());
            flowControl.onStreamCreated(stream);
            if (log.isDebugEnabled())
                log.debug("Created remote {}", stream);
            return stream;
        } else {
            close(ErrorCode.PROTOCOL_ERROR.code, "duplicate_stream", Callback.NOOP);
            return null;
        }
    }

    protected StreamSPI newStream(int streamId, boolean local) {
        return new HTTP2Stream(scheduler, this, streamId, local);
    }

    @Override
    public void removeStream(StreamSPI stream) {
        StreamSPI removed = streams.remove(stream.getId());
        if (removed != null) {
            boolean local = stream.isLocal();
            if (local)
                localStreamCount.decrementAndGet();
            else
                remoteStreamCount.decrementAndGet();

            onStreamClosed(stream);

            flowControl.onStreamDestroyed(stream);

            if (log.isDebugEnabled())
                log.debug("Removed {} {}", local ? "local" : "remote", stream);
        }
    }

    @Override
    public Collection<Stream> getStreams() {
        List<Stream> result = new ArrayList<>();
        result.addAll(streams.values());
        return result;
    }

    public int getStreamCount() {
        return streams.size();
    }

    @Override
    public StreamSPI getStream(int streamId) {
        return streams.get(streamId);
    }

    public int getSendWindow() {
        return sendWindow.get();
    }

    public int getRecvWindow() {
        return recvWindow.get();
    }

    @Override
    public int updateSendWindow(int delta) {
        return sendWindow.getAndAdd(delta);
    }

    @Override
    public int updateRecvWindow(int delta) {
        return recvWindow.getAndAdd(delta);
    }

    @Override
    public void onWindowUpdate(StreamSPI stream, WindowUpdateFrame frame) {
        // WindowUpdateFrames arrive concurrently with writes.
        // Increasing (or reducing) the window size concurrently
        // with writes requires coordination with the flusher, that
        // decides how many frames to write depending on the available
        // window sizes. If the window sizes vary concurrently, the
        // flusher may take non-optimal or wrong decisions.
        // Here, we "queue" window updates to the flusher, so it will
        // be the only component responsible for window updates, for
        // both increments and reductions.
        flusher.window(stream, frame);
    }

    @Override
    public boolean isPushEnabled() {
        return pushEnabled;
    }

    /**
     * A typical close by a remote peer involves a GO_AWAY frame followed by TCP FIN.
     * This method is invoked when the TCP FIN is received, or when an exception is
     * thrown while reading, and we check the close state to act appropriately:
     * <ul>
     * <li>NOT_CLOSED: means that the remote peer did not send a GO_AWAY (abrupt close)
     * or there was an exception while reading, and therefore we terminate.</li>
     * <li>LOCALLY_CLOSED: we have sent the GO_AWAY to the remote peer, which received
     * it and closed the connection; we queue a disconnect to close the connection
     * on the local side.
     * The GO_AWAY just shutdown the output, so we need this step to make sure the
     * connection is closed. See {@link #close(int, String, Callback)}.</li>
     * <li>REMOTELY_CLOSED: we received the GO_AWAY, and the TCP FIN afterwards, so we
     * do nothing since the handling of the GO_AWAY will take care of closing the
     * connection. See {@link #onGoAway(GoAwayFrame)}.</li>
     * </ul>
     *
     * @see #onGoAway(GoAwayFrame)
     * @see #close(int, String, Callback)
     * @see #onIdleTimeout()
     */
    @Override
    public void onShutdown() {
        if (log.isDebugEnabled())
            log.debug("Shutting down {}", this);

        switch (closed.get()) {
            case NOT_CLOSED: {
                // The other peer did not send a GO_AWAY, no need to be gentle.
                if (log.isDebugEnabled())
                    log.debug("Abrupt close for {}", this);
                abort(new ClosedChannelException());
                break;
            }
            case LOCALLY_CLOSED: {
                // We have closed locally, and only shutdown
                // the output; now queue a disconnect.
                control(null, Callback.NOOP, new DisconnectFrame());
                break;
            }
            case REMOTELY_CLOSED: {
                // Nothing to do, the GO_AWAY frame we
                // received will close the connection.
                break;
            }
            default: {
                break;
            }
        }
    }

    /**
     * This method is invoked when the idle timeout triggers. We check the close state
     * to act appropriately:
     * <ul>
     * <li>NOT_CLOSED: it's a real idle timeout, we just initiate a close, see
     * {@link #close(int, String, Callback)}.</li>
     * <li>LOCALLY_CLOSED: we have sent a GO_AWAY and only shutdown the output, but the
     * other peer did not close the connection so we never received the TCP FIN, and
     * therefore we terminate.</li>
     * <li>REMOTELY_CLOSED: the other peer sent us a GO_AWAY, we should have queued a
     * disconnect, but for some reason it was not processed (for example, queue was
     * stuck because of TCP congestion), therefore we terminate.
     * See {@link #onGoAway(GoAwayFrame)}.</li>
     * </ul>
     *
     * @return true if the session should be closed, false otherwise
     * @see #onGoAway(GoAwayFrame)
     * @see #close(int, String, Callback)
     * @see #onShutdown()
     */
    @Override
    public boolean onIdleTimeout() {
        switch (closed.get()) {
            case NOT_CLOSED: {
                long elapsed = Millisecond100Clock.currentTimeMillis() - idleTime;
                return elapsed >= endPoint.getMaxIdleTimeout() && notifyIdleTimeout(this);
            }
            case LOCALLY_CLOSED:
            case REMOTELY_CLOSED: {
                abort(new TimeoutException("Idle timeout " + endPoint.getMaxIdleTimeout() + " ms"));
                return false;
            }
            default: {
                return false;
            }
        }
    }

    private void notIdle() {
        idleTime = Millisecond100Clock.currentTimeMillis();
    }

    @Override
    public void onFrame(Frame frame) {
        onConnectionFailure(ErrorCode.PROTOCOL_ERROR.code, "upgrade");
    }

    protected void onStreamOpened(StreamSPI stream) {
    }

    protected void onStreamClosed(StreamSPI stream) {
    }

    public void disconnect() {
        if (log.isDebugEnabled())
            log.debug("Disconnecting {}", this);
        endPoint.close();
    }

    private void terminate(Throwable cause) {
        while (true) {
            CloseState current = closed.get();
            switch (current) {
                case NOT_CLOSED:
                case LOCALLY_CLOSED:
                case REMOTELY_CLOSED: {
                    if (closed.compareAndSet(current, CloseState.CLOSED)) {
                        flusher.terminate(cause);
                        for (StreamSPI stream : streams.values())
                            stream.close();
                        streams.clear();
                        disconnect();
                        return;
                    }
                    break;
                }
                default: {
                    return;
                }
            }
        }
    }

    protected void abort(Throwable failure) {
        notifyFailure(this, failure, new TerminateCallback(failure));
    }

    public boolean isDisconnected() {
        return !endPoint.isOpen();
    }

    private void updateLastStreamId(int streamId) {
        Atomics.updateMax(lastStreamId, streamId);
    }

    protected Stream.Listener notifyNewStream(Stream stream, HeadersFrame frame) {
        try {
            return listener.onNewStream(stream, frame);
        } catch (Throwable x) {
            log.info("Failure while notifying listener " + listener, x);
            return null;
        }
    }

    protected void notifySettings(Session session, SettingsFrame frame) {
        try {
            listener.onSettings(session, frame);
        } catch (Throwable x) {
            log.info("Failure while notifying listener " + listener, x);
        }
    }

    protected void notifyPing(Session session, PingFrame frame) {
        try {
            listener.onPing(session, frame);
        } catch (Throwable x) {
            log.info("Failure while notifying listener " + listener, x);
        }
    }

    protected void notifyReset(Session session, ResetFrame frame) {
        try {
            listener.onReset(session, frame);
        } catch (Throwable x) {
            log.info("Failure while notifying listener " + listener, x);
        }
    }

    protected void notifyClose(Session session, GoAwayFrame frame, Callback callback) {
        try {
            listener.onClose(session, frame, callback);
        } catch (Throwable x) {
            log.info("Failure while notifying listener " + listener, x);
        }
    }

    protected boolean notifyIdleTimeout(Session session) {
        try {
            return listener.onIdleTimeout(session);
        } catch (Throwable x) {
            log.info("Failure while notifying listener " + listener, x);
            return true;
        }
    }

    protected void notifyFailure(Session session, Throwable failure, Callback callback) {
        try {
            listener.onFailure(session, failure, callback);
        } catch (Throwable x) {
            log.info("Failure while notifying listener " + listener, x);
        }
    }

    protected void notifyHeaders(StreamSPI stream, HeadersFrame frame) {
        Stream.Listener listener = stream.getListener();
        if (listener == null)
            return;
        try {
            listener.onHeaders(stream, frame);
        } catch (Throwable x) {
            log.info("Failure while notifying listener " + listener, x);
        }
    }

    @Override
    public String toString() {
        return String.format("%s@%x{l:%s <-> r:%s,sendWindow=%s,recvWindow=%s,streams=%d,%s}",
                getClass().getSimpleName(),
                hashCode(),
                getEndPoint().getLocalAddress(),
                getEndPoint().getRemoteAddress(),
                sendWindow,
                recvWindow,
                streams.size(),
                closed);
    }

    private class ControlEntry extends HTTP2Flusher.Entry {
        private int bytes;

        private ControlEntry(Frame frame, StreamSPI stream, Callback callback) {
            super(frame, stream, callback);
        }

        protected boolean generate(Queue<ByteBuffer> buffers) {
            List<ByteBuffer> controlFrame = generator.control(frame);
            bytes = (int) BufferUtils.remaining(controlFrame);
            buffers.addAll(controlFrame);
            if (log.isDebugEnabled())
                log.debug("Generated {}", frame);
            prepare();
            return true;
        }

        /**
         * <p>Performs actions just before writing the frame to the network.</p>
         * <p>Some frame, when sent over the network, causes the receiver
         * to react and send back frames that may be processed by the original
         * sender *before* {@link #succeeded()} is called.
         * <p>If the action to perform updates some state, this update may
         * not be seen by the received frames and cause errors.</p>
         * <p>For example, suppose the action updates the stream window to a
         * larger value; the sender sends the frame; the receiver is now entitled
         * to send back larger data; when the data is received by the original
         * sender, the action may have not been performed yet, causing the larger
         * data to be rejected, when it should have been accepted.</p>
         */
        private void prepare() {
            switch (frame.getType()) {
                case SETTINGS: {
                    SettingsFrame settingsFrame = (SettingsFrame) frame;
                    Integer initialWindow = settingsFrame.getSettings().get(SettingsFrame.INITIAL_WINDOW_SIZE);
                    if (initialWindow != null)
                        flowControl.updateInitialStreamWindow(HTTP2Session.this, initialWindow, true);
                    break;
                }
                default: {
                    break;
                }
            }
        }

        @Override
        public void succeeded() {
            bytesWritten.addAndGet(bytes);
            switch (frame.getType()) {
                case HEADERS: {
                    onStreamOpened(stream);
                    HeadersFrame headersFrame = (HeadersFrame) frame;
                    if (stream.updateClose(headersFrame.isEndStream(), true))
                        removeStream(stream);
                    break;
                }
                case RST_STREAM: {
                    if (stream != null) {
                        stream.close();
                        removeStream(stream);
                    }
                    break;
                }
                case PUSH_PROMISE: {
                    // Pushed streams are implicitly remotely closed.
                    // They are closed when sending an end-stream DATA frame.
                    stream.updateClose(true, false);
                    break;
                }
                case GO_AWAY: {
                    // We just sent a GO_AWAY, only shutdown the
                    // output without closing yet, to allow reads.
                    getEndPoint().shutdownOutput();
                    break;
                }
                case WINDOW_UPDATE: {
                    flowControl.windowUpdate(HTTP2Session.this, stream, (WindowUpdateFrame) frame);
                    break;
                }
                case DISCONNECT: {
                    terminate(new ClosedChannelException());
                    break;
                }
                default: {
                    break;
                }
            }
            super.succeeded();
        }
    }

    private class DataEntry extends HTTP2Flusher.Entry {
        private int bytes;
        private int dataRemaining;
        private int dataWritten;

        private DataEntry(DataFrame frame, StreamSPI stream, Callback callback) {
            super(frame, stream, callback);
            // We don't do any padding, so the flow control length is
            // always the data remaining. This simplifies the handling
            // of data frames that cannot be completely written due to
            // the flow control window exhausting, since in that case
            // we would have to count the padding only once.
            dataRemaining = frame.remaining();
        }

        @Override
        public int dataRemaining() {
            return dataRemaining;
        }

        protected boolean generate(Queue<ByteBuffer> buffers) {
            int dataRemaining = dataRemaining();

            int sessionSendWindow = getSendWindow();
            int streamSendWindow = stream.updateSendWindow(0);
            int window = Math.min(streamSendWindow, sessionSendWindow);
            if (window <= 0 && dataRemaining > 0)
                return false;

            int length = Math.min(dataRemaining, window);

            // Only one DATA frame is generated.
            Pair<Integer, List<ByteBuffer>> pair = generator.data((DataFrame) frame, length);
            bytes = pair.first;
            buffers.addAll(pair.second);
            int written = bytes - Frame.HEADER_LENGTH;
            if (log.isDebugEnabled())
                log.debug("Generated {}, length/window/data={}/{}/{}", frame, written, window, dataRemaining);

            this.dataWritten = written;
            this.dataRemaining -= written;

            flowControl.onDataSending(stream, written);

            return true;
        }

        @Override
        public void succeeded() {
            bytesWritten.addAndGet(bytes);
            flowControl.onDataSent(stream, dataWritten);

            // Do we have more to send ?
            DataFrame dataFrame = (DataFrame) frame;
            if (dataRemaining() == 0) {
                // Only now we can update the close state
                // and eventually remove the stream.
                if (stream.updateClose(dataFrame.isEndStream(), true))
                    removeStream(stream);
                super.succeeded();
            }
        }
    }

    private static class PromiseCallback<C> implements Callback {
        private final Promise<C> promise;
        private final C value;

        private PromiseCallback(Promise<C> promise, C value) {
            this.promise = promise;
            this.value = value;
        }

        @Override
        public void succeeded() {
            promise.succeeded(value);
        }

        @Override
        public void failed(Throwable x) {
            promise.failed(x);
        }
    }

    private class ResetCallback implements Callback {
        @Override
        public void succeeded() {
            complete();
        }

        @Override
        public void failed(Throwable x) {
            complete();
        }

        private void complete() {
            flusher.iterate();
        }
    }

    private class CloseCallback implements Callback {
        private final int error;
        private final String reason;

        private CloseCallback(int error, String reason) {
            this.error = error;
            this.reason = reason;
        }

        @Override
        public void succeeded() {
            complete();
        }

        @Override
        public void failed(Throwable x) {
            complete();
        }

        private void complete() {
            close(error, reason, Callback.NOOP);
        }
    }

    private class DisconnectCallback implements Callback {
        @Override
        public void succeeded() {
            complete();
        }

        @Override
        public void failed(Throwable x) {
            complete();
        }

        private void complete() {
            control(null, Callback.NOOP, new DisconnectFrame());
        }
    }

    private class TerminateCallback implements Callback {
        private final Throwable failure;

        private TerminateCallback(Throwable failure) {
            this.failure = failure;
        }

        @Override
        public void succeeded() {
            complete();
        }

        @Override
        public void failed(Throwable x) {
            failure.addSuppressed(x);
            complete();
        }

        private void complete() {
            terminate(failure);
        }
    }
}
