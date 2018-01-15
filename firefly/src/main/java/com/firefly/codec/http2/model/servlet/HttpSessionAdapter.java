package com.firefly.codec.http2.model.servlet;

import com.firefly.server.http2.router.HTTPSession;
import com.firefly.utils.StringUtils;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpSession;
import javax.servlet.http.HttpSessionContext;
import java.util.Collections;
import java.util.Enumeration;
import java.util.Map;

/**
 * @author Pengtao Qiu
 */
public class HttpSessionAdapter implements HttpSession {
    protected final HTTPSession httpSession;

    public HttpSessionAdapter(HTTPSession httpSession) {
        this.httpSession = httpSession;
    }

    @Override
    public String getId() {
        return httpSession.getId();
    }

    public void setId(String id) {
        httpSession.setId(id);
    }

    @Override
    public long getCreationTime() {
        return httpSession.getCreationTime();
    }

    public void setCreationTime(long creationTime) {
        httpSession.setCreationTime(creationTime);
    }

    @Override
    public long getLastAccessedTime() {
        return httpSession.getLastAccessedTime();
    }

    @Override
    public ServletContext getServletContext() {
        return null;
    }

    public void setLastAccessedTime(long lastAccessedTime) {
        httpSession.setLastAccessedTime(lastAccessedTime);
    }

    @Override
    public int getMaxInactiveInterval() {
        return httpSession.getMaxInactiveInterval();
    }

    @Override
    public HttpSessionContext getSessionContext() {
        return null;
    }

    @Override
    public Object getAttribute(String name) {
        return getAttributes().get(name);
    }

    @Override
    public Object getValue(String name) {
        return getAttributes().get(name);
    }

    @Override
    public Enumeration<String> getAttributeNames() {
        return Collections.enumeration(getAttributes().keySet());
    }

    @Override
    public String[] getValueNames() {
        return getAttributes().keySet().toArray(StringUtils.EMPTY_STRING_ARRAY);
    }

    @Override
    public void setAttribute(String name, Object value) {
        httpSession.getAttributes().put(name, value);
    }

    @Override
    public void putValue(String name, Object value) {
        httpSession.getAttributes().put(name, value);
    }

    @Override
    public void removeAttribute(String name) {
        httpSession.getAttributes().remove(name);
    }

    @Override
    public void removeValue(String name) {
        httpSession.getAttributes().remove(name);
    }

    @Override
    public void invalidate() {
        httpSession.setMaxInactiveInterval(0);
    }

    @Override
    public boolean isNew() {
        return httpSession.isNewSession();
    }

    @Override
    public void setMaxInactiveInterval(int maxInactiveInterval) {
        httpSession.setMaxInactiveInterval(maxInactiveInterval);
    }

    public Map<String, Object> getAttributes() {
        return httpSession.getAttributes();
    }
}
