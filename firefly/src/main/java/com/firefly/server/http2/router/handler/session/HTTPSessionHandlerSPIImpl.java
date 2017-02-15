package com.firefly.server.http2.router.handler.session;

import com.firefly.codec.http2.model.Cookie;
import com.firefly.server.http2.router.RoutingContext;
import com.firefly.server.http2.router.spi.HTTPSessionHandlerSPI;
import com.firefly.utils.time.Millisecond100Clock;

import javax.servlet.http.HttpSession;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentMap;

/**
 * @author Pengtao Qiu
 */
public class HTTPSessionHandlerSPIImpl implements HTTPSessionHandlerSPI {

    private final LocalHTTPSessionConfiguration configuration;
    private final ConcurrentMap<String, HTTPSessionImpl> sessionMap;
    private final RoutingContext routingContext;
    private boolean requestedSessionIdFromURL;
    private boolean requestedSessionIdFromCookie;
    private String requestedSessionId;
    private HTTPSessionImpl httpSession;

    public HTTPSessionHandlerSPIImpl(ConcurrentMap<String, HTTPSessionImpl> sessionMap,
                                     RoutingContext routingContext,
                                     LocalHTTPSessionConfiguration configuration) {
        this.sessionMap = sessionMap;
        this.routingContext = routingContext;
        this.configuration = configuration;
        init();
    }

    private void init() {
        if (!getHttpSessionFromCookie()) {
            getHttpSessionFromURL();
        }
    }

    private boolean getHttpSessionFromURL() {
        String uri = routingContext.getURI().getPath();
        String prefix = configuration.getSessionIdPathParameterNamePrefix();
        if (prefix != null) {
            int s = uri.indexOf(prefix);
            if (s >= 0) {
                s += prefix.length();
                int i = s;
                while (i < uri.length()) {
                    char c = uri.charAt(i);
                    if (c == ';' || c == '#' || c == '?' || c == '/') {
                        break;
                    }
                    i++;
                }

                requestedSessionId = uri.substring(s, i);
                requestedSessionIdFromCookie = false;
                requestedSessionIdFromURL = true;
                initHttpSession();
                return true;
            } else {
                return false;
            }
        } else {
            return false;
        }
    }

    private boolean getHttpSessionFromCookie() {
        List<Cookie> cookies = routingContext.getCookies();
        if (cookies != null && !cookies.isEmpty()) {
            Optional<Cookie> optional = cookies.stream()
                                               .filter(c -> configuration.getSessionIdParameterName().equalsIgnoreCase(c.getName()))
                                               .findFirst();
            if (optional.isPresent()) {
                requestedSessionIdFromCookie = true;
                requestedSessionIdFromURL = false;
                requestedSessionId = optional.get().getValue();
                initHttpSession();
                return true;
            } else {
                return false;
            }
        } else {
            return false;
        }
    }


    private void initHttpSession() {
        httpSession = sessionMap.get(requestedSessionId);
        if (httpSession != null) {
            if (httpSession.isInvalid()) {
                httpSession = null;
                sessionMap.remove(requestedSessionId);
            } else {
                long currentTime = Millisecond100Clock.currentTimeMillis();
                if ((currentTime - httpSession.getLastAccessedTime()) > (httpSession.getMaxInactiveInterval() * 1000)) {
                    httpSession = null;
                    sessionMap.remove(requestedSessionId);
                } else {
                    httpSession.setLastAccessedTime(currentTime);
                    httpSession.setNewSession(false);
                }
            }
        }
    }

    @Override
    public HttpSession getSession() {
        return getSession(false);
    }

    @Override
    public HttpSession getSession(boolean create) {
        if (create) {
            if (httpSession == null) {
                String id = UUID.randomUUID().toString();
                httpSession = new HTTPSessionImpl(id);
                routingContext.addCookie(new Cookie(configuration.getSessionIdParameterName(), id));
                sessionMap.put(id, httpSession);
                return httpSession;
            } else {
                return httpSession;
            }
        } else {
            return httpSession;
        }
    }

    @Override
    public boolean isRequestedSessionIdFromURL() {
        return requestedSessionIdFromURL;
    }

    @Override
    public boolean isRequestedSessionIdFromCookie() {
        return requestedSessionIdFromCookie;
    }

    @Override
    public boolean isRequestedSessionIdValid() {
        return false;
    }

    @Override
    public String getRequestedSessionId() {
        return requestedSessionId;
    }
}
