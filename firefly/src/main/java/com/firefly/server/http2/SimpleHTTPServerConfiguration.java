package com.firefly.server.http2;

import com.firefly.codec.http2.stream.HTTP2Configuration;

public class SimpleHTTPServerConfiguration extends HTTP2Configuration {

    private String host;
    private int port;

    /**
     * Get the HTTP server host name.
     *
     * @return The HTTP server host name.
     */
    public String getHost() {
        return host;
    }

    /**
     * Set the HTTP server host name.
     *
     * @param host The HTTP server host name.
     */
    public void setHost(String host) {
        this.host = host;
    }

    /**
     * Get the HTTP server TCP port.
     *
     * @return The HTTP server TCP port.
     */
    public int getPort() {
        return port;
    }

    /**
     * Set the HTTP server TCP port.
     *
     * @param port The HTTP server TCP port.
     */
    public void setPort(int port) {
        this.port = port;
    }

}
