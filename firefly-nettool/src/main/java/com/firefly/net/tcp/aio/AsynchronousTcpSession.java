package com.firefly.net.tcp.aio;

import com.codahale.metrics.Counter;
import com.codahale.metrics.Histogram;
import com.codahale.metrics.MetricRegistry;
import com.firefly.net.*;
import com.firefly.net.buffer.AdaptiveBufferSizePredictor;
import com.firefly.net.buffer.FileRegion;
import com.firefly.utils.concurrent.Callback;
import com.firefly.utils.io.BufferUtils;
import com.firefly.utils.time.Millisecond100Clock;
import com.firefly.utils.time.SafeSimpleDateFormat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.*;
import java.util.Collection;
import java.util.Date;
import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class AsynchronousTcpSession implements Session {

    private static Logger log = LoggerFactory.getLogger("firefly-system");

    private final int sessionId;
    private final long openTime;
    private final Counter activeCount;
    private final Histogram duration;
    private long closeTime;
    private long lastReadTime;
    private long lastWrittenTime;
    private long readBytes = 0;
    private long writtenBytes = 0;
    private volatile State state;
    private final AsynchronousSocketChannel socketChannel;
    private volatile InetSocketAddress localAddress;
    private volatile InetSocketAddress remoteAddress;

    private final Config config;
    private final EventManager eventManager;
    private volatile Object attachment;

    private final Lock outputLock = new ReentrantLock();
    private boolean isWriting = false;
    private final Queue<OutputEntry<?>> outputBuffer = new LinkedList<>();
    private final BufferSizePredictor bufferSizePredictor = new AdaptiveBufferSizePredictor();

    AsynchronousTcpSession(int sessionId, Config config, EventManager eventManager,
                           AsynchronousSocketChannel socketChannel) {
        this.sessionId = sessionId;
        this.openTime = Millisecond100Clock.currentTimeMillis();
        this.config = config;
        this.eventManager = eventManager;
        this.socketChannel = socketChannel;
        state = State.OPEN;
        MetricRegistry metrics = config.getMetricReporterFactory().getMetricRegistry();
        activeCount = metrics.counter("aio.AsynchronousTcpSession.activeCount");
        activeCount.inc();
        duration = metrics.histogram("aio.AsynchronousTcpSession.duration");
    }

    private ByteBuffer allocateReadBuffer() {
        return ByteBuffer.allocate(BufferUtils.normalizeBufferSize(bufferSizePredictor.nextBufferSize()));
    }

    void _read() {
        if (!isOpen()) {
            return;
        }

        final ByteBuffer buf = allocateReadBuffer();
        if (log.isDebugEnabled()) {
            log.debug("The session {} allocates buffer. Its size is {}", getSessionId(), buf.remaining());
        }
        socketChannel.read(buf, config.getTimeout(), TimeUnit.MILLISECONDS, this, new InputCompletionHandler(buf));
    }

    private class InputCompletionHandler implements CompletionHandler<Integer, AsynchronousTcpSession> {

        private final ByteBuffer buf;

        public InputCompletionHandler(ByteBuffer buf) {
            this.buf = buf;
        }

        @Override
        public void completed(Integer currentReadBytes, AsynchronousTcpSession session) {
            session.lastReadTime = Millisecond100Clock.currentTimeMillis();
            if (currentReadBytes < 0) {
                log.info("The session {} input channel is shutdown, {}", session.getSessionId(), currentReadBytes);
                session.closeNow();
                return;
            }

            if (log.isDebugEnabled()) {
                log.debug("The session {} read {} bytes", session.getSessionId(), currentReadBytes);
            }
            // Update the predictor.
            session.bufferSizePredictor.previousReceivedBufferSize(currentReadBytes);
            session.readBytes += currentReadBytes;
            buf.flip();
            try {
                config.getDecoder().decode(buf, session);
            } catch (Throwable t) {
                eventManager.executeExceptionTask(session, t);
            } finally {
                _read();
            }
        }

        @Override
        public void failed(Throwable t, AsynchronousTcpSession session) {
            if (t instanceof InterruptedByTimeoutException) {
                if (log.isDebugEnabled()) {
                    log.debug("The session {} reading data is timeout.", getSessionId());
                }
            } else {
                log.warn("The session {} read data is failed", t, session.getSessionId());
            }

            session.closeNow();
        }
    }

    private class OutputEntryCompletionHandler<V extends Number, T> implements CompletionHandler<V, AsynchronousTcpSession> {

        private final OutputEntry<T> entry;

        OutputEntryCompletionHandler(OutputEntry<T> entry) {
            this.entry = entry;
        }

        @Override
        public void completed(V currentWrittenBytes, AsynchronousTcpSession session) {
            lastWrittenTime = Millisecond100Clock.currentTimeMillis();
            if (log.isDebugEnabled()) {
                log.debug("The session {} completed writing {} bytes, remaining {} bytes",
                        session.getSessionId(),
                        currentWrittenBytes, entry.remaining());
            }

            long w = currentWrittenBytes.longValue();
            if (w < 0) {
                log.info("The session {} output channel is shutdown, {}", getSessionId(), currentWrittenBytes);
                shutdownSocketChannel();
                return;
            }

            if (log.isDebugEnabled()) {
                log.debug("The session {} writes {} bytes", getSessionId(), currentWrittenBytes);
            }
            writtenBytes += w;

            if (entry.remaining() > 0) {
                _write(entry);
            } else {
                writingCompletedCallback(entry.getCallback());
            }
        }

        @Override
        public void failed(Throwable t, AsynchronousTcpSession session) {
            writingFailedCallback(entry.getCallback(), t);
        }


        private void writingCompletedCallback(Callback callback) {
            callback.succeeded();
            outputLock.lock();
            try {
                OutputEntry<?> obj = outputBuffer.poll();
                if (obj != null) {
                    _write(obj);
                } else {
                    isWriting = false;
                }
            } finally {
                outputLock.unlock();
            }
        }

        private void writingFailedCallback(Callback callback, Throwable t) {
            if (t instanceof InterruptedByTimeoutException) {
                if (log.isDebugEnabled()) {
                    log.debug("The session {} writing data is timeout.", getSessionId());
                }
            } else {
                log.warn("The session {} writes data is failed", t, getSessionId());
            }

            outputLock.lock();
            try {
                int bufferSize = outputBuffer.size();
                log.warn("The session {} has {} buffer data can not output", getSessionId(), bufferSize);
                outputBuffer.clear();
                isWriting = false;
                shutdownSocketChannel();
            } finally {
                outputLock.unlock();
            }
            callback.failed(t);
        }
    }

    private void _write(final OutputEntry<?> entry) {
        if (!isOpen()) {
            return;
        }
        switch (entry.getOutputEntryType()) {
            case BYTE_BUFFER:
                ByteBufferOutputEntry byteBufferOutputEntry = (ByteBufferOutputEntry) entry;
                socketChannel.write(byteBufferOutputEntry.getData(),
                        config.getTimeout(), TimeUnit.MILLISECONDS, this,
                        new OutputEntryCompletionHandler<>(byteBufferOutputEntry));
                break;

            case BYTE_BUFFER_ARRAY:
                ByteBufferArrayOutputEntry byteBuffersEntry = (ByteBufferArrayOutputEntry) entry;
                socketChannel.write(byteBuffersEntry.getData(), 0, byteBuffersEntry.getData().length,
                        config.getTimeout(), TimeUnit.MILLISECONDS, this,
                        new OutputEntryCompletionHandler<>(byteBuffersEntry));
                break;
            case DISCONNECTION:
                log.debug("The session {} will close", getSessionId());
                shutdownSocketChannel();
            default:
                break;
        }
    }

    @Override
    public void write(OutputEntry<?> entry) {
        if (!isOpen()) {
            return;
        }
        if (entry == null) {
            return;
        }
        outputLock.lock();
        try {
            if (!isWriting) {
                isWriting = true;
                _write(entry);
            } else {
                outputBuffer.offer(entry);
            }
        } finally {
            outputLock.unlock();
        }
    }

    @Override
    public void write(ByteBuffer byteBuffer, Callback callback) {
        write(new ByteBufferOutputEntry(callback, byteBuffer));
    }

    @Override
    public void write(ByteBuffer[] buffers, Callback callback) {
        write(new ByteBufferArrayOutputEntry(callback, buffers));
    }

    @Override
    public void write(Collection<ByteBuffer> buffers, Callback callback) {
        write(new ByteBufferArrayOutputEntry(callback, buffers.toArray(BufferUtils.EMPTY_BYTE_BUFFER_ARRAY)));
    }

    @Override
    public void write(FileRegion file, Callback callback) {
        try (FileRegion fileRegion = file) {
            fileRegion.transferTo(callback, (buf, countingCallback, count) -> write(buf, countingCallback));
        } catch (Throwable t) {
            log.error("transfer file error", t);
        }
    }

    @Override
    public void attachObject(Object attachment) {
        this.attachment = attachment;
    }

    @Override
    public Object getAttachment() {
        return attachment;
    }

    @Override
    public void fireReceiveMessage(Object message) {
        eventManager.executeReceiveTask(this, message);
    }

    @Override
    public void encode(Object message) {
        try {
            config.getEncoder().encode(message, this);
        } catch (Throwable t) {
            eventManager.executeExceptionTask(this, t);
        }
    }

    @Override
    public void close() {
        write(DISCONNECTION_FLAG);
    }

    @Override
    public void closeNow() {
        if (!isOpen()) {
            return;
        }
        closeTime = Millisecond100Clock.currentTimeMillis();
        try {
            socketChannel.close();
        } catch (AsynchronousCloseException e) {
            if (log.isDebugEnabled())
                log.debug("The session {} asynchronously closed", sessionId);
        } catch (IOException e) {
            log.error("The session {} close error", e, sessionId);
        }
        state = State.CLOSE;
        eventManager.executeCloseTask(this);
        activeCount.dec();
        duration.update(getDuration());
    }

    @Override
    public void shutdownOutput() {
        try {
            socketChannel.shutdownOutput();
        } catch (ClosedChannelException e) {
            log.debug("The session {} is closed", e, sessionId);
        } catch (IOException e) {
            log.error("The session {} shutdown output error", e, sessionId);
        }
    }

    @Override
    public void shutdownInput() {
        try {
            socketChannel.shutdownInput();
        } catch (ClosedChannelException e) {
            log.debug("The session {} is closed", e, sessionId);
        } catch (IOException e) {
            log.error("The session {} shutdown input error", e, sessionId);
        }
    }

    private void shutdownSocketChannel() {
        shutdownOutput();
        shutdownInput();
        state = State.CLOSE;
    }

    @Override
    public int getSessionId() {
        return sessionId;
    }

    @Override
    public long getOpenTime() {
        return openTime;
    }

    @Override
    public long getCloseTime() {
        return closeTime;
    }

    @Override
    public long getDuration() {
        if (closeTime > 0) {
            return closeTime - openTime;
        } else {
            return Millisecond100Clock.currentTimeMillis() - openTime;
        }
    }

    @Override
    public long getLastReadTime() {
        return lastReadTime;
    }

    @Override
    public long getLastWrittenTime() {
        return lastWrittenTime;
    }

    @Override
    public long getLastActiveTime() {
        return Math.max(lastReadTime, lastWrittenTime);
    }

    @Override
    public long getReadBytes() {
        return readBytes;
    }

    @Override
    public long getWrittenBytes() {
        return writtenBytes;
    }

    @Override
    public State getState() {
        return state;
    }

    @Override
    public boolean isOpen() {
        return state == State.OPEN;
    }

    @Override
    public InetSocketAddress getLocalAddress() {
        if (localAddress != null) {
            return localAddress;
        } else {
            try {
                localAddress = (InetSocketAddress) socketChannel.getLocalAddress();
                return localAddress;
            } catch (IOException e) {
                log.error("The session {} gets local address error", e, sessionId);
                return null;
            }
        }
    }

    @Override
    public InetSocketAddress getRemoteAddress() {
        if (remoteAddress != null) {
            return remoteAddress;
        } else {
            try {
                remoteAddress = (InetSocketAddress) socketChannel.getRemoteAddress();
                return remoteAddress;
            } catch (Throwable t) {
                log.error("The session {} gets remote address error", t, sessionId);
                return null;
            }
        }
    }

    @Override
    public String toString() {
        return "[sessionId=" + sessionId + ", openTime="
                + SafeSimpleDateFormat.defaultDateFormat.format(new Date(openTime)) + ", closeTime="
                + SafeSimpleDateFormat.defaultDateFormat.format(new Date(closeTime)) + ", duration=" + getDuration()
                + ", readBytes=" + readBytes + ", writtenBytes=" + writtenBytes + "]";
    }

    @Override
    public long getIdleTimeout() {
        return config.getTimeout();
    }

}
