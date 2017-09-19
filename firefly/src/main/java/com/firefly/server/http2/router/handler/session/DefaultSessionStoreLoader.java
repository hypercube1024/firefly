package com.firefly.server.http2.router.handler.session;

import com.firefly.server.http2.router.SessionStore;
import com.firefly.utils.ServiceUtils;

/**
 * @author Pengtao Qiu
 */
public class DefaultSessionStoreLoader {
    private static DefaultSessionStoreLoader ourInstance = new DefaultSessionStoreLoader();

    public static DefaultSessionStoreLoader getInstance() {
        return ourInstance;
    }

    private final SessionStore sessionStore;

    private DefaultSessionStoreLoader() {
        sessionStore = ServiceUtils.loadService(SessionStore.class, new LocalSessionStore());
    }

    public SessionStore getSessionStore() {
        return sessionStore;
    }
}
