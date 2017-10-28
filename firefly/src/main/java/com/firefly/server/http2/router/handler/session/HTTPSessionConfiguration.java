package com.firefly.server.http2.router.handler.session;

/**
 * @author Pengtao Qiu
 */
public class HTTPSessionConfiguration {

    private String sessionIdParameterName = "jsessionid";
    private int defaultMaxInactiveInterval = 10 * 60; //second

    public String getSessionIdParameterName() {
        return sessionIdParameterName;
    }

    public void setSessionIdParameterName(String sessionIdParameterName) {
        this.sessionIdParameterName = sessionIdParameterName;
    }

    public int getDefaultMaxInactiveInterval() {
        return defaultMaxInactiveInterval;
    }

    public void setDefaultMaxInactiveInterval(int defaultMaxInactiveInterval) {
        this.defaultMaxInactiveInterval = defaultMaxInactiveInterval;
    }
}
