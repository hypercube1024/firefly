package com.firefly.net.tcp.codec.flex.stream.impl;

import com.firefly.net.tcp.TcpConnection;
import com.firefly.net.tcp.codec.flex.encode.FrameGenerator;
import com.firefly.net.tcp.codec.flex.protocol.*;
import com.firefly.net.tcp.codec.flex.stream.Context;
import com.firefly.net.tcp.codec.flex.stream.FlexConnection;
import com.firefly.net.tcp.codec.flex.stream.Session;
import com.firefly.net.tcp.codec.flex.stream.Stream;
import com.firefly.net.tcp.flex.metric.FlexMetric;
import com.firefly.utils.Assert;
import com.firefly.utils.concurrent.Callback;
import com.firefly.utils.concurrent.CountingCallback;
import com.firefly.utils.io.IO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicInteger;

import static com.firefly.net.tcp.codec.flex.stream.impl.FlexConnectionImpl.CONTEXT_KEY;
import static com.firefly.net.tcp.codec.flex.stream.impl.FlexConnectionImpl.CTX_LISTENER_KEY;
import static com.firefly.net.tcp.codec.flex.stream.impl.StreamStateTransferMap.getNextState;

/**
 * @author Pengtao Qiu
 */
public class FlexSession implements Session, Callback {

    protected static final Logger log = LoggerFactory.getLogger("firefly-system");

    protected final ConcurrentMap<Integer, Stream> streamMap = new ConcurrentHashMap<>();
    protected final AtomicInteger idGenerator;
    protected final TcpConnection connection;
    protected final LazyContextAttribute attribute = new LazyContextAttribute();
    protected final FlexMetric flexMetric;

    protected volatile Listener listener;

    public FlexSession(int initStreamId, TcpConnection connection, FlexMetric flexMetric) {
        this.idGenerator = new AtomicInteger(initStreamId);
        this.connection = connection;
        this.flexMetric = flexMetric;
    }

    @Override
    public Stream getStream(int streamId) {
        return streamMap.get(streamId);
    }

    @Override
    public Map<Integer, Stream> getAllStreams() {
        return streamMap;
    }

    protected void notifyCloseStream(Stream stream) {
        FlexStream flexStream = (FlexStream) stream;
        streamMap.remove(flexStream.getId());
        flexMetric.getActiveStreamCount().dec();
        if (log.isDebugEnabled()) {
            log.debug("Closed stream {}", stream.getId());
        }
        FlexConnection.Listener listener = (FlexConnection.Listener) stream.getAttribute(CTX_LISTENER_KEY);
        Context context = (Context) stream.getAttribute(CONTEXT_KEY);
        if (listener != null && context != null) {
            listener.close(context);
        }
    }

    protected void notifyNewStream(Stream stream, boolean local) {
        flexMetric.getActiveStreamCount().inc();
        flexMetric.getRequestMeter().mark();
    }

    public void notifyFrame(Frame frame) {
        switch (frame.getType()) {
            case CONTROL: {
                ControlFrame controlFrame = (ControlFrame) frame;
                Stream stream = streamMap.get(controlFrame.getStreamId());
                if (stream == null) {
                    // new remote stream
                    int id = controlFrame.getStreamId();
                    Stream.State state;
                    if (controlFrame.isEndStream()) {
                        state = getNextState(Stream.State.OPEN, StreamStateTransferMap.Op.RECV_ES);
                    } else {
                        state = Stream.State.OPEN;
                    }
                    FlexStream remoteNewStream = new FlexStream(id, this, null, state, false);
                    Stream old = streamMap.putIfAbsent(id, remoteNewStream);
                    Assert.state(old == null, "The stream " + id + " has been created.");

                    if (log.isDebugEnabled()) {
                        log.debug("Received a new remote stream: {}", remoteNewStream.toString());
                    }
                    notifyNewStream(remoteNewStream, false);
                    if (listener != null) {
                        remoteNewStream.setListener(listener.onNewStream(remoteNewStream, controlFrame));
                    }
                } else {
                    FlexStream flexStream = (FlexStream) stream;
                    if (controlFrame.isEndStream()) {
                        Stream.State next = getNextState(stream.getState(), StreamStateTransferMap.Op.RECV_ES);
                        flexStream.setState(next);
                        flexStream.getListener().onControl(controlFrame);
                        if (next == Stream.State.CLOSED) {
                            notifyCloseStream(stream);
                        }
                    } else {
                        flexStream.getListener().onControl(controlFrame);
                    }
                }
            }
            break;
            case DATA: {
                DataFrame dataFrame = (DataFrame) frame;
                FlexStream stream = (FlexStream) streamMap.get(dataFrame.getStreamId());
                Assert.state(stream != null, "The stream " + dataFrame.getStreamId() + " has been not created");

                if (dataFrame.isEndStream()) {
                    Stream.State next = getNextState(stream.getState(), StreamStateTransferMap.Op.RECV_ES);
                    stream.setState(next);
                    stream.getListener().onData(dataFrame);
                    if (next == Stream.State.CLOSED) {
                        notifyCloseStream(stream);
                    }
                } else {
                    stream.getListener().onData(dataFrame);
                }
            }
            break;
            case PING: {
                PingFrame pingFrame = (PingFrame) frame;
                if (pingFrame.isReply()) {
                    log.info("Connection {} received ping reply.", connection.getSessionId());
                    if (listener != null) {
                        listener.onPing(this, pingFrame);
                    }
                } else {
                    log.info("Connection {} received ping request.", connection.getSessionId());
                    PingFrame reply = new PingFrame(true);
                    sendFrame(reply);
                }
            }
            break;
            case DISCONNECTION: {
                DisconnectionFrame disconnectionFrame = (DisconnectionFrame) frame;
                log.info("Received disconnection frame" + disconnectionFrame);
                if (listener != null) {
                    listener.onDisconnect(this, disconnectionFrame);
                }
                IO.close(connection);
            }
            break;
        }
    }

    protected int generateId() {
        return idGenerator.getAndAdd(2);
    }

    @Override
    public Stream newStream(ControlFrame controlFrame, Callback callback, Stream.Listener listener) {
        int id = generateId();
        Stream.State state;
        if (controlFrame.isEndStream()) {
            state = getNextState(Stream.State.OPEN, StreamStateTransferMap.Op.SEND_ES);
        } else {
            state = Stream.State.OPEN;
        }

        Assert.notNull(listener, "The stream listener must be not null");
        FlexStream localNewStream = new FlexStream(id, this, listener, state, true);
        Stream old = streamMap.putIfAbsent(id, localNewStream);
        Assert.state(old == null, "The stream " + id + " has been created.");

        notifyNewStream(localNewStream, false);
        if (log.isDebugEnabled()) {
            log.debug("Create a new local stream: {}", localNewStream.toString());
        }
        sendFrame(new ControlFrame(controlFrame.isEndStream(), id, controlFrame.isEndFrame(), controlFrame.getData()), callback);
        return localNewStream;
    }

    @Override
    public void setListener(Listener listener) {
        Assert.notNull(listener, "The session listener must be not null");
        this.listener = listener;
    }

    @Override
    public CompletableFuture<Boolean> ping(PingFrame pingFrame) {
        return sendFrame(pingFrame);
    }

    @Override
    public CompletableFuture<Boolean> disconnect(DisconnectionFrame disconnectionFrame) {
        return sendFrame(disconnectionFrame);
    }

    @Override
    public CompletableFuture<Boolean> sendFrame(Frame frame) {
        CompletableFuture<Boolean> completableFuture = new CompletableFuture<>();
        sendFrame(frame, new Callback() {
            @Override
            public void succeeded() {
                completableFuture.complete(true);
            }

            @Override
            public void failed(Throwable x) {
                completableFuture.completeExceptionally(x);
            }
        });
        return completableFuture;
    }

    @Override
    public CompletableFuture<Boolean> sendFrames(List<Frame> frames) {
        CompletableFuture<Boolean> completableFuture = new CompletableFuture<>();
        sendFrames(frames, new Callback() {
            @Override
            public void succeeded() {
                completableFuture.complete(true);
            }

            @Override
            public void failed(Throwable x) {
                completableFuture.completeExceptionally(x);
            }
        });
        return completableFuture;
    }

    @Override
    public void sendFrame(Frame frame, Callback callback) {
        Callback.Nested nested = new Callback.Nested(callback) {
            @Override
            public void succeeded() {
                switch (frame.getType()) {
                    case CONTROL:
                    case DATA:
                        MessageFrame messageFrame = (MessageFrame) frame;
                        if (messageFrame.isEndStream()) {
                            Optional.ofNullable(streamMap.get(messageFrame.getStreamId()))
                                    .map(stream -> (FlexStream) stream)
                                    .ifPresent(stream -> {
                                        if (log.isDebugEnabled()) {
                                            log.debug("The stream {} sends message frame success.", stream.toString());
                                        }
                                        Stream.State next = getNextState(stream.getState(), StreamStateTransferMap.Op.SEND_ES);
                                        stream.setState(next);
                                        if (next == Stream.State.CLOSED) {
                                            notifyCloseStream(stream);
                                        }
                                    });
                        }
                        break;
                }
                super.succeeded();
                FlexSession.this.succeeded();
            }

            @Override
            public void failed(Throwable x) {
                super.failed(x);
                FlexSession.this.failed(x);
            }
        };
        _writeFrame(frame, nested);
    }

    @Override
    public void succeeded() {
    }

    @Override
    public void failed(Throwable x) {
        log.error("Write frame error", x);
        IO.close(connection);
    }

    public void clear() {
        int streamSize = streamMap.size();
        log.info("Connection closed. It will clear remaining {} streams.", streamSize);
        flexMetric.getActiveStreamCount().dec(streamSize);
        streamMap.clear();
    }

    protected void _writeFrame(Frame frame, Callback callback) {
        if (log.isDebugEnabled()) {
            log.debug("Send a frame: {}", frame.toString());
        }
        connection.write(FrameGenerator.generate(frame), callback::succeeded, callback::failed);
    }

    @Override
    public void sendFrames(List<Frame> frames, Callback callback) {
        CountingCallback countingCallback = new CountingCallback(callback, frames.size());
        frames.forEach(frame -> sendFrame(frame, countingCallback));
    }

    @Override
    public Map<String, Object> getAttributes() {
        return attribute.getAttributes();
    }

    @Override
    public void setAttribute(String key, Object value) {
        attribute.setAttribute(key, value);
    }

    @Override
    public Object getAttribute(String key) {
        return attribute.getAttribute(key);
    }
}
