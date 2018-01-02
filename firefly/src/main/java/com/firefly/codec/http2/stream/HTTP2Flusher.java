package com.firefly.codec.http2.stream;

import com.firefly.codec.http2.frame.Frame;
import com.firefly.codec.http2.frame.WindowUpdateFrame;
import com.firefly.net.ByteBufferArrayOutputEntry;
import com.firefly.utils.concurrent.Callback;
import com.firefly.utils.concurrent.IteratingCallback;
import com.firefly.utils.io.BufferUtils;
import com.firefly.utils.io.EofException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.ByteBuffer;
import java.util.*;

public class HTTP2Flusher extends IteratingCallback {
    private static Logger log = LoggerFactory.getLogger("firefly-system");

    private final Queue<WindowEntry> windows = new ArrayDeque<>();
    private final Deque<Entry> frames = new ArrayDeque<>();
    private final Queue<Entry> entries = new ArrayDeque<>();
    private final List<Entry> actives = new ArrayList<>();
    private final HTTP2Session session;
    private final Queue<ByteBuffer> buffers = new LinkedList<>();
    private Entry stalled;
    private Throwable terminated;

    public HTTP2Flusher(HTTP2Session session) {
        this.session = session;
    }

    public void window(StreamSPI stream, WindowUpdateFrame frame) {
        Throwable closed;
        synchronized (this) {
            closed = terminated;
            if (closed == null)
                windows.offer(new WindowEntry(stream, frame));
        }
        // Flush stalled data.
        if (closed == null)
            iterate();
    }

    public boolean prepend(Entry entry) {
        Throwable closed;
        synchronized (this) {
            closed = terminated;
            if (closed == null) {
                frames.offerFirst(entry);
                if (log.isDebugEnabled()) {
                    log.debug("Prepended {}, frames={}", entry.toString(), frames.size());
                }
            }
        }
        if (closed == null)
            return true;
        closed(entry, closed);
        return false;
    }

    public boolean append(Entry entry) {
        Throwable closed;
        synchronized (this) {
            closed = terminated;
            if (closed == null) {
                frames.offer(entry);
                if (log.isDebugEnabled()) {
                    log.debug("Appended {}, frames={}", entry.toString(), frames.size());
                }
            }
        }
        if (closed == null) {
            return true;
        }
        closed(entry, closed);
        return false;
    }

    private synchronized int getWindowQueueSize() {
        return windows.size();
    }

    public synchronized int getFrameQueueSize() {
        return frames.size();
    }

    @Override
    protected Action process() throws Throwable {
        if (log.isDebugEnabled()) {
            log.debug("Flushing {}", session.toString());
        }
        synchronized (this) {
            if (terminated != null) {
                throw terminated;
            }

            while (!windows.isEmpty()) {
                WindowEntry entry = windows.poll();
                entry.perform();
            }

            for (Entry entry : frames) {
                entries.offer(entry);
                actives.add(entry);
            }
            frames.clear();
        }


        if (entries.isEmpty()) {
            if (log.isDebugEnabled()) {
                log.debug("Flushed {}", session.toString());
            }
            return Action.IDLE;
        }

        while (!entries.isEmpty()) {
            Entry entry = entries.poll();
            if (log.isDebugEnabled()) {
                log.debug("Processing {}", entry.toString());
            }
            // If the stream has been reset or removed, don't send the frame.
            if (entry.isStale()) {
                if (log.isDebugEnabled()) {
                    log.debug("Stale {}", entry.toString());
                }
                continue;
            }

            try {
                if (entry.generate(buffers)) {
                    if (entry.dataRemaining() > 0)
                        entries.offer(entry);
                } else {
                    if (stalled == null)
                        stalled = entry;
                }
            } catch (Throwable failure) {
                // Failure to generate the entry is catastrophic.
                if (log.isDebugEnabled()) {
                    log.debug("Failure generating frame " + entry.frame, failure);
                }
                failed(failure);
                return Action.SUCCEEDED;
            }
        }

        if (buffers.isEmpty()) {
            complete();
            return Action.IDLE;
        }

        if (log.isDebugEnabled()) {
            log.debug("Writing {} buffers ({} bytes) for {} frames {}",
                    buffers.size(), BufferUtils.remaining(buffers), actives.size(), actives.toString());
        }
        session.getEndPoint().encode(new ByteBufferArrayOutputEntry(this, buffers.toArray(BufferUtils.EMPTY_BYTE_BUFFER_ARRAY)));
        return Action.SCHEDULED;
    }

    @Override
    public void succeeded() {
        if (log.isDebugEnabled()) {
            log.debug("Written {} frames for {}", actives.size(), actives.toString());
        }
        complete();

        super.succeeded();
    }

    private void complete() {
        buffers.clear();

        actives.forEach(Entry::complete);

        if (stalled != null) {
            // We have written part of the frame, but there is more to write.
            // The API will not allow to send two data frames for the same
            // stream so we append the unfinished frame at the end to allow
            // better interleaving with other streams.
            int index = actives.indexOf(stalled);
            for (int i = index; i < actives.size(); ++i) {
                Entry entry = actives.get(i);
                if (entry.dataRemaining() > 0)
                    append(entry);
            }
            for (int i = 0; i < index; ++i) {
                Entry entry = actives.get(i);
                if (entry.dataRemaining() > 0)
                    append(entry);
            }
            stalled = null;
        }

        actives.clear();
    }

    @Override
    protected void onCompleteSuccess() {
        throw new IllegalStateException();
    }

    @Override
    protected void onCompleteFailure(Throwable x) {
        buffers.clear();

        Throwable closed;
        synchronized (this) {
            closed = terminated;
            terminated = x;
            if (log.isDebugEnabled()) {
                log.debug("{}, active/queued={}/{}", closed != null ? "Closing" : "Failing", actives.size(), frames.size());
            }
            actives.addAll(frames);
            frames.clear();
        }

        actives.forEach(entry -> entry.failed(x));
        actives.clear();

        // If the failure came from within the
        // flusher, we need to close the connection.
        if (closed == null)
            session.abort(x);
    }

    void terminate(Throwable cause) {
        Throwable closed;
        synchronized (this) {
            closed = terminated;
            terminated = cause;
            if (log.isDebugEnabled()) {
                log.debug("{}", closed != null ? "Terminated" : "Terminating");
            }
        }
        if (closed == null)
            iterate();
    }

    private void closed(Entry entry, Throwable failure) {
        entry.failed(failure);
    }

    @Override
    public String toString() {
        return String.format("%s[window_queue=%d,frame_queue=%d,actives=%d]",
                super.toString(),
                getWindowQueueSize(),
                getFrameQueueSize(),
                actives.size());
    }

    public static abstract class Entry extends Callback.Nested {
        protected final Frame frame;
        protected final StreamSPI stream;

        protected Entry(Frame frame, StreamSPI stream, Callback callback) {
            super(callback);
            this.frame = frame;
            this.stream = stream;
        }

        public int dataRemaining() {
            return 0;
        }

        protected abstract boolean generate(Queue<ByteBuffer> buffers);

        private void complete() {
            if (isStale())
                failed(new EofException("reset"));
            else
                succeeded();
        }

        @Override
        public void failed(Throwable x) {
            if (stream != null) {
                stream.close();
                stream.getSession().removeStream(stream);
            }
            super.failed(x);
        }

        private boolean isStale() {
            return !isProtocol() && stream != null && stream.isReset();
        }

        private boolean isProtocol() {
            switch (frame.getType()) {
                case DATA:
                case HEADERS:
                case PUSH_PROMISE:
                case CONTINUATION:
                    return false;
                case PRIORITY:
                case RST_STREAM:
                case SETTINGS:
                case PING:
                case GO_AWAY:
                case WINDOW_UPDATE:
                case PREFACE:
                case DISCONNECT:
                    return true;
                default:
                    throw new IllegalStateException();
            }
        }

        @Override
        public String toString() {
            return frame.toString();
        }
    }

    private class WindowEntry {
        private final StreamSPI stream;
        private final WindowUpdateFrame frame;

        public WindowEntry(StreamSPI stream, WindowUpdateFrame frame) {
            this.stream = stream;
            this.frame = frame;
        }

        public void perform() {
            FlowControlStrategy flowControl = session.getFlowControlStrategy();
            flowControl.onWindowUpdate(session, stream, frame);
        }
    }

}
