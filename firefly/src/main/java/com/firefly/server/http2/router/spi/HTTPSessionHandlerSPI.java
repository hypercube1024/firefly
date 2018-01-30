package com.firefly.server.http2.router.spi;

import com.firefly.server.http2.router.HTTPSession;

import javax.servlet.http.HttpSession;
import java.util.concurrent.CompletableFuture;

/**
 * @author Pengtao Qiu
 */
public interface HTTPSessionHandlerSPI {

    CompletableFuture<HTTPSession> getSession();

    CompletableFuture<HTTPSession> getSession(boolean create);

    CompletableFuture<HTTPSession> getAndCreateSession(int maxAge);

    CompletableFuture<Integer> getSessionSize();

    CompletableFuture<Boolean> removeSession();

    CompletableFuture<Boolean> updateSession(HTTPSession httpSession);

    boolean isRequestedSessionIdFromURL();

    boolean isRequestedSessionIdFromCookie();

    String getRequestedSessionId();

    String getSessionIdParameterName();
}
