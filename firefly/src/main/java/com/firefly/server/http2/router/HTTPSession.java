package com.firefly.server.http2.router;

import com.firefly.utils.time.Millisecond100Clock;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * @author Pengtao Qiu
 */
public class HTTPSession implements Serializable {

    private String id;
    private long creationTime;
    private long lastAccessedTime;
    private int maxInactiveInterval;
    private Map<String, Object> attributes;
    private boolean newSession;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public long getCreationTime() {
        return creationTime;
    }

    public void setCreationTime(long creationTime) {
        this.creationTime = creationTime;
    }

    public long getLastAccessedTime() {
        return lastAccessedTime;
    }

    public void setLastAccessedTime(long lastAccessedTime) {
        this.lastAccessedTime = lastAccessedTime;
    }

    /**
     * Get the max inactive interval. The time unit is second.
     *
     * @return The max inactive interval.
     */
    public int getMaxInactiveInterval() {
        return maxInactiveInterval;
    }

    /**
     * Set the max inactive interval. The time unit is second.
     *
     * @param maxInactiveInterval The max inactive interval.
     */
    public void setMaxInactiveInterval(int maxInactiveInterval) {
        this.maxInactiveInterval = maxInactiveInterval;
    }

    public Map<String, Object> getAttributes() {
        return attributes;
    }

    public void setAttributes(Map<String, Object> attributes) {
        this.attributes = attributes;
    }

    public boolean isNewSession() {
        return newSession;
    }

    public void setNewSession(boolean newSession) {
        this.newSession = newSession;
    }

    public boolean isInvalid() {
        long currentTime = Millisecond100Clock.currentTimeMillis();
        return (currentTime - lastAccessedTime) > (maxInactiveInterval * 1000);
    }

    public static HTTPSession create(String id, int maxInactiveInterval) {
        HTTPSession session = new HTTPSession();
        session.setId(id);
        session.setMaxInactiveInterval(maxInactiveInterval);
        session.setCreationTime(Millisecond100Clock.currentTimeMillis());
        session.setLastAccessedTime(session.getCreationTime());
        session.setAttributes(new HashMap<>());
        session.setNewSession(true);
        return session;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        HTTPSession that = (HTTPSession) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
