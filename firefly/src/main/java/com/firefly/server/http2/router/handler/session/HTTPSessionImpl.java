package com.firefly.server.http2.router.handler.session;

import com.firefly.utils.exception.CommonRuntimeException;
import com.firefly.utils.time.Millisecond100Clock;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpSession;
import javax.servlet.http.HttpSessionContext;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * @author Pengtao Qiu
 */
public class HTTPSessionImpl implements HttpSession {

    private int maxInactiveInterval;
    private final ConcurrentMap<String, Object> attributes = new ConcurrentHashMap<>();
    private final long createTime;
    private final String id;

    private volatile boolean newSession;
    private volatile long lastAccessedTime;
    private volatile boolean invalid;

    public HTTPSessionImpl(String id) {
        this.id = id;
        createTime = Millisecond100Clock.currentTimeMillis();
        newSession = true;
        invalid = false;
        lastAccessedTime = createTime;
    }

    @Override
    public long getCreationTime() {
        return createTime;
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public long getLastAccessedTime() {
        return lastAccessedTime;
    }

    public void setLastAccessedTime(long lastAccessedTime) {
        this.lastAccessedTime = lastAccessedTime;
    }

    @Override
    public ServletContext getServletContext() {
        return null;
    }

    @Override
    public void setMaxInactiveInterval(int maxInactiveInterval) {
        this.maxInactiveInterval = maxInactiveInterval;
    }

    @Override
    public int getMaxInactiveInterval() {
        return maxInactiveInterval;
    }

    @Deprecated
    @Override
    public HttpSessionContext getSessionContext() {
        throw new CommonRuntimeException("not implement");
    }

    @Override
    public Object getAttribute(String name) {
        return attributes.get(name);
    }

    @Deprecated
    @Override
    public Object getValue(String name) {
        throw new CommonRuntimeException("not implement");
    }

    @Override
    public Enumeration<String> getAttributeNames() {
        return new Enumeration<String>() {

            final Iterator<String> iterator = attributes.keySet().iterator();

            @Override
            public boolean hasMoreElements() {
                return iterator.hasNext();
            }

            @Override
            public String nextElement() {
                return iterator.next();
            }
        };
    }

    @Deprecated
    @Override
    public String[] getValueNames() {
        throw new CommonRuntimeException("not implement");
    }

    @Override
    public void setAttribute(String name, Object value) {
        attributes.put(name, value);
    }

    @Deprecated
    @Override
    public void putValue(String name, Object value) {
        throw new CommonRuntimeException("not implement");
    }

    @Override
    public void removeAttribute(String name) {
        attributes.remove(name);
    }

    @Deprecated
    @Override
    public void removeValue(String name) {
        throw new CommonRuntimeException("not implement");
    }

    @Override
    public void invalidate() {
        invalid = true;
    }

    public boolean isInvalid() {
        return invalid;
    }

    @Override
    public boolean isNew() {
        return newSession;
    }

    public void setNewSession(boolean newSession) {
        this.newSession = newSession;
    }
}
