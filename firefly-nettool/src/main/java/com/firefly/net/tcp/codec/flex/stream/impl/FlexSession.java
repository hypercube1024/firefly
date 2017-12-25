package com.firefly.net.tcp.codec.flex.stream.impl;

import com.firefly.net.tcp.TcpConnection;
import com.firefly.net.tcp.codec.flex.encode.FrameGenerator;
import com.firefly.net.tcp.codec.flex.protocol.*;
import com.firefly.net.tcp.codec.flex.stream.Session;
import com.firefly.net.tcp.codec.flex.stream.Stream;
import com.firefly.utils.Assert;
import com.firefly.utils.concurrent.Callback;
import com.firefly.utils.concurrent.CountingCallback;
import com.firefly.utils.io.IO;
import com.firefly.utils.lang.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author Pengtao Qiu
 */
public class FlexSession implements Session, Callback {

    protected static final Logger log = LoggerFactory.getLogger("firefly-system");

    protected final ConcurrentMap<Integer, Stream> streamMap = new ConcurrentHashMap<>();
    protected final AtomicInteger idGenerator;
    protected final TcpConnection connection;
    protected final LazyContextAttribute attribute = new LazyContextAttribute();

    protected volatile Listener listener;
    protected boolean isWriting;
    protected LinkedList<Pair<Frame, Callback>> frames = new LinkedList<>();

    public FlexSession(int initStreamId, TcpConnection connection) {
        this.idGenerator = new AtomicInteger(initStreamId);
        this.connection = connection;
    }

    @Override
    public Stream getStream(int streamId) {
        return streamMap.get(streamId);
    }

    @Override
    public Map<Integer, Stream> getAllStreams() {
        return streamMap;
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
                        state = Stream.State.REMOTELY_CLOSED;
                    } else {
                        state = Stream.State.OPEN;
                    }
                    FlexStream remoteNewStream = new FlexStream(id, this, null, state, false);
                    Stream old = streamMap.putIfAbsent(id, remoteNewStream);
                    Assert.state(old == null, "The stream " + id + " has been created.");

                    if (listener != null) {
                        remoteNewStream.setListener(listener.onNewStream(remoteNewStream, controlFrame));
                    }
                } else {
                    switch (stream.getState()) {
                        case REMOTELY_CLOSED:
                        case CLOSED:
                            throw new IllegalStateException("The stream has been closed");
                        case LOCALLY_CLOSED: {
                            if (controlFrame.isEndStream()) {
                                ((FlexStream) stream).getListener().onControl(controlFrame);
                                ((FlexStream) stream).setState(Stream.State.CLOSED);
                                streamMap.remove(stream.getId());
                            } else {
                                ((FlexStream) stream).getListener().onControl(controlFrame);
                            }
                        }
                        break;
                        case OPEN:
                            ((FlexStream) stream).getListener().onControl(controlFrame);
                            break;
                    }
                }
            }
            break;
            case DATA: {
                DataFrame dataFrame = (DataFrame) frame;
                Stream stream = streamMap.get(dataFrame.getStreamId());
                Assert.state(stream != null, "The stream " + dataFrame.getStreamId() + " has been not created");

                switch (stream.getState()) {
                    case REMOTELY_CLOSED:
                    case CLOSED:
                        throw new IllegalStateException("The stream has been closed");
                    case LOCALLY_CLOSED: {
                        if (dataFrame.isEndStream()) {
                            ((FlexStream) stream).getListener().onData(dataFrame);
                            ((FlexStream) stream).setState(Stream.State.CLOSED);
                            streamMap.remove(stream.getId());
                        } else {
                            ((FlexStream) stream).getListener().onData(dataFrame);
                        }
                    }
                    break;
                    case OPEN:
                        ((FlexStream) stream).getListener().onData(dataFrame);
                        break;
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
    public CompletableFuture<Stream> newStream(ControlFrame controlFrame, Stream.Listener listener) {
        int id = generateId();
        Stream.State state;
        if (controlFrame.isEndStream()) {
            state = Stream.State.LOCALLY_CLOSED;
        } else {
            state = Stream.State.OPEN;
        }

        Assert.notNull(listener, "The stream listener must be not null");
        FlexStream stream = new FlexStream(id, this, listener, state, true);
        Stream old = streamMap.putIfAbsent(id, stream);
        Assert.state(old == null, "The stream " + id + " has been created.");

        ControlFrame newFrame = new ControlFrame(controlFrame.isEndStream(), id, controlFrame.isEndFrame(), controlFrame.getData());
        return sendFrame(newFrame).thenApply(success -> stream);
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
    public synchronized void sendFrame(Frame frame, Callback callback) {
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
                                        switch (stream.getState()) {
                                            case OPEN:
                                                stream.setState(Stream.State.LOCALLY_CLOSED);
                                                break;
                                            case REMOTELY_CLOSED:
                                                stream.setState(Stream.State.CLOSED);
                                                break;
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
        if (isWriting) {
            frames.offer(new Pair<>(frame, nested));
        } else {
            _writeFrame(frame, nested);
        }
    }

    @Override
    public synchronized void succeeded() {
        Pair<Frame, Callback> pair = frames.poll();
        if (pair != null) {
            _writeFrame(pair.first, pair.second);
        } else {
            isWriting = false;
        }
    }

    @Override
    public synchronized void failed(Throwable x) {
        log.error("Write flex frame error", x);
        isWriting = false;
        IO.close(connection);
    }

    protected synchronized void _writeFrame(Frame frame, Callback callback) {
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
