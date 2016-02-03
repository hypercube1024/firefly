package com.firefly.server.http2.servlet.session;

import java.util.Enumeration;
import java.util.concurrent.ConcurrentHashMap;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpSession;
import javax.servlet.http.HttpSessionBindingEvent;

import com.firefly.server.exception.HttpServerException;
import com.firefly.utils.time.Millisecond100Clock;

public class HttpSessionImpl implements HttpSession {

	private static final String[] EMPTY_ARR = new String[0];

	private final HttpSessionManager sessionManager;
	private final String id;
	private final long creationTime;
	private volatile long lastAccessedTime;
	private volatile int maxInactiveInterval;
	private ConcurrentHashMap<String, Object> map = new ConcurrentHashMap<String, Object>();

	public HttpSessionImpl(HttpSessionManager sessionManager, String id, long creationTime, int maxInactiveInterval) {
		super();
		this.sessionManager = sessionManager;
		this.id = id;
		this.creationTime = creationTime;
		this.maxInactiveInterval = maxInactiveInterval;
	}

	@Override
	public long getCreationTime() {
		return creationTime;
	}

	@Override
	public String getId() {
		return id;
	}

	@Override
	public long getLastAccessedTime() {
		return lastAccessedTime;
	}

	@Override
	public ServletContext getServletContext() {
		throw new HttpServerException("no implements this method!");
	}

	@Override
	public void setMaxInactiveInterval(int interval) {
		maxInactiveInterval = interval;
	}

	@Override
	public int getMaxInactiveInterval() {
		return maxInactiveInterval;
	}

	@Deprecated
	@Override
	public javax.servlet.http.HttpSessionContext getSessionContext() {
		throw new HttpServerException("no implements this method!");
	}

	@Override
	public Object getAttribute(String name) {
		lastAccessedTime = Millisecond100Clock.currentTimeMillis();
		return map.get(name);
	}

	@Override
	public Object getValue(String name) {
		return getAttribute(name);
	}

	@Override
	public Enumeration<String> getAttributeNames() {
		lastAccessedTime = Millisecond100Clock.currentTimeMillis();
		return map.keys();
	}

	@Override
	public String[] getValueNames() {
		lastAccessedTime = Millisecond100Clock.currentTimeMillis();
		return map.keySet().toArray(EMPTY_ARR);
	}

	@Override
	public void setAttribute(String name, Object value) {
		lastAccessedTime = Millisecond100Clock.currentTimeMillis();
		Object v = map.put(name, value);
		if (v == null)
			sessionManager.getHttpSessionAttributeListener()
					.attributeAdded(new HttpSessionBindingEvent(this, name, value));
		else
			sessionManager.getHttpSessionAttributeListener()
					.attributeReplaced(new HttpSessionBindingEvent(this, name, value));
	}

	@Override
	public void putValue(String name, Object value) {
		setAttribute(name, value);
	}

	@Override
	public void removeAttribute(String name) {
		lastAccessedTime = Millisecond100Clock.currentTimeMillis();
		Object value = map.remove(name);
		if (value != null)
			sessionManager.getHttpSessionAttributeListener()
					.attributeRemoved(new HttpSessionBindingEvent(this, name, value));
	}

	@Override
	public void removeValue(String name) {
		removeAttribute(name);
	}

	@Override
	public void invalidate() {
		sessionManager.remove(id);
	}

	@Override
	public boolean isNew() {
		return lastAccessedTime > 0;
	}
}
