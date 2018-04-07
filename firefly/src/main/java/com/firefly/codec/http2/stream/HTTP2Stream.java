package com.firefly.codec.http2.stream;

import com.firefly.codec.http2.frame.*;
import com.firefly.utils.concurrent.Callback;
import com.firefly.utils.concurrent.IdleTimeout;
import com.firefly.utils.concurrent.Promise;
import com.firefly.utils.concurrent.Scheduler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.EOFException;
import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

public class HTTP2Stream extends IdleTimeout implements StreamSPI {

    private static Logger log = LoggerFactory.getLogger("firefly-system");

    private final AtomicReference<ConcurrentMap<String, Object>> attributes = new AtomicReference<>();
    private final AtomicReference<CloseState> closeState = new AtomicReference<>(CloseState.NOT_CLOSED);
    private final AtomicInteger sendWindow = new AtomicInteger();
    private final AtomicInteger recvWindow = new AtomicInteger();
    private final SessionSPI session;
    private final int streamId;
    private final boolean local;
    private volatile Listener listener;
    private volatile boolean localReset;
    private volatile boolean remoteReset;

    public HTTP2Stream(Scheduler scheduler, SessionSPI session, int streamId, boolean local) {
        super(scheduler);
        this.session = session;
        this.streamId = streamId;
        this.local = local;
    }

    @Override
    public int getId() {
        return streamId;
    }

    @Override
    public boolean isLocal() {
        return local;
    }

    @Override
    public SessionSPI getSession() {
        return session;
    }

    @Override
    public void headers(HeadersFrame frame, Callback callback) {
        session.frames(this, callback, frame, Frame.EMPTY_ARRAY);
    }

    @Override
    public void push(PushPromiseFrame frame, Promise<Stream> promise, Listener listener) {
        session.push(this, promise, frame, listener);
    }

    @Override
    public void data(DataFrame frame, Callback callback) {
        session.data(this, callback, frame);
    }

    @Override
    public void reset(ResetFrame frame, Callback callback) {
        if (isReset())
            return;
        localReset = true;
        session.frames(this, callback, frame, Frame.EMPTY_ARRAY);
    }

    @Override
    public Object getAttribute(String key) {
        return attributes().get(key);
    }

    @Override
    public void setAttribute(String key, Object value) {
        attributes().put(key, value);
    }

    @Override
    public Object removeAttribute(String key) {
        return attributes().remove(key);
    }

    @Override
    public boolean isReset() {
        return localReset || remoteReset;
    }

    @Override
    public boolean isClosed() {
        return closeState.get() == CloseState.CLOSED;
    }

    @Override
    public boolean isRemotelyClosed() {
        return closeState.get() == CloseState.REMOTELY_CLOSED;
    }

    public boolean isLocallyClosed() {
        return closeState.get() == CloseState.LOCALLY_CLOSED;
    }

    @Override
    public boolean isOpen() {
        return !isClosed();
    }

    @Override
    protected void onIdleExpired(TimeoutException timeout) {
        if (log.isDebugEnabled()) {
            log.debug("Idle timeout {}ms expired on {}", getIdleTimeout(), this.toString());
        }

        // Notify the application.
        if (notifyIdleTimeout(this, timeout)) {
            // Tell the other peer that we timed out.
            reset(new ResetFrame(getId(), ErrorCode.CANCEL_STREAM_ERROR.code), Callback.NOOP);
        }
    }

    private ConcurrentMap<String, Object> attributes() {
        ConcurrentMap<String, Object> map = attributes.get();
        if (map == null) {
            map = new ConcurrentHashMap<>();
            if (!attributes.compareAndSet(null, map)) {
                map = attributes.get();
            }
        }
        return map;
    }

    @Override
    public Listener getListener() {
        return listener;
    }

    @Override
    public void setListener(Listener listener) {
        this.listener = listener;
    }

    @Override
    public void process(Frame frame, Callback callback) {
        notIdle();
        switch (frame.getType()) {
            case HEADERS: {
                onHeaders((HeadersFrame) frame, callback);
                break;
            }
            case DATA: {
                onData((DataFrame) frame, callback);
                break;
            }
            case RST_STREAM: {
                onReset((ResetFrame) frame, callback);
                break;
            }
            case PUSH_PROMISE: {
                onPush((PushPromiseFrame) frame, callback);
                break;
            }
            case WINDOW_UPDATE: {
                onWindowUpdate((WindowUpdateFrame) frame, callback);
                break;
            }
            default: {
                throw new UnsupportedOperationException();
            }
        }
    }

    private void onHeaders(HeadersFrame frame, Callback callback) {
        if (updateClose(frame.isEndStream(), CloseState.Event.RECEIVED))
            session.removeStream(this);
        callback.succeeded();
    }

    private void onData(DataFrame frame, Callback callback) {
        if (getRecvWindow() < 0) {
            // It's a bad client, it does not deserve to be
            // treated gently by just resetting the stream.
            session.close(ErrorCode.FLOW_CONTROL_ERROR.code, "stream_window_exceeded", Callback.NOOP);
            callback.failed(new IOException("stream_window_exceeded"));
            return;
        }

        // SPEC: remotely closed streams must be replied with a reset.
        if (isRemotelyClosed()) {
            reset(new ResetFrame(streamId, ErrorCode.STREAM_CLOSED_ERROR.code), Callback.NOOP);
            callback.failed(new EOFException("stream_closed"));
            return;
        }

        if (isReset()) {
            // Just drop the frame.
            callback.failed(new IOException("stream_reset"));
            return;
        }

        if (updateClose(frame.isEndStream(), CloseState.Event.RECEIVED))
            session.removeStream(this);
        notifyData(this, frame, callback);
    }

    private void onReset(ResetFrame frame, Callback callback) {
        remoteReset = true;
        close();
        session.removeStream(this);
        notifyReset(this, frame, callback);
    }

    private void onPush(PushPromiseFrame frame, Callback callback) {
        // Pushed streams are implicitly locally closed.
        // They are closed when receiving an end-stream DATA frame.
        updateClose(true, CloseState.Event.AFTER_SEND);
        callback.succeeded();
    }

    private void onWindowUpdate(WindowUpdateFrame frame, Callback callback) {
        callback.succeeded();
    }

    @Override
    public boolean updateClose(boolean update, CloseState.Event event) {
        if (log.isDebugEnabled()) {
            log.debug("Update close for {} update={} event={}", this, update, event);
        }

        if (!update)
            return false;

        switch (event) {
            case RECEIVED:
                return updateCloseAfterReceived();
            case BEFORE_SEND:
                return updateCloseBeforeSend();
            case AFTER_SEND:
                return updateCloseAfterSend();
            default:
                return false;
        }
    }

    private boolean updateCloseAfterReceived() {
        while (true) {
            CloseState current = closeState.get();
            switch (current) {
                case NOT_CLOSED: {
                    if (closeState.compareAndSet(current, CloseState.REMOTELY_CLOSED))
                        return false;
                    break;
                }
                case LOCALLY_CLOSING: {
                    if (closeState.compareAndSet(current, CloseState.CLOSING)) {
                        updateStreamCount(0, 1);
                        return false;
                    }
                    break;
                }
                case LOCALLY_CLOSED: {
                    close();
                    return true;
                }
                default: {
                    return false;
                }
            }
        }
    }

    private boolean updateCloseBeforeSend() {
        while (true) {
            CloseState current = closeState.get();
            switch (current) {
                case NOT_CLOSED: {
                    if (closeState.compareAndSet(current, CloseState.LOCALLY_CLOSING))
                        return false;
                    break;
                }
                case REMOTELY_CLOSED: {
                    if (closeState.compareAndSet(current, CloseState.CLOSING)) {
                        updateStreamCount(0, 1);
                        return false;
                    }
                    break;
                }
                default: {
                    return false;
                }
            }
        }
    }

    private boolean updateCloseAfterSend() {
        while (true) {
            CloseState current = closeState.get();
            switch (current) {
                case NOT_CLOSED:
                case LOCALLY_CLOSING: {
                    if (closeState.compareAndSet(current, CloseState.LOCALLY_CLOSED))
                        return false;
                    break;
                }
                case REMOTELY_CLOSED:
                case CLOSING: {
                    close();
                    return true;
                }
                default: {
                    return false;
                }
            }
        }
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
    public void close() {
        CloseState oldState = closeState.getAndSet(CloseState.CLOSED);
        if (oldState != CloseState.CLOSED) {
            int deltaClosing = oldState == CloseState.CLOSING ? -1 : 0;
            updateStreamCount(-1, deltaClosing);
            onClose();
        }
    }

    private void updateStreamCount(int deltaStream, int deltaClosing) {
        ((HTTP2Session) session).updateStreamCount(isLocal(), deltaStream, deltaClosing);
    }

    private void notifyData(Stream stream, DataFrame frame, Callback callback) {
        final Listener listener = this.listener;
        if (listener == null)
            return;
        try {
            listener.onData(stream, frame, callback);
        } catch (Throwable x) {
            log.info("Failure while notifying listener " + listener, x);
        }
    }

    private void notifyReset(Stream stream, ResetFrame frame, Callback callback) {
        final Listener listener = this.listener;
        if (listener == null)
            return;
        try {
            listener.onReset(stream, frame, callback);
        } catch (Throwable x) {
            log.info("Failure while notifying listener " + listener, x);
        }
    }

    private boolean notifyIdleTimeout(Stream stream, Throwable failure) {
        Listener listener = this.listener;
        if (listener == null)
            return true;
        try {
            return listener.onIdleTimeout(stream, failure);
        } catch (Throwable x) {
            log.info("Failure while notifying listener " + listener, x);
            return true;
        }
    }

    @Override
    public String toString() {
        return String.format("%s@%x#%d{sendWindow=%s,recvWindow=%s,reset=%b,%s}", getClass().getSimpleName(),
                hashCode(), getId(), sendWindow, recvWindow, isReset(), closeState);
    }
}
