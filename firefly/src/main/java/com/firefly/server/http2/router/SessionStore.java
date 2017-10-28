package com.firefly.server.http2.router;

import com.firefly.utils.lang.LifeCycle;

import java.util.concurrent.CompletableFuture;

/**
 * @author Pengtao Qiu
 */
public interface SessionStore extends LifeCycle {

    CompletableFuture<Boolean> remove(String key);

    CompletableFuture<Boolean> put(String key, HTTPSession value);

    CompletableFuture<HTTPSession> get(String key);

    CompletableFuture<Integer> size();

}
