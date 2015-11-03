package com.firefly.server.http2;

import com.firefly.codec.http2.stream.Session;

/**
 * <p>
 * Server-side extension of {@link org.eclipse.jetty.http2.api.Session.Listener}
 * that exposes additional events.
 * </p>
 */
public interface ServerSessionListener extends Session.Listener {
	/**
	 * <p>
	 * Callback method invoked when a connection has been accepted by the
	 * server.
	 * </p>
	 * 
	 * @param session
	 *            the session
	 */
	public void onAccept(Session session);

	/**
	 * <p>
	 * Empty implementation of {@link ServerSessionListener}
	 * </p>
	 */
	public static class Adapter extends Session.Listener.Adapter implements ServerSessionListener {
		@Override
		public void onAccept(Session session) {
		}
	}
}
