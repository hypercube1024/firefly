package com.firefly.codec.websocket.stream;

import com.firefly.codec.websocket.encode.Generator;
import com.firefly.codec.websocket.frame.BinaryFrame;
import com.firefly.codec.websocket.frame.Frame;
import com.firefly.codec.websocket.model.BatchMode;
import com.firefly.codec.websocket.model.OpCode;
import com.firefly.codec.websocket.model.WriteCallback;
import com.firefly.net.ByteBufferArrayOutputEntry;
import com.firefly.utils.concurrent.IteratingCallback;
import com.firefly.utils.io.BufferUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.ByteBuffer;
import java.nio.channels.ClosedChannelException;
import java.util.*;

public class FrameFlusher extends IteratingCallback {
    public static final BinaryFrame FLUSH_FRAME = new BinaryFrame();
    private static Logger LOG = LoggerFactory.getLogger("firefly-system");

    private final com.firefly.net.Session endPoint;
    private final int bufferSize;
    private final Generator generator;
    private final int maxGather;
    private final Deque<FrameEntry> queue = new ArrayDeque<>();
    private final List<FrameEntry> entries;
    private final List<ByteBuffer> buffers;
    private boolean closed;
    private Throwable terminated;
    private ByteBuffer aggregate;
    private BatchMode batchMode;

    public FrameFlusher(Generator generator, com.firefly.net.Session endPoint, int bufferSize, int maxGather) {
        this.endPoint = endPoint;
        this.bufferSize = bufferSize;
        this.generator = Objects.requireNonNull(generator);
        this.maxGather = maxGather;
        this.entries = new ArrayList<>(maxGather);
        this.buffers = new ArrayList<>((maxGather * 2) + 1);
    }

    public void enqueue(Frame frame, WriteCallback callback, BatchMode batchMode) {
        FrameEntry entry = new FrameEntry(frame, callback, batchMode);

        Throwable closed;
        synchronized (this) {
            closed = terminated;
            if (closed == null) {
                byte opCode = frame.getOpCode();
                if (opCode == OpCode.PING || opCode == OpCode.PONG)
                    queue.offerFirst(entry);
                else
                    queue.offerLast(entry);
            }
        }

        if (closed == null)
            iterate();
        else
            notifyCallbackFailure(callback, closed);
    }

    @Override
    protected Action process() throws Throwable {
        if (LOG.isDebugEnabled())
            LOG.debug("Flushing {}", this);

        int space = aggregate == null ? bufferSize : BufferUtils.space(aggregate);
        BatchMode currentBatchMode = BatchMode.AUTO;
        synchronized (this) {
            if (closed)
                return Action.SUCCEEDED;

            if (terminated != null)
                throw terminated;

            while (!queue.isEmpty() && entries.size() <= maxGather) {
                FrameEntry entry = queue.poll();
                currentBatchMode = BatchMode.max(currentBatchMode, entry.batchMode);

                // Force flush if we need to.
                if (entry.frame == FLUSH_FRAME)
                    currentBatchMode = BatchMode.OFF;

                int payloadLength = BufferUtils.length(entry.frame.getPayload());
                int approxFrameLength = Generator.MAX_HEADER_LENGTH + payloadLength;

                // If it is a "big" frame, avoid copying into the aggregate buffer.
                if (approxFrameLength > (bufferSize >> 2))
                    currentBatchMode = BatchMode.OFF;

                // If the aggregate buffer overflows, do not batch.
                space -= approxFrameLength;
                if (space <= 0)
                    currentBatchMode = BatchMode.OFF;

                entries.add(entry);
            }
        }

        if (LOG.isDebugEnabled())
            LOG.debug("{} processing {} entries: {}", this, entries.size(), entries);

        if (entries.isEmpty()) {
            if (batchMode != BatchMode.AUTO) {
                // Nothing more to do, release the aggregate buffer if we need to.
                // Releasing it here rather than in succeeded() allows for its reuse.
                releaseAggregate();
                return Action.IDLE;
            }

            if (LOG.isDebugEnabled())
                LOG.debug("{} auto flushing", this);

            return flush();
        }

        batchMode = currentBatchMode;

        return currentBatchMode == BatchMode.OFF ? flush() : batch();
    }

    private Action batch() {
        if (aggregate == null) {
            aggregate = BufferUtils.allocate(bufferSize);
            if (LOG.isDebugEnabled())
                LOG.debug("{} acquired aggregate buffer {}", this, aggregate);
        }

        for (FrameEntry entry : entries) {
            entry.generateHeaderBytes(aggregate);

            ByteBuffer payload = entry.frame.getPayload();
            if (BufferUtils.hasContent(payload))
                BufferUtils.append(aggregate, payload);
        }
        if (LOG.isDebugEnabled())
            LOG.debug("{} aggregated {} frames: {}", this, entries.size(), entries);

        // We just aggregated the entries, so we need to succeed their callbacks.
        succeeded();

        return Action.SCHEDULED;
    }

    private Action flush() {
        if (!BufferUtils.isEmpty(aggregate)) {
            buffers.add(aggregate);
            if (LOG.isDebugEnabled())
                LOG.debug("{} flushing aggregate {}", this, aggregate);
        }

        for (FrameEntry entry : entries) {
            // Skip the "synthetic" frame used for flushing.
            if (entry.frame == FLUSH_FRAME)
                continue;

            buffers.add(entry.generateHeaderBytes());
            ByteBuffer payload = entry.frame.getPayload();
            if (BufferUtils.hasContent(payload))
                buffers.add(payload);
        }

        if (LOG.isDebugEnabled())
            LOG.debug("{} flushing {} frames: {}", this, entries.size(), entries);

        if (buffers.isEmpty()) {
            releaseAggregate();
            // We may have the FLUSH_FRAME to notify.
            succeedEntries();
            return Action.IDLE;
        }

        ByteBufferArrayOutputEntry outputEntry = new ByteBufferArrayOutputEntry(this,
                buffers.toArray(BufferUtils.EMPTY_BYTE_BUFFER_ARRAY));
        endPoint.encode(outputEntry);
        buffers.clear();
        return Action.SCHEDULED;
    }

    private int getQueueSize() {
        synchronized (this) {
            return queue.size();
        }
    }

    @Override
    public void succeeded() {
        succeedEntries();
        super.succeeded();
    }

    private void succeedEntries() {
        for (FrameEntry entry : entries) {
            notifyCallbackSuccess(entry.callback);
            entry.release();
            if (entry.frame.getOpCode() == OpCode.CLOSE) {
                terminate(new ClosedChannelException(), true);
                endPoint.shutdownOutput();
            }
        }
        entries.clear();
    }

    @Override
    public void onCompleteFailure(Throwable failure) {
        releaseAggregate();

        Throwable closed;
        synchronized (this) {
            closed = terminated;
            if (closed == null)
                terminated = failure;
            entries.addAll(queue);
            queue.clear();
        }

        for (FrameEntry entry : entries) {
            notifyCallbackFailure(entry.callback, failure);
            entry.release();
        }
        entries.clear();
    }

    private void releaseAggregate() {
        aggregate = null;
    }

    void terminate(Throwable cause, boolean close) {
        Throwable reason;
        synchronized (this) {
            closed = close;
            reason = terminated;
            if (reason == null)
                terminated = cause;
        }
        if (LOG.isDebugEnabled())
            LOG.debug("{} {}", reason == null ? "Terminating" : "Terminated", this);
        if (reason == null && !close)
            iterate();
    }

    protected void notifyCallbackSuccess(WriteCallback callback) {
        try {
            if (callback != null) {
                callback.writeSuccess();
            }
        } catch (Throwable x) {
            if (LOG.isDebugEnabled())
                LOG.debug("Exception while notifying success of callback " + callback, x);
        }
    }

    protected void notifyCallbackFailure(WriteCallback callback, Throwable failure) {
        try {
            if (callback != null) {
                callback.writeFailed(failure);
            }
        } catch (Throwable x) {
            if (LOG.isDebugEnabled())
                LOG.debug("Exception while notifying failure of callback " + callback, x);
        }
    }

    @Override
    public String toString() {
        return String.format("%s@%x[queueSize=%d,aggregateSize=%d,terminated=%s]",
                getClass().getSimpleName(),
                hashCode(),
                getQueueSize(),
                aggregate == null ? 0 : aggregate.position(),
                terminated);
    }

    private class FrameEntry {
        private final Frame frame;
        private final WriteCallback callback;
        private final BatchMode batchMode;
        private ByteBuffer headerBuffer;

        private FrameEntry(Frame frame, WriteCallback callback, BatchMode batchMode) {
            this.frame = Objects.requireNonNull(frame);
            this.callback = callback;
            this.batchMode = batchMode;
        }

        private ByteBuffer generateHeaderBytes() {
            return headerBuffer = generator.generateHeaderBytes(frame);
        }

        private void generateHeaderBytes(ByteBuffer buffer) {
            generator.generateHeaderBytes(frame, buffer);
        }

        private void release() {
            headerBuffer = null;
        }

        @Override
        public String toString() {
            return String.format("%s[%s,%s,%s,%s]", getClass().getSimpleName(), frame, callback, batchMode, terminated);
        }
    }
}
