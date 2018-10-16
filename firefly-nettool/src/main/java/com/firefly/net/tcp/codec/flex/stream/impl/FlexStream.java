package com.firefly.net.tcp.codec.flex.stream.impl;

import com.firefly.net.tcp.codec.flex.protocol.ControlFrame;
import com.firefly.net.tcp.codec.flex.protocol.DataFrame;
import com.firefly.net.tcp.codec.flex.protocol.DisconnectionFrame;
import com.firefly.net.tcp.codec.flex.protocol.ErrorCode;
import com.firefly.net.tcp.codec.flex.stream.Session;
import com.firefly.net.tcp.codec.flex.stream.Stream;
import com.firefly.utils.Assert;
import com.firefly.utils.StringUtils;
import com.firefly.utils.concurrent.Callback;
import com.firefly.utils.concurrent.IdleTimeout;
import com.firefly.utils.concurrent.Scheduler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeoutException;

/**
 * @author Pengtao Qiu
 */
public class FlexStream extends IdleTimeout implements Stream {

    protected static final Logger log = LoggerFactory.getLogger("firefly-system");

    protected final int id;
    protected final Session session;
    protected final LazyContextAttribute attribute = new LazyContextAttribute();

    protected volatile boolean committed;
    protected volatile Listener listener;
    protected volatile State state;

    public FlexStream(int id, Session session, Listener listener, State state, boolean committed, Scheduler scheduler) {
        super(scheduler);
        this.id = id;
        this.session = session;
        this.listener = listener;
        this.state = state;
        this.committed = committed;
    }

    @Override
    public int getId() {
        return id;
    }

    @Override
    public CompletableFuture<Boolean> send(ControlFrame controlFrame) {
        return session.sendFrame(controlFrame);
    }

    @Override
    public CompletableFuture<Boolean> send(DataFrame dataFrame) {
        return session.sendFrame(dataFrame);
    }

    @Override
    public void send(ControlFrame controlFrame, Callback callback) {
        session.sendFrame(controlFrame, callback);
    }

    @Override
    public void send(DataFrame dataFrame, Callback callback) {
        session.sendFrame(dataFrame, callback);
    }

    @Override
    public void setListener(Listener listener) {
        Assert.notNull(listener, "The stream listener must be not null");
        this.listener = listener;
    }

    public Listener getListener() {
        return listener;
    }

    @Override
    public State getState() {
        return state;
    }

    public void setState(State state) {
        this.state = state;
    }

    @Override
    public Session getSession() {
        return session;
    }

    @Override
    public boolean isCommitted() {
        return committed;
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

    @Override
    public String toString() {
        return "FlexStream{" +
                "id=" + id +
                ", committed=" + committed +
                ", state=" + state +
                '}';
    }

    @Override
    protected void onIdleExpired(TimeoutException timeout) {
        String err = StringUtils.replace("Idle timeout {}ms expired on {}", getIdleTimeout(), this.toString());
        log.error(err);

        if (isOpen()) {
            session.disconnect(new DisconnectionFrame(ErrorCode.INTERNAL.getValue(), err.getBytes(StandardCharsets.UTF_8)));
            FlexSession flexSession = (FlexSession) session;
            setState(State.CLOSED);
            flexSession.notifyCloseStream(this);
        }
    }

    @Override
    public boolean isOpen() {
        return state != State.CLOSED;
    }
}
