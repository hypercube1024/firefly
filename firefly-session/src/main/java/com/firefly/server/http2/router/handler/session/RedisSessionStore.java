package com.firefly.server.http2.router.handler.session;

import com.firefly.server.http2.router.HTTPSession;
import com.firefly.server.http2.router.SessionInvalidException;
import com.firefly.server.http2.router.SessionNotFound;
import com.firefly.server.http2.router.SessionStore;
import com.firefly.utils.Assert;
import com.firefly.utils.lang.AbstractLifeCycle;
import com.firefly.utils.time.Millisecond100Clock;
import org.redisson.api.RMapCacheReactive;
import org.redisson.api.RedissonReactiveClient;
import org.redisson.codec.FstCodec;
import reactor.core.publisher.Mono;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import static com.firefly.reactive.adapter.common.ReactiveUtils.toFuture;

/**
 * @author Pengtao Qiu
 */
public class RedisSessionStore extends AbstractLifeCycle implements SessionStore {

    private RedissonReactiveClient client;
    private String keyPrefix;
    private String sessionKey = "firefly_redis_session";
    private RMapCacheReactive<String, HTTPSession> map;
    private int ttl = 12 * 60 * 60;

    public RedissonReactiveClient getClient() {
        return client;
    }

    public void setClient(RedissonReactiveClient client) {
        this.client = client;
    }

    public String getKeyPrefix() {
        return keyPrefix;
    }

    public void setKeyPrefix(String keyPrefix) {
        this.keyPrefix = keyPrefix;
    }

    public String getSessionKey() {
        return sessionKey;
    }

    public void setSessionKey(String sessionKey) {
        this.sessionKey = sessionKey;
    }

    public int getTtl() {
        return ttl;
    }

    public void setTtl(int ttl) {
        this.ttl = ttl;
    }

    @Override
    public CompletableFuture<Boolean> remove(String key) {
        return Mono.from(map.fastRemove(key)).map(i -> i > 0).toFuture();
    }

    @Override
    public CompletableFuture<Boolean> put(String key, HTTPSession value) {
        return toFuture(map.fastPut(key, value, ttl, TimeUnit.SECONDS, value.getMaxInactiveInterval(), TimeUnit.SECONDS));
    }

    @Override
    public CompletableFuture<HTTPSession> get(String key) {
        return toFuture(map.get(key)).thenCompose(session -> {
            CompletableFuture<HTTPSession> future = new CompletableFuture<>();
            if (session == null) {
                future.completeExceptionally(new SessionNotFound());
            } else {
                if (session.isInvalid()) {
                    map.fastRemove(session.getId());
                    future.completeExceptionally(new SessionInvalidException("the session is expired"));
                } else {
                    session.setLastAccessedTime(Millisecond100Clock.currentTimeMillis());
                    session.setNewSession(false);
                    map.fastPut(session.getId(), session);
                    future.complete(session);
                }
            }
            return future;
        });
    }

    @Override
    public CompletableFuture<Integer> size() {
        return toFuture(map.size());
    }

    @Override
    protected void init() {
        Assert.notNull(client);
        Assert.hasText(keyPrefix);
        Assert.hasText(sessionKey);
        map = client.getMapCache(keyPrefix + sessionKey, new FstCodec());
    }

    @Override
    protected void destroy() {
        client.shutdown();
    }
}
