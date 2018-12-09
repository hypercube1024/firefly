package com.fireflysource.net.http.common.model;

import com.fireflysource.common.string.StringUtils;

/**
 * Parse an authority string into Host and Port
 * <p>Parse a string in the form "host:port", handling IPv4 an IPv6 hosts</p>
 *
 * <p>The System property "com.fireflysource.net.http.model.HostPort.STRIP_IPV6" can be set to a boolean
 * value to control of the square brackets are stripped off IPv6 addresses (default false).</p>
 */
public class HostPort {
    private final static boolean STRIP_IPV6 = Boolean.parseBoolean(System.getProperty("com.fireflysource.net.http.model.HostPort.STRIP_IPV6", "false"));

    private final String host;
    private final int port;

    public HostPort(String authority) throws IllegalArgumentException {
        if (authority == null)
            throw new IllegalArgumentException("No Authority");
        try {
            if (authority.isEmpty()) {
                host = authority;
                port = 0;
            } else if (authority.charAt(0) == '[') {
                // ipv6reference
                int close = authority.lastIndexOf(']');
                if (close < 0)
                    throw new IllegalArgumentException("Bad IPv6 host");
                host = STRIP_IPV6 ? authority.substring(1, close) : authority.substring(0, close + 1);

                if (authority.length() > close + 1) {
                    if (authority.charAt(close + 1) != ':')
                        throw new IllegalArgumentException("Bad IPv6 port");
                    port = StringUtils.toInt(authority, close + 2);
                } else
                    port = 0;
            } else {
                // ipv4address or hostname
                int c = authority.lastIndexOf(':');
                if (c >= 0) {
                    host = authority.substring(0, c);
                    port = StringUtils.toInt(authority, c + 1);
                } else {
                    host = authority;
                    port = 0;
                }
            }
        } catch (IllegalArgumentException iae) {
            throw iae;
        } catch (final Exception ex) {
            throw new IllegalArgumentException("Bad HostPort") {
                {
                    initCause(ex);
                }
            };
        }
        if (host == null)
            throw new IllegalArgumentException("Bad host");
        if (port < 0)
            throw new IllegalArgumentException("Bad port");
    }

    /* ------------------------------------------------------------ */

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

    /* ------------------------------------------------------------ */

    /**
     * Get the host.
     *
     * @return the host
     */
    public String getHost() {
        return host;
    }

    /* ------------------------------------------------------------ */

    /**
     * Get the port.
     *
     * @return the port
     */
    public int getPort() {
        return port;
    }

    /* ------------------------------------------------------------ */

    /**
     * Get the port.
     *
     * @param defaultPort, the default port to return if a port is not specified
     * @return the port
     */
    public int getPort(int defaultPort) {
        return port > 0 ? port : defaultPort;
    }
}
