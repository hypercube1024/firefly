package com.firefly.net.tcp.aio;

import com.firefly.net.*;
import com.firefly.net.buffer.AdaptiveBufferSizePredictor;
import com.firefly.net.buffer.FileRegion;
import com.firefly.net.exception.NetException;
import com.firefly.net.tcp.aio.metric.SessionMetric;
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
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class AsynchronousTcpSession implements Session {

    private static Logger log = LoggerFactory.getLogger("firefly-system");

    private final int sessionId;
    private final long openTime;
    private long closeTime;
    private long lastReadTime;
    private long lastWrittenTime;
    private long readBytes = 0;
    private long writtenBytes = 0;
    private final SessionMetric sessionMetric;
    private final AtomicBoolean closed = new AtomicBoolean(false);
    private final AtomicBoolean shutdownOutput = new AtomicBoolean(false);
    private final AtomicBoolean shutdownInput = new AtomicBoolean(false);
    private final AtomicBoolean waitingForClose = new AtomicBoolean(false);

    private final AsynchronousSocketChannel socketChannel;
    private volatile InetSocketAddress localAddress;
    private volatile InetSocketAddress remoteAddress;

    private final Config config;
    private final NetEvent netEvent;
    private volatile Object attachment;

    private final Lock outputLock = new ReentrantLock();
    private boolean isWriting = false;
    private final Queue<OutputEntry<?>> outputBuffer = new LinkedList<>();
    private final BufferSizePredictor bufferSizePredictor = new AdaptiveBufferSizePredictor();

    AsynchronousTcpSession(int sessionId, Config config, SessionMetric sessionMetric, NetEvent netEvent, AsynchronousSocketChannel socketChannel) {
        this.sessionId = sessionId;
        this.openTime = Millisecond100Clock.currentTimeMillis();
        this.config = config;
        this.netEvent = netEvent;
        this.socketChannel = socketChannel;
        this.sessionMetric = sessionMetric;
        this.sessionMetric.getActiveSessionCount().inc();
    }

    private ByteBuffer allocateReadBuffer() {
        int size = BufferUtils.normalizeBufferSize(bufferSizePredictor.nextBufferSize());
        sessionMetric.getAllocatedInputBufferSize().update(size);
        return ByteBuffer.allocate(size);
    }

    void _read() {
        try {
            final ByteBuffer buf = allocateReadBuffer();
            if (log.isDebugEnabled()) {
                log.debug("The session {} allocates buffer. Its size is {}", getSessionId(), buf.remaining());
            }
            socketChannel.read(buf, config.getTimeout(), TimeUnit.MILLISECONDS, this, new InputCompletionHandler(buf));
        } catch (Exception e) {
            log.warn("register read event exception. {}", e.getMessage());
            closeNow();
        }
    }

    private class InputCompletionHandler implements CompletionHandler<Integer, AsynchronousTcpSession> {

        private final ByteBuffer buf;

        private InputCompletionHandler(ByteBuffer buf) {
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
                netEvent.notifyExceptionCaught(session, t);
            } finally {
                _read();
            }
        }

        @Override
        public void failed(Throwable t, AsynchronousTcpSession session) {
            if (t instanceof InterruptedByTimeoutException) {
                log.info("Read data failure. The session {} idle {}ms timeout. It will close.", getSessionId(), getIdleTimeout());
            } else {
                log.warn("The session {} reads data failure. It will force to close.", t, session.getSessionId());
            }
            closeNow();
        }
    }

    private class OutputEntryCompletionHandler<V extends Number, T> implements CompletionHandler<V, AsynchronousTcpSession> {

        private final OutputEntry<T> entry;

        private OutputEntryCompletionHandler(OutputEntry<T> entry) {
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
                closeNow();
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
            OutputEntry<?> entry = getNextOutputEntry();
            if (entry != null) {
                _write(entry);
            }
        }

        private OutputEntry<?> getNextOutputEntry() {
            OutputEntry<?> entry = null;
            outputLock.lock();
            try {
                sessionMetric.getOutputBufferQueueSize().update(outputBuffer.size());
                if (outputBuffer.isEmpty()) {
                    isWriting = false;
                } else {
                    List<OutputEntry<?>> entries = new LinkedList<>();

                    OutputEntry<?> obj;
                    boolean disconnection = false;
                    while ((obj = outputBuffer.poll()) != null) {
                        if (disconnection) {
                            log.warn("The session {} is waiting close. The entry [{}/{}] will discard", getSessionId(), obj.getOutputEntryType(), obj.remaining());
                            continue;
                        }
                        if (obj.getOutputEntryType() != OutputEntryType.DISCONNECTION) {
                            entries.add(obj);
                        } else {
                            disconnection = true;
                        }
                    }
                    if (disconnection) {
                        outputBuffer.offer(DISCONNECTION_FLAG);
                    }

                    if (entries.isEmpty()) {
                        if (!outputBuffer.isEmpty()) {
                            obj = outputBuffer.peek();
                            if (obj.getOutputEntryType() == OutputEntryType.DISCONNECTION) {
                                entry = DISCONNECTION_FLAG;
                                outputBuffer.poll();
                            }
                        }
                    } else {
                        if (entries.size() == 1) {
                            entry = entries.get(0);
                        } else {
                            // merge ByteBuffer to ByteBuffer Array
                            List<Callback> callbackList = new LinkedList<>();
                            List<ByteBuffer> byteBufferList = new LinkedList<>();
                            entries.forEach(e -> {
                                callbackList.add(e.getCallback());
                                switch (e.getOutputEntryType()) {
                                    case BYTE_BUFFER:
                                        ByteBufferOutputEntry byteBufferOutputEntry = (ByteBufferOutputEntry) e;
                                        byteBufferList.add(byteBufferOutputEntry.getData());
                                        break;
                                    case BYTE_BUFFER_ARRAY:
                                        ByteBufferArrayOutputEntry byteBufferArrayOutputEntry = (ByteBufferArrayOutputEntry) e;
                                        byteBufferList.addAll(Arrays.asList(byteBufferArrayOutputEntry.getData()));
                                        break;
                                    case MERGED_BUFFER:
                                        MergedOutputEntry mergedOutputEntry = (MergedOutputEntry) e;
                                        byteBufferList.addAll(Arrays.asList(mergedOutputEntry.getData()));
                                        break;
                                }
                            });
                            sessionMetric.getMergedOutputBufferSize().update(callbackList.size());
                            entry = new MergedOutputEntry(callbackList, byteBufferList);
                        }
                    }
                }
            } finally {
                outputLock.unlock();
            }
            return entry;
        }

        private void writingFailedCallback(Callback callback, Throwable t) {
            if (t instanceof InterruptedByTimeoutException) {
                log.info("Write data failure. The session {} idle {}ms timeout. It will close.", getSessionId(), getIdleTimeout());
            } else {
                log.warn("The session {} writes data failure. It will close.", t, getSessionId());
            }
            _writingFailedCallback(callback, t);
        }

        private void _writingFailedCallback(Callback callback, Throwable t) {
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
        try {
            switch (entry.getOutputEntryType()) {
                case BYTE_BUFFER: {
                    ByteBufferOutputEntry byteBufferOutputEntry = (ByteBufferOutputEntry) entry;
                    socketChannel.write(byteBufferOutputEntry.getData(),
                            config.getTimeout(), TimeUnit.MILLISECONDS, this,
                            new OutputEntryCompletionHandler<>(byteBufferOutputEntry));
                }
                break;
                case BYTE_BUFFER_ARRAY: {
                    ByteBufferArrayOutputEntry byteBuffersEntry = (ByteBufferArrayOutputEntry) entry;
                    int offset = byteBuffersEntry.getOffset();
                    int length = byteBuffersEntry.getData().length - offset;
                    socketChannel.write(byteBuffersEntry.getData(), offset, length,
                            config.getTimeout(), TimeUnit.MILLISECONDS, this,
                            new OutputEntryCompletionHandler<>(byteBuffersEntry));
                }
                break;
                case MERGED_BUFFER: {
                    MergedOutputEntry mergedOutputEntry = (MergedOutputEntry) entry;
                    int offset = mergedOutputEntry.getOffset();
                    int length = mergedOutputEntry.getData().length - offset;
                    socketChannel.write(mergedOutputEntry.getData(), offset, length,
                            config.getTimeout(), TimeUnit.MILLISECONDS, this,
                            new OutputEntryCompletionHandler<>(mergedOutputEntry));
                }
                break;
                case DISCONNECTION: {
                    log.info("The session {} has completed output. It will close.", getSessionId());
                    shutdownSocketChannel();
                }
                break;
                default:
                    throw new NetException("unknown output entry type");
            }
        } catch (Exception e) {
            log.warn("register write event exception. {}", e.getMessage());
            shutdownSocketChannel();
        }
    }

    @Override
    public void write(OutputEntry<?> entry) {
        if (entry == null) {
            return;
        }
        if (waitingForClose.get() && entry.getOutputEntryType() != OutputEntryType.DISCONNECTION) {
            log.warn("The session {} is waiting for close. The entry [{}/{}] can not write to remote endpoint.",
                    getSessionId(), entry.getOutputEntryType(), entry.remaining());
            return;
        }

        boolean writeEntry = false;
        outputLock.lock();
        try {
            if (!isWriting) {
                isWriting = true;
                writeEntry = true;
            } else {
                outputBuffer.offer(entry);
            }
        } finally {
            outputLock.unlock();
        }
        if (writeEntry) {
            _write(entry);
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
    public void notifyMessageReceived(Object message) {
        netEvent.notifyMessageReceived(this, message);
    }

    @Override
    public void encode(Object message) {
        try {
            config.getEncoder().encode(message, this);
        } catch (Throwable t) {
            netEvent.notifyExceptionCaught(this, t);
        }
    }

    @Override
    public void close() {
        if (waitingForClose.compareAndSet(false, true) && isOpen()) {
            write(DISCONNECTION_FLAG);
            log.info("The session {} is waiting for close", sessionId);
        } else {
            log.info("The session {} is already waiting for close", sessionId);
        }
    }

    @Override
    public void closeNow() {
        if (closed.compareAndSet(false, true)) {
            closeTime = Millisecond100Clock.currentTimeMillis();
            try {
                socketChannel.close();
                log.info("The session {} closed", sessionId);
            } catch (AsynchronousCloseException e) {
                log.warn("The session {} asynchronously close exception", sessionId);
            } catch (IOException e) {
                log.error("The session " + sessionId + " close exception", e);
            } finally {
                netEvent.notifySessionClosed(this);
                sessionMetric.getActiveSessionCount().dec();
                sessionMetric.getDuration().update(getDuration());
            }
        } else {
            log.info("The session {} already closed", sessionId);
        }
    }

    @Override
    public void shutdownOutput() {
        if (shutdownOutput.compareAndSet(false, true)) {
            try {
                socketChannel.shutdownOutput();
                log.info("The session {} is shutdown output", sessionId);
            } catch (ClosedChannelException e) {
                log.warn("Shutdown output exception. The session {} is closed", sessionId);
            } catch (IOException e) {
                log.error("The session {} shutdown output I/O exception. {}", sessionId, e.getMessage());
            }
        } else {
            log.info("The session {} is already shutdown output", sessionId);
        }
    }

    @Override
    public void shutdownInput() {
        if (shutdownInput.compareAndSet(false, true)) {
            try {
                socketChannel.shutdownInput();
                log.info("The session {} is shutdown input", sessionId);
            } catch (ClosedChannelException e) {
                log.warn("Shutdown input exception. The session {} is closed", sessionId);
            } catch (IOException e) {
                log.error("The session {} shutdown input I/O exception. {}", sessionId, e.getMessage());
            }
        } else {
            log.info("The session {} is already shutdown input", sessionId);
        }
    }

    private void shutdownSocketChannel() {
        shutdownOutput();
        shutdownInput();
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
        return Math.max(Math.max(lastReadTime, lastWrittenTime), openTime);
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
    public boolean isOpen() {
        return !closed.get();
    }

    @Override
    public boolean isClosed() {
        return closed.get();
    }

    @Override
    public boolean isShutdownOutput() {
        return shutdownOutput.get();
    }

    @Override
    public boolean isShutdownInput() {
        return shutdownInput.get();
    }

    @Override
    public boolean isWaitingForClose() {
        return waitingForClose.get();
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
    public long getIdleTimeout() {
        return Millisecond100Clock.currentTimeMillis() - getLastActiveTime();
    }

    @Override
    public long getMaxIdleTimeout() {
        return config.getTimeout();
    }

    @Override
    public String toString() {
        return "[sessionId=" + sessionId + ", openTime="
                + SafeSimpleDateFormat.defaultDateFormat.format(new Date(openTime)) + ", closeTime="
                + SafeSimpleDateFormat.defaultDateFormat.format(new Date(closeTime)) + ", duration=" + getDuration()
                + ", readBytes=" + readBytes + ", writtenBytes=" + writtenBytes + "]";
    }

}
