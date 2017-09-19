package com.firefly.server.http2.router;

import com.firefly.codec.http2.model.Cookie;
import com.firefly.server.http2.router.spi.HTTPSessionHandlerSPI;
import com.firefly.utils.StringUtils;

import java.util.List;
import java.util.Optional;

/**
 * @author Pengtao Qiu
 */
abstract public class AbstractHTTPSessionHandlerSPI implements HTTPSessionHandlerSPI {

    protected final RoutingContext routingContext;

    protected String sessionIdParameterName = "jsessionid";
    protected int defaultMaxInactiveInterval = 10 * 60; //second

    protected boolean requestedSessionIdFromURL;
    protected boolean requestedSessionIdFromCookie;
    protected String requestedSessionId;

    public AbstractHTTPSessionHandlerSPI(RoutingContext routingContext,
                                         String sessionIdParameterName,
                                         int defaultMaxInactiveInterval) {
        this.routingContext = routingContext;
        if (StringUtils.hasText(sessionIdParameterName)) {
            this.sessionIdParameterName = sessionIdParameterName;
        }
        if (defaultMaxInactiveInterval > 0) {
            this.defaultMaxInactiveInterval = defaultMaxInactiveInterval;
        }
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
            String prefix = sessionIdParameterName + "=";
            if (param.length() > prefix.length()) {
                int s = param.indexOf(prefix);
                if (s >= 0) {
                    s += prefix.length();
                    requestedSessionId = param.substring(s);
                    requestedSessionIdFromCookie = false;
                    requestedSessionIdFromURL = true;
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
                                               .filter(c -> sessionIdParameterName.equalsIgnoreCase(c.getName()))
                                               .findFirst();
            if (optional.isPresent()) {
                requestedSessionIdFromCookie = true;
                requestedSessionIdFromURL = false;
                requestedSessionId = optional.get().getValue();
                return requestedSessionId;
            }
        }
        return null;
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
    public String getRequestedSessionId() {
        return requestedSessionId;
    }
}
