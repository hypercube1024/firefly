package com.firefly.server.http2.router.handler.session;

/**
 * @author Pengtao Qiu
 */
public class LocalHTTPSessionConfiguration {

    private String sessionIdParameterName = "jsessionid";
    private int defaultMaxInactiveInterval = 10 * 60; //unit second
    private int schedulerPoolSize = 1;

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

    public int getSchedulerPoolSize() {
        return schedulerPoolSize;
    }

    public void setSchedulerPoolSize(int schedulerPoolSize) {
        this.schedulerPoolSize = schedulerPoolSize;
    }
}
