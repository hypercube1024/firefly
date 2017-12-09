package com.firefly.codec.http2.stream;

import com.firefly.codec.http2.frame.WindowUpdateFrame;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

public abstract class AbstractFlowControlStrategy implements FlowControlStrategy {
    protected static Logger log = LoggerFactory.getLogger("firefly-system");

    private final AtomicLong sessionStall = new AtomicLong();
    private final AtomicLong sessionStallTime = new AtomicLong();
    private final Map<StreamSPI, Long> streamsStalls = new ConcurrentHashMap<>();
    private final AtomicLong streamsStallTime = new AtomicLong();
    private int initialStreamSendWindow;
    private int initialStreamRecvWindow;

    public AbstractFlowControlStrategy(int initialStreamSendWindow) {
        this.initialStreamSendWindow = initialStreamSendWindow;
        this.initialStreamRecvWindow = DEFAULT_WINDOW_SIZE;
    }

    public int getInitialStreamSendWindow() {
        return initialStreamSendWindow;
    }

    public int getInitialStreamRecvWindow() {
        return initialStreamRecvWindow;
    }

    @Override
    public void onStreamCreated(StreamSPI stream) {
        stream.updateSendWindow(initialStreamSendWindow);
        stream.updateRecvWindow(initialStreamRecvWindow);
    }

    @Override
    public void onStreamDestroyed(StreamSPI stream) {
        streamsStalls.remove(stream);
    }

    @Override
    public void updateInitialStreamWindow(SessionSPI session, int initialStreamWindow, boolean local) {
        int previousInitialStreamWindow;
        if (local) {
            previousInitialStreamWindow = getInitialStreamRecvWindow();
            this.initialStreamRecvWindow = initialStreamWindow;
        } else {
            previousInitialStreamWindow = getInitialStreamSendWindow();
            this.initialStreamSendWindow = initialStreamWindow;
        }
        int delta = initialStreamWindow - previousInitialStreamWindow;

        // SPEC: updates of the initial window size only affect stream windows, not session's.
        for (Stream stream : session.getStreams()) {
            if (local) {
                ((StreamSPI) stream).updateRecvWindow(delta);
                if (log.isDebugEnabled())
                    log.debug("Updated initial stream recv window {} -> {} for {}", previousInitialStreamWindow, initialStreamWindow, stream);
            } else {
                session.onWindowUpdate((StreamSPI) stream, new WindowUpdateFrame(stream.getId(), delta));
            }
        }
    }

    @Override
    public void onWindowUpdate(SessionSPI session, StreamSPI stream, WindowUpdateFrame frame) {
        int delta = frame.getWindowDelta();
        if (frame.getStreamId() > 0) {
            // The stream may have been removed concurrently.
            if (stream != null) {
                int oldSize = stream.updateSendWindow(delta);
                if (log.isDebugEnabled())
                    log.debug("Updated stream send window {} -> {} for {}", oldSize, oldSize + delta, stream);
                if (oldSize <= 0)
                    onStreamUnstalled(stream);
            }
        } else {
            int oldSize = session.updateSendWindow(delta);
            if (log.isDebugEnabled())
                log.debug("Updated session send window {} -> {} for {}", oldSize, oldSize + delta, session);
            if (oldSize <= 0)
                onSessionUnstalled(session);
        }
    }

    @Override
    public void onDataReceived(SessionSPI session, StreamSPI stream, int length) {
        int oldSize = session.updateRecvWindow(-length);
        if (log.isDebugEnabled())
            log.debug("Data received, {} bytes, updated session recv window {} -> {} for {}", length, oldSize, oldSize - length, session);

        if (stream != null) {
            oldSize = stream.updateRecvWindow(-length);
            if (log.isDebugEnabled())
                log.debug("Data received, {} bytes, updated stream recv window {} -> {} for {}", length, oldSize, oldSize - length, stream);
        }
    }

    @Override
    public void windowUpdate(SessionSPI session, StreamSPI stream, WindowUpdateFrame frame) {
    }

    @Override
    public void onDataSending(StreamSPI stream, int length) {
        if (length == 0)
            return;

        SessionSPI session = stream.getSession();
        int oldSessionWindow = session.updateSendWindow(-length);
        int newSessionWindow = oldSessionWindow - length;
        if (log.isDebugEnabled())
            log.debug("Sending, session send window {} -> {} for {}", oldSessionWindow, newSessionWindow, session);
        if (newSessionWindow <= 0)
            onSessionStalled(session);

        int oldStreamWindow = stream.updateSendWindow(-length);
        int newStreamWindow = oldStreamWindow - length;
        if (log.isDebugEnabled())
            log.debug("Sending, stream send window {} -> {} for {}", oldStreamWindow, newStreamWindow, stream);
        if (newStreamWindow <= 0)
            onStreamStalled(stream);
    }

    @Override
    public void onDataSent(StreamSPI stream, int length) {
    }

    protected void onSessionStalled(SessionSPI session) {
        sessionStall.set(System.nanoTime());
        if (log.isDebugEnabled())
            log.debug("Session stalled {}", session);
    }

    protected void onStreamStalled(StreamSPI stream) {
        streamsStalls.put(stream, System.nanoTime());
        if (log.isDebugEnabled())
            log.debug("Stream stalled {}", stream);
    }

    protected void onSessionUnstalled(SessionSPI session) {
        sessionStallTime.addAndGet(System.nanoTime() - sessionStall.getAndSet(0));
        if (log.isDebugEnabled())
            log.debug("Session unstalled {}", session);
    }

    protected void onStreamUnstalled(StreamSPI stream) {
        Long time = streamsStalls.remove(stream);
        if (time != null)
            streamsStallTime.addAndGet(System.nanoTime() - time);
        if (log.isDebugEnabled())
            log.debug("Stream unstalled {}", stream);
    }

    public long getSessionStallTime() {
        long pastStallTime = sessionStallTime.get();
        long currentStallTime = sessionStall.get();
        if (currentStallTime != 0)
            currentStallTime = System.nanoTime() - currentStallTime;
        return TimeUnit.NANOSECONDS.toMillis(pastStallTime + currentStallTime);
    }

    public long getStreamsStallTime() {
        long pastStallTime = streamsStallTime.get();
        long now = System.nanoTime();
        long currentStallTime = streamsStalls.values().stream().reduce(0L, (result, time) -> now - time);
        return TimeUnit.NANOSECONDS.toMillis(pastStallTime + currentStallTime);
    }

    public void reset() {
        sessionStallTime.set(0);
        streamsStallTime.set(0);
    }
}
