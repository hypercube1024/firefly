package com.firefly.server.http2;

import com.firefly.codec.http2.stream.Session;

public interface ServerSessionListener extends Session.Listener {
    /**
     * <p>
     * Callback method invoked when a connection has been accepted by the
     * server.
     * </p>
     *
     * @param session the session
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
