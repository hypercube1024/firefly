package com.firefly.server.http2.router.handler.session;

/**
 * @author Pengtao Qiu
 */
public class LocalHTTPSessionConfiguration {

    private String sessionIdParameterName = "jsessionid";
    private String sessionIdPathParameterNamePrefix = ";" + sessionIdParameterName + "=";
    private int defaultMaxInactiveInterval = 10 * 60; //unit second

    public String getSessionIdParameterName() {
        return sessionIdParameterName;
    }

    public void setSessionIdParameterName(String sessionIdParameterName) {
        this.sessionIdParameterName = sessionIdParameterName;
        sessionIdPathParameterNamePrefix = ";" + sessionIdParameterName + "=";
    }

    public int getDefaultMaxInactiveInterval() {
        return defaultMaxInactiveInterval;
    }

    public void setDefaultMaxInactiveInterval(int defaultMaxInactiveInterval) {
        this.defaultMaxInactiveInterval = defaultMaxInactiveInterval;
    }

    public String getSessionIdPathParameterNamePrefix() {
        return sessionIdPathParameterNamePrefix;
    }
}
