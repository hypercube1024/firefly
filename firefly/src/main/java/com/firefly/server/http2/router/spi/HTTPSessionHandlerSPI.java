package com.firefly.server.http2.router.spi;

import com.firefly.server.http2.router.HTTPSession;

import java.util.concurrent.CompletableFuture;

/**
 * @author Pengtao Qiu
 */
public interface HTTPSessionHandlerSPI {

    CompletableFuture<HTTPSession> getSessionById(String id);

    CompletableFuture<HTTPSession> getSession();

    CompletableFuture<HTTPSession> getSession(boolean create);

    CompletableFuture<HTTPSession> getAndCreateSession(int maxAge);

    CompletableFuture<HTTPSession> getAndCreateSession(int maxAge, String domain);

    CompletableFuture<HTTPSession> createSession(int maxAge);

    CompletableFuture<HTTPSession> createSession(int maxAge, String domain);

    CompletableFuture<Integer> getSessionSize();

    CompletableFuture<Boolean> removeSession();

    CompletableFuture<Boolean> removeSessionById(String id);

    CompletableFuture<Boolean> updateSession(HTTPSession httpSession);

    boolean isRequestedSessionIdFromURL();

    boolean isRequestedSessionIdFromCookie();

    String getRequestedSessionId();

    String getSessionIdParameterName();
}
