package com.firefly.server.http2.router.handler.session;

import javax.servlet.http.HttpSession;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * @author Pengtao Qiu
 */
public class LocalSessionStore implements SessionStore {

    private final ConcurrentMap<String, HttpSession> sessionMap = new ConcurrentHashMap<>();

    @Override
    public HttpSession remove(String key) {
        return sessionMap.remove(key);
    }

    @Override
    public HttpSession put(String key, HttpSession value) {
        return sessionMap.put(key, value);
    }

    @Override
    public HttpSession get(String key) {
        return sessionMap.get(key);
    }

    @Override
    public int size() {
        return sessionMap.size();
    }
}
