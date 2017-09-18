package com.firefly.server.http2.router.spi;

import javax.servlet.http.HttpSession;
import java.util.concurrent.CompletableFuture;

/**
 * @author Pengtao Qiu
 */
public interface HTTPSessionHandlerSPI {

    HttpSession getSession();

    HttpSession getSession(boolean create);

    CompletableFuture<AsynchronousHttpSession> getAsyncSession();

    CompletableFuture<AsynchronousHttpSession> getAsyncSession(boolean create);

    CompletableFuture<Integer> getAsyncSessionSize();

    boolean isRequestedSessionIdFromURL();

    boolean isRequestedSessionIdFromCookie();

    boolean isRequestedSessionIdValid();

    String getRequestedSessionId();

    int getSessionSize();

}
