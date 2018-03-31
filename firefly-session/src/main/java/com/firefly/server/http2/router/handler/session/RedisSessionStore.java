package com.firefly.server.http2.router.handler.session;

import com.firefly.server.http2.router.HTTPSession;
import com.firefly.server.http2.router.SessionInvalidException;
import com.firefly.server.http2.router.SessionNotFound;
import com.firefly.server.http2.router.SessionStore;
import com.firefly.utils.Assert;
import com.firefly.utils.StringUtils;
import com.firefly.utils.lang.AbstractLifeCycle;
import com.firefly.utils.time.Millisecond100Clock;
import org.redisson.api.RMapCacheReactive;
import org.redisson.api.RedissonReactiveClient;
import org.redisson.codec.FstCodec;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Mono;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import static com.firefly.reactive.adapter.common.ReactiveUtils.toFuture;

/**
 * @author Pengtao Qiu
 */
public class RedisSessionStore extends AbstractLifeCycle implements SessionStore {

    private static final Logger log = LoggerFactory.getLogger("firefly-system");

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
        if (StringUtils.hasText(key)) {
            return Mono.from(map.fastRemove(key)).map(i -> i > 0).toFuture();
        } else {
            CompletableFuture<Boolean> ret = new CompletableFuture<>();
            ret.complete(true);
            return ret;
        }
    }

    @Override
    public CompletableFuture<Boolean> put(String key, HTTPSession value) {
        value.setLastAccessedTime(Millisecond100Clock.currentTimeMillis());
        return toFuture(map.fastPut(key, value, ttl, TimeUnit.SECONDS, value.getMaxInactiveInterval(), TimeUnit.SECONDS));
    }

    @Override
    public CompletableFuture<HTTPSession> get(String key) {
        if (!StringUtils.hasText(key)) {
            CompletableFuture<HTTPSession> ret = new CompletableFuture<>();
            ret.completeExceptionally(new SessionNotFound());
            return ret;
        }
        return toFuture(map.get(key)).thenCompose(session -> {
            CompletableFuture<HTTPSession> future = new CompletableFuture<>();
            if (session == null) {
                future.completeExceptionally(new SessionNotFound());
            } else {
                session.setLastAccessedTime(Millisecond100Clock.currentTimeMillis());
                if (session.isNewSession()) {
                    session.setNewSession(false);
                    Mono.from(map.fastPut(session.getId(), session))
                        .subscribe(success -> {
                            log.debug("Get the new session success. {}", session.getId());
                            future.complete(session);
                        }, future::completeExceptionally);
                } else {
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
        map = client.getMapCache(keyPrefix + ":" + sessionKey, new FstCodec());
    }

    @Override
    protected void destroy() {
        client.shutdown();
    }
}
