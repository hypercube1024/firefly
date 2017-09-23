package com.firefly.example.reactive.coffee.store;

/**
 * @author Pengtao Qiu
 */
public class ProjectConfig {

    private String logoutURL = "/logout";
    private String loginURL = "/login";
    private String loginUserKey = "_loginUser";
    private int sessionMaxInactiveInterval = 30 * 60;
    private String templateRoot;
    private String host;
    private int port;

    public String getTemplateRoot() {
        return templateRoot;
    }

    public void setTemplateRoot(String templateRoot) {
        this.templateRoot = templateRoot;
    }

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public String getLoginUserKey() {
        return loginUserKey;
    }

    public void setLoginUserKey(String loginUserKey) {
        this.loginUserKey = loginUserKey;
    }

    public String getLoginURL() {
        return loginURL;
    }

    public void setLoginURL(String loginURL) {
        this.loginURL = loginURL;
    }

    public String getLogoutURL() {
        return logoutURL;
    }

    public void setLogoutURL(String logoutURL) {
        this.logoutURL = logoutURL;
    }

    public int getSessionMaxInactiveInterval() {
        return sessionMaxInactiveInterval;
    }

    public void setSessionMaxInactiveInterval(int sessionMaxInactiveInterval) {
        this.sessionMaxInactiveInterval = sessionMaxInactiveInterval;
    }
}
