package com.fireflysource.net.http.common;

public class ProxyConfig {

    private String protocol;
    private String host;
    private int port;
    private ProxyAuthentication proxyAuthentication;

    public String getProtocol() {
        return protocol;
    }

    public void setProtocol(String protocol) {
        this.protocol = protocol;
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

    public ProxyAuthentication getProxyAuthentication() {
        return proxyAuthentication;
    }

    public void setProxyAuthentication(ProxyAuthentication proxyAuthentication) {
        this.proxyAuthentication = proxyAuthentication;
    }

    @Override
    public String toString() {
        return "ProxyConfig{" +
                "protocol='" + protocol + '\'' +
                ", host='" + host + '\'' +
                ", port=" + port +
                ", proxyAuthentication=" + proxyAuthentication +
                '}';
    }
}
