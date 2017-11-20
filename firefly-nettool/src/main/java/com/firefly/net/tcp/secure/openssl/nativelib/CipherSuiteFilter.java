package com.firefly.net.tcp.secure.openssl.nativelib;

import javax.net.ssl.SSLEngine;
import java.util.List;
import java.util.Set;

/**
 * Provides a means to filter the supplied cipher suite based upon the supported and default cipher suites.
 */
public interface CipherSuiteFilter {
    /**
     * Filter the requested {@code ciphers} based upon other cipher characteristics.
     *
     * @param ciphers          The requested ciphers
     * @param defaultCiphers   The default recommended ciphers for the current {@link SSLEngine} as determined by Netty
     * @param supportedCiphers The supported ciphers for the current {@link SSLEngine}
     * @return The filter list of ciphers. Must not return {@code null}.
     */
    String[] filterCipherSuites(Iterable<String> ciphers, List<String> defaultCiphers, Set<String> supportedCiphers);
}
