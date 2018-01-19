package com.firefly.server.http2.router.handler.session;

import com.firefly.server.http2.router.HTTPSession;
import com.firefly.server.http2.router.SessionStore;
import com.firefly.utils.lang.AbstractLifeCycle;

import java.util.concurrent.CompletableFuture;

/**
 * @author Pengtao Qiu
 */
public class RedisSessionStore extends AbstractLifeCycle implements SessionStore {
    @Override
    public CompletableFuture<Boolean> remove(String key) {
        return null;
    }

    @Override
    public CompletableFuture<Boolean> put(String key, HTTPSession value) {
        return null;
    }

    @Override
    public CompletableFuture<HTTPSession> get(String key) {
        return null;
    }

    @Override
    public CompletableFuture<Integer> size() {
        return null;
    }

    @Override
    protected void init() {

    }

    @Override
    protected void destroy() {

    }
}
