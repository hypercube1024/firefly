package com.firefly.server.http2.router.handler.session;

import com.firefly.codec.http2.model.Cookie;
import com.firefly.server.http2.router.RoutingContext;
import com.firefly.server.http2.router.spi.AsynchronousHttpSession;
import com.firefly.server.http2.router.spi.HTTPSessionHandlerSPI;
import com.firefly.utils.StringUtils;
import com.firefly.utils.concurrent.Scheduler;
import com.firefly.utils.time.Millisecond100Clock;

import javax.servlet.http.HttpSession;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

/**
 * @author Pengtao Qiu
 */
public class HTTPSessionHandlerSPIImpl implements HTTPSessionHandlerSPI {

    private final HTTPSessionConfiguration configuration;
    private final SessionStore sessionStore;
    private final RoutingContext routingContext;
    private final Scheduler scheduler;
    private boolean requestedSessionIdFromURL;
    private boolean requestedSessionIdFromCookie;
    private String requestedSessionId;
    private HTTPSessionImpl httpSession;

    public HTTPSessionHandlerSPIImpl(SessionStore sessionStore,
                                     RoutingContext routingContext,
                                     Scheduler scheduler,
                                     HTTPSessionConfiguration configuration) {
        this.sessionStore = sessionStore;
        this.routingContext = routingContext;
        this.configuration = configuration;
        this.scheduler = scheduler;
        init();
    }

    protected void init() {
        if (getHttpSessionFromCookie() == null) {
            getHttpSessionFromURL();
        }
    }

    protected String getHttpSessionFromURL() {
        if (requestedSessionId != null) {
            return requestedSessionId;
        }

        String param = routingContext.getURI().getParam();
        if (StringUtils.hasText(param)) {
            String prefix = configuration.getSessionIdParameterName() + "=";
            if (param.length() > prefix.length()) {
                int s = param.indexOf(prefix);
                if (s >= 0) {
                    s += prefix.length();
                    requestedSessionId = param.substring(s);
                    requestedSessionIdFromCookie = false;
                    requestedSessionIdFromURL = true;
                    requestHttpSession();
                    return requestedSessionId;
                }
            }
        }
        return null;
    }

    protected String getHttpSessionFromCookie() {
        if (requestedSessionId != null) {
            return requestedSessionId;
        }

        List<Cookie> cookies = routingContext.getCookies();
        if (cookies != null && !cookies.isEmpty()) {
            Optional<Cookie> optional = cookies.stream()
                                               .filter(c -> configuration.getSessionIdParameterName().equalsIgnoreCase(c.getName()))
                                               .findFirst();
            if (optional.isPresent()) {
                requestedSessionIdFromCookie = true;
                requestedSessionIdFromURL = false;
                requestedSessionId = optional.get().getValue();
                requestHttpSession();
                return requestedSessionId;
            }
        }
        return null;
    }


    protected void requestHttpSession() {
        httpSession = (HTTPSessionImpl) sessionStore.get(requestedSessionId);
        if (httpSession != null) {
            if (httpSession.check()) {
                httpSession.setLastAccessedTime(Millisecond100Clock.currentTimeMillis());
                httpSession.setNewSession(false);
                scheduleCheck(httpSession, httpSession.getRemainInactiveInterval());
            } else {
                httpSession = null;
                sessionStore.remove(requestedSessionId);
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
                httpSession.setMaxInactiveInterval(configuration.getDefaultMaxInactiveInterval());
                routingContext.addCookie(new Cookie(configuration.getSessionIdParameterName(), id));
                sessionStore.put(id, httpSession);
                scheduleCheck(httpSession, httpSession.getMaxInactiveInterval());
                return httpSession;
            } else {
                return httpSession;
            }
        } else {
            return httpSession;
        }
    }

    @Override
    public CompletableFuture<AsynchronousHttpSession> getAsyncSession() {
        return null;
    }

    @Override
    public CompletableFuture<AsynchronousHttpSession> getAsyncSession(boolean create) {
        return null;
    }

    @Override
    public CompletableFuture<Integer> getAsyncSessionSize() {
        return null;
    }

    protected void scheduleCheck(final HTTPSessionImpl session, final long remainInactiveInterval) {
        scheduler.schedule(() -> {
            if (!session.check()) {
                sessionStore.remove(session.getId());
            } else {
                scheduleCheck(session, session.getRemainInactiveInterval());
            }
        }, remainInactiveInterval, TimeUnit.MILLISECONDS);
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

    @Override
    public int getSessionSize() {
        return sessionStore.size();
    }
}
