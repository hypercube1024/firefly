package com.firefly.server.http2.servlet.session;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import javax.servlet.http.HttpSession;
import javax.servlet.http.HttpSessionAttributeListener;
import javax.servlet.http.HttpSessionEvent;
import javax.servlet.http.HttpSessionListener;

import com.firefly.utils.log.Log;
import com.firefly.utils.log.LogFactory;
import com.firefly.utils.time.HashTimeWheel;
import com.firefly.utils.time.Millisecond100Clock;

public class LocalHttpSessionManager implements HttpSessionManager {

	private static Log log = LogFactory.getInstance().getLog("firefly-system");

	private ConcurrentHashMap<String, HttpSessionImpl> map = new ConcurrentHashMap<String, HttpSessionImpl>();

	private int maxSessionInactiveInterval = 10 * 60;
	private HttpSessionAttributeListener httpSessionAttributeListener = new HttpSessionAttributeListenerAdapter();
	private HttpSessionListener httpSessionListener = new HttpSessionListenerAdapter();

	private static final HashTimeWheel timeWheel = new HashTimeWheel();

	static {
		timeWheel.start();
	}

	@Override
	public boolean containsKey(String id) {
		return map.containsKey(id);
	}

	@Override
	public HttpSession remove(String id) {
		HttpSession session = map.remove(id);
		if (session != null)
			httpSessionListener.sessionDestroyed(new HttpSessionEvent(session));

		return session;
	}

	@Override
	public HttpSession get(String id) {
		return map.get(id);
	}

	@Override
	public HttpSession create() {
		String id = UUID.randomUUID().toString().replace("-", "");
		long timeout = maxSessionInactiveInterval * 1000;
		HttpSessionImpl session = new HttpSessionImpl(this, id, Millisecond100Clock.currentTimeMillis(),
				maxSessionInactiveInterval);
		timeWheel.add(timeout, new TimeoutTask(session));
		map.put(id, session);
		httpSessionListener.sessionCreated(new HttpSessionEvent(session));
		return session;
	}

	@Override
	public int size() {
		return map.size();
	}

	private class TimeoutTask implements Runnable {
		private HttpSessionImpl session;

		public TimeoutTask(HttpSessionImpl session) {
			this.session = session;
		}

		@Override
		public void run() {
			long timeDifference = Millisecond100Clock.currentTimeMillis() - session.getLastAccessedTime();
			long timeout = session.getMaxInactiveInterval() * 1000;
			log.debug("the local session timeout is {}, the time difference is {}", timeout, timeDifference);
			if (timeDifference > timeout) {
				log.debug("removes local session {}", session.getId());
				remove(session.getId());
			} else {
				if (timeout > 0) {
					long nextCheckTime = timeout - timeDifference;
					timeWheel.add(nextCheckTime, TimeoutTask.this);
				} else if (timeout == 0) {
					remove(session.getId());
				}
			}
		}

	}

	@Override
	public int getMaxSessionInactiveInterval() {
		return maxSessionInactiveInterval;
	}

	@Override
	public void setMaxSessionInactiveInterval(int maxSessionInactiveInterval) {
		this.maxSessionInactiveInterval = maxSessionInactiveInterval;
	}

	@Override
	public HttpSessionAttributeListener getHttpSessionAttributeListener() {
		return httpSessionAttributeListener;
	}

	@Override
	public void setHttpSessionAttributeListener(HttpSessionAttributeListener httpSessionAttributeListener) {
		this.httpSessionAttributeListener = httpSessionAttributeListener;
	}

	@Override
	public HttpSessionListener getHttpSessionListener() {
		return httpSessionListener;
	}

	@Override
	public void setHttpSessionListener(HttpSessionListener httpSessionListener) {
		this.httpSessionListener = httpSessionListener;
	}

}
