package com.firefly.server.http2.servlet.session;

import javax.servlet.http.HttpSession;
import javax.servlet.http.HttpSessionAttributeListener;
import javax.servlet.http.HttpSessionBindingEvent;
import javax.servlet.http.HttpSessionEvent;
import javax.servlet.http.HttpSessionListener;

import com.firefly.utils.lang.LifeCycle;

public interface HttpSessionManager extends LifeCycle {

	public boolean containsKey(String id);

	public HttpSession remove(String id);

	public HttpSession get(String id);

	public HttpSession create();

	public int size();
	
	public int getMaxSessionInactiveInterval();
	
	public void setMaxSessionInactiveInterval(int maxSessionInactiveInterval);
	
	public HttpSessionAttributeListener getHttpSessionAttributeListener();
	
	public void setHttpSessionAttributeListener(HttpSessionAttributeListener httpSessionAttributeListener);
	
	public HttpSessionListener getHttpSessionListener();
	
	public void setHttpSessionListener(HttpSessionListener httpSessionListener);
	
	
	public static class HttpSessionAttributeListenerAdapter implements HttpSessionAttributeListener {

		@Override
		public void attributeAdded(HttpSessionBindingEvent event) {
		}

		@Override
		public void attributeRemoved(HttpSessionBindingEvent event) {
		}

		@Override
		public void attributeReplaced(HttpSessionBindingEvent event) {
		}
		
	}
	
	public static class HttpSessionListenerAdapter implements HttpSessionListener {

		@Override
		public void sessionCreated(HttpSessionEvent se) {
		}

		@Override
		public void sessionDestroyed(HttpSessionEvent se) {
		}
		
	}
}
