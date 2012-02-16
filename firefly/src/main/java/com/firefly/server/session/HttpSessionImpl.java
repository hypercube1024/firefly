package com.firefly.server.session;

import java.util.Enumeration;
import java.util.concurrent.ConcurrentHashMap;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpSession;
import javax.servlet.http.HttpSessionBindingEvent;
import javax.servlet.http.HttpSessionContext;

import com.firefly.server.exception.HttpServerException;

@SuppressWarnings("deprecation")
public class HttpSessionImpl implements HttpSession {
	private static final String[] EMPTY_ARR = new String[0];
	private final HttpSessionManager sessionManager;
	private final String id;
	private final long creationTime;
	private volatile long lastAccessedTime;
	private volatile int maxInactiveInterval;
	private ConcurrentHashMap<String, Object> map = new ConcurrentHashMap<String, Object>();

	public HttpSessionImpl(HttpSessionManager sessionManager, String id,
			long creationTime, int maxInactiveInterval) {
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

	@Override
	public HttpSessionContext getSessionContext() {
		throw new HttpServerException("no implements this method!");
	}

	@Override
	public Object getAttribute(String name) {
		lastAccessedTime = com.firefly.net.Config.TIME_PROVIDER.currentTimeMillis();
		return map.get(name);
	}

	@Override
	public Object getValue(String name) {
		return getAttribute(name);
	}

	@Override
	public Enumeration<String> getAttributeNames() {
		lastAccessedTime = com.firefly.net.Config.TIME_PROVIDER.currentTimeMillis();
		return map.keys();
	}

	@Override
	public String[] getValueNames() {
		lastAccessedTime = com.firefly.net.Config.TIME_PROVIDER.currentTimeMillis();
		return map.keySet().toArray(EMPTY_ARR);
	}

	@Override
	public void setAttribute(String name, Object value) {
		lastAccessedTime = com.firefly.net.Config.TIME_PROVIDER.currentTimeMillis();
		Object v = map.put(name, value);
		if(v == null)
			sessionManager.getConfig().getHttpSessionAttributeListener().attributeAdded(new HttpSessionBindingEvent(this, name, value));
		else
			sessionManager.getConfig().getHttpSessionAttributeListener().attributeReplaced(new HttpSessionBindingEvent(this, name, value));
	}

	@Override
	public void putValue(String name, Object value) {
		setAttribute(name, value);
	}

	@Override
	public void removeAttribute(String name) {
		lastAccessedTime = com.firefly.net.Config.TIME_PROVIDER.currentTimeMillis();
		Object value = map.remove(name);
		if(value != null)
			sessionManager.getConfig().getHttpSessionAttributeListener().attributeRemoved(new HttpSessionBindingEvent(this, name, value));
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
