package com.firefly.example.reactive.coffee.store;

/**
 * @author Pengtao Qiu
 */
public class ProjectConfig {

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
}
