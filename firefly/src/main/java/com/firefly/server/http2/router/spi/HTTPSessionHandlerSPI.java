package com.firefly.server.http2.router.spi;

import javax.servlet.http.HttpSession;

/**
 * @author Pengtao Qiu
 */
public interface HTTPSessionHandlerSPI {

    HttpSession getSession();

    HttpSession getSession(boolean create);

    boolean isRequestedSessionIdFromURL();

    boolean isRequestedSessionIdFromCookie();

    boolean isRequestedSessionIdValid();

    String getRequestedSessionId();

    int getSessionSize();

}
