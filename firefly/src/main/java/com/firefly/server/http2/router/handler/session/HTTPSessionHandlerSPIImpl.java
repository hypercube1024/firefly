package com.firefly.server.http2.router.handler.session;

import com.firefly.codec.http2.model.Cookie;
import com.firefly.server.http2.router.*;
import com.firefly.utils.StringUtils;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * @author Pengtao Qiu
 */
public class HTTPSessionHandlerSPIImpl extends AbstractHTTPSessionHandlerSPI {

    private final SessionStore sessionStore;

    public HTTPSessionHandlerSPIImpl(RoutingContext routingContext,
                                     SessionStore sessionStore,
                                     HTTPSessionConfiguration configuration) {
        super(routingContext, configuration.getSessionIdParameterName(), configuration.getDefaultMaxInactiveInterval());
        this.sessionStore = sessionStore;
    }


    @Override
    public CompletableFuture<HTTPSession> getSession() {
        return getSession(true);
    }

    @Override
    public CompletableFuture<HTTPSession> getSession(boolean create) {
        CompletableFuture<HTTPSession> ret = new CompletableFuture<>();
        sessionStore.get(getSessionId(create)).thenAccept(ret::complete).exceptionally(ex -> {
            if (create && Optional.ofNullable(ex)
                                  .map(Throwable::getCause)
                                  .filter(e -> e instanceof SessionNotFound || e instanceof SessionInvalidException)
                                  .isPresent()) {
                createSession(ret);
            } else {
                Optional.ofNullable(ex)
                        .map(Throwable::getCause)
                        .filter(e -> e instanceof SessionInvalidException)
                        .ifPresent(e -> removeCookie());
                ret.completeExceptionally(ex);
            }
            return null;
        });
        return ret;
    }

    private void removeCookie() {
        Cookie cookie = new Cookie(sessionIdParameterName, requestedSessionId);
        cookie.setMaxAge(0);
        routingContext.addCookie(cookie);
    }


    @Override
    public CompletableFuture<Integer> getSessionSize() {
        return sessionStore.size();
    }

    @Override
    public CompletableFuture<Boolean> removeSession() {
        CompletableFuture<Boolean> ret = new CompletableFuture<>();
        sessionStore.remove(requestedSessionId).thenAccept(success -> {
            removeCookie();
            ret.complete(success);
        }).exceptionally(ex -> {
            ret.completeExceptionally(ex);
            return null;
        });
        return ret;
    }

    @Override
    public CompletableFuture<Boolean> updateSession(HTTPSession httpSession) {
        return sessionStore.put(requestedSessionId, httpSession);
    }

    protected String getSessionId(boolean create) {
        if (create && !StringUtils.hasText(requestedSessionId)) {
            requestedSessionId = UUID.randomUUID().toString().replace("-", "");
        }
        return requestedSessionId;
    }

    protected void createSession(CompletableFuture<HTTPSession> ret) {
        HTTPSession newSession = HTTPSession.create(requestedSessionId, defaultMaxInactiveInterval);
        sessionStore.put(newSession.getId(), newSession)
                    .thenAccept(r -> {
                        createCookie();
                        ret.complete(newSession);
                    })
                    .exceptionally(ex -> {
                        ret.completeExceptionally(ex);
                        return null;
                    });
    }

    private void createCookie() {
        Cookie cookie = new Cookie(sessionIdParameterName, requestedSessionId);
        routingContext.addCookie(cookie);
    }
}
