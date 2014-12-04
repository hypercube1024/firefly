package com.firefly.server.session;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import javax.servlet.http.HttpSession;
import javax.servlet.http.HttpSessionEvent;

import com.firefly.server.http.Config;
import com.firefly.utils.time.HashTimeWheel;
import com.firefly.utils.time.Millisecond100Clock;

public class LocalHttpSessionManager implements HttpSessionManager {

	private ConcurrentHashMap<String, HttpSessionImpl> map = new ConcurrentHashMap<String, HttpSessionImpl>();
	private Config config;
	
	private static final HashTimeWheel timeWheel = new HashTimeWheel();
	static {
		timeWheel.start();
	}

	public LocalHttpSessionManager(Config config) {
		this.config = config;
	}

	@Override
	public boolean containsKey(String id) {
		return map.containsKey(id);
	}

	@Override
	public HttpSession remove(String id) {
		HttpSession session = map.remove(id);
		if (session != null)
			config.getHttpSessionListener().sessionDestroyed(new HttpSessionEvent(session));

		return session;
	}

	@Override
	public HttpSession get(String id) {
		return map.get(id);
	}

	@Override
	public HttpSession create() {
		String id = UUID.randomUUID().toString().replace("-", "");
		long timeout = config.getMaxSessionInactiveInterval() * 1000;
		HttpSessionImpl session = new HttpSessionImpl(this, id,
				Millisecond100Clock.currentTimeMillis(),
				config.getMaxSessionInactiveInterval());
		timeWheel.add(timeout, new TimeoutTask(session));
		map.put(id, session);
		config.getHttpSessionListener().sessionCreated(new HttpSessionEvent(session));
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
			long t = Millisecond100Clock.currentTimeMillis() - session.getLastAccessedTime();
			long timeout = session.getMaxInactiveInterval() * 1000;
			// System.out.println(timeout + "|" + t);
			if (t > timeout) {
				// System.out.println("removie session id: " + session.getId());
				remove(session.getId());
			} else {
				if (timeout > 0) {
					long nextCheckTime = timeout - t;
					timeWheel.add(nextCheckTime, TimeoutTask.this);
				} else if (timeout == 0) {
					remove(session.getId());
				}
			}
		}

	}

	@Override
	public Config getConfig() {
		return config;
	}

}
