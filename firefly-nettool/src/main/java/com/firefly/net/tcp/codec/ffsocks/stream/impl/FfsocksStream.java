package com.firefly.net.tcp.codec.ffsocks.stream.impl;

import com.firefly.net.tcp.codec.ffsocks.protocol.ControlFrame;
import com.firefly.net.tcp.codec.ffsocks.protocol.DataFrame;
import com.firefly.net.tcp.codec.ffsocks.stream.Session;
import com.firefly.net.tcp.codec.ffsocks.stream.Stream;
import com.firefly.utils.Assert;
import com.firefly.utils.concurrent.Callback;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * @author Pengtao Qiu
 */
public class FfsocksStream implements Stream {

    protected final int id;
    protected final Session session;
    protected final LazyContextAttribute attribute = new LazyContextAttribute();

    protected volatile boolean committed;
    protected volatile Listener listener;
    protected volatile State state;

    public FfsocksStream(int id, Session session, Listener listener, State state, boolean committed) {
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
}
