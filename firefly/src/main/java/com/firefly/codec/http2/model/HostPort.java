package com.firefly.codec.http2.model;

import com.firefly.utils.StringUtils;

/**
 * Parse an authority string into Host and Port
 * <p>
 * Parse a string in the form "host:port", handling IPv4 an IPv6 hosts
 * </p>
 */
public class HostPort {
    private final String _host;
    private final int _port;

    public HostPort(String authority) throws IllegalArgumentException {
        if (authority == null || authority.length() == 0)
            throw new IllegalArgumentException("No Authority");
        try {
            if (authority.charAt(0) == '[') {
                // ipv6reference
                int close = authority.lastIndexOf(']');
                if (close < 0)
                    throw new IllegalArgumentException("Bad IPv6 host");
                _host = authority.substring(0, close + 1);

                if (authority.length() > close + 1) {
                    if (authority.charAt(close + 1) != ':')
                        throw new IllegalArgumentException("Bad IPv6 port");
                    _port = StringUtils.toInt(authority, close + 2);
                } else
                    _port = 0;
            } else {
                // ipv4address or hostname
                int c = authority.lastIndexOf(':');
                if (c >= 0) {
                    _host = authority.substring(0, c);
                    _port = StringUtils.toInt(authority, c + 1);
                } else {
                    _host = authority;
                    _port = 0;
                }
            }
        } catch (IllegalArgumentException iae) {
            throw iae;
        } catch (final Exception ex) {
            throw new IllegalArgumentException("Bad HostPort") {

                private static final long serialVersionUID = -2006013178255092535L;

                {
                    initCause(ex);
                }
            };
        }
        if (_host.isEmpty())
            throw new IllegalArgumentException("Bad host");
        if (_port < 0)
            throw new IllegalArgumentException("Bad port");
    }

    /**
     * Get the host.
     *
     * @return the host
     */
    public String getHost() {
        return _host;
    }

    /**
     * Get the port.
     *
     * @return the port
     */
    public int getPort() {
        return _port;
    }

    /**
     * Get the port.
     *
     * @param defaultPort, the default port to return if a port is not specified
     * @return the port
     */
    public int getPort(int defaultPort) {
        return _port > 0 ? _port : defaultPort;
    }

    /**
     * Normalize IPv6 address as per https://www.ietf.org/rfc/rfc2732.txt
     *
     * @param host A host name
     * @return Host name surrounded by '[' and ']' as needed.
     */
    public static String normalizeHost(String host) {
        // if it is normalized IPv6 or could not be IPv6, return
        if (host.isEmpty() || host.charAt(0) == '[' || host.indexOf(':') < 0)
            return host;

        // normalize with [ ]
        return "[" + host + "]";
    }
}
