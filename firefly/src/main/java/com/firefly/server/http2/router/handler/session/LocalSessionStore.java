package com.firefly.server.http2.router.handler.session;

import com.firefly.server.http2.router.HTTPSession;
import com.firefly.server.http2.router.SessionInvalidException;
import com.firefly.server.http2.router.SessionNotFound;
import com.firefly.server.http2.router.SessionStore;
import com.firefly.utils.StringUtils;
import com.firefly.utils.concurrent.Scheduler;
import com.firefly.utils.concurrent.Schedulers;
import com.firefly.utils.lang.AbstractLifeCycle;
import com.firefly.utils.time.Millisecond100Clock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeUnit;

/**
 * @author Pengtao Qiu
 */
public class LocalSessionStore extends AbstractLifeCycle implements SessionStore {

    private static Logger log = LoggerFactory.getLogger("firefly-system");

    private final ConcurrentMap<String, HTTPSession> map = new ConcurrentHashMap<>();
    private final Scheduler scheduler = Schedulers.createScheduler();

    public LocalSessionStore() {
        start();
    }

    @Override
    public CompletableFuture<Boolean> remove(String key) {
        if (key != null) {
            map.remove(key);
        }
        return success();
    }

    @Override
    public CompletableFuture<Boolean> put(String key, HTTPSession value) {
        if (key != null && value != null) {
            if (!value.isNewSession()) {
                value.setLastAccessedTime(Millisecond100Clock.currentTimeMillis());
            }
            map.put(key, value);
        }
        return success();
    }

    @Override
    public CompletableFuture<HTTPSession> get(String key) {
        CompletableFuture<HTTPSession> ret = new CompletableFuture<>();
        if (!StringUtils.hasText(key)) {
            ret.completeExceptionally(new SessionNotFound());
            return ret;
        }

        HTTPSession session = map.get(key);
        if (session == null) {
            ret.completeExceptionally(new SessionNotFound());
        } else {
            if (session.isInvalid()) {
                map.remove(session.getId());
                ret.completeExceptionally(new SessionInvalidException("the session is expired"));
            } else {
                session.setLastAccessedTime(Millisecond100Clock.currentTimeMillis());
                session.setNewSession(false);
                ret.complete(session);
            }
        }
        return ret;
    }

    @Override
    public CompletableFuture<Integer> size() {
        CompletableFuture<Integer> ret = new CompletableFuture<>();
        ret.complete(map.size());
        return ret;
    }

    private CompletableFuture<Boolean> success() {
        CompletableFuture<Boolean> ret = new CompletableFuture<>();
        ret.complete(true);
        return ret;
    }

    @Override
    protected void init() {
        scheduler.scheduleWithFixedDelay(() -> map.forEach((id, session) -> {
            if (session.isInvalid()) {
                map.remove(id);
                log.info("remove expired local HTTP session -> {}", id);
            }
        }), 1, 1, TimeUnit.SECONDS);
    }

    @Override
    protected void destroy() {
        scheduler.stop();
    }
}
