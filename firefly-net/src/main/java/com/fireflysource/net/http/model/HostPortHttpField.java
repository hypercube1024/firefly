package com.fireflysource.net.http.model;

import com.fireflysource.net.http.exception.BadMessageException;

public class HostPortHttpField extends HttpField {

    private final HostPort hostPort;

    public HostPortHttpField(String authority) {
        this(HttpHeader.HOST, HttpHeader.HOST.getValue(), authority);
    }

    public HostPortHttpField(HttpHeader header, String name, String authority) {
        super(header, name, authority);
        try {
            hostPort = new HostPort(authority);
        } catch (Exception e) {
            throw new BadMessageException(HttpStatus.BAD_REQUEST_400, "Bad HostPort", e);
        }
    }

    /**
     * Get the host.
     *
     * @return the host
     */
    public String getHost() {
        return hostPort.getHost();
    }

    /**
     * Get the port.
     *
     * @return the port
     */
    public int getPort() {
        return hostPort.getPort();
    }

    /**
     * Get the port.
     *
     * @param defaultPort The default port to return if no port set
     * @return the port
     */
    public int getPort(int defaultPort) {
        return hostPort.getPort(defaultPort);
    }
}
