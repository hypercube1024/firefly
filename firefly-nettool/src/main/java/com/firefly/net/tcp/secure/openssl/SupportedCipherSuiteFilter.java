package com.firefly.net.tcp.secure.openssl;

import javax.net.ssl.SSLEngine;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * This class will filter all requested ciphers out that are not supported by the current {@link SSLEngine}.
 */
public final class SupportedCipherSuiteFilter implements CipherSuiteFilter {
    public static final SupportedCipherSuiteFilter INSTANCE = new SupportedCipherSuiteFilter();

    private SupportedCipherSuiteFilter() {
    }

    @Override
    public String[] filterCipherSuites(Iterable<String> ciphers, List<String> defaultCiphers,
                                       Set<String> supportedCiphers) {
        if (defaultCiphers == null) {
            throw new NullPointerException("defaultCiphers");
        }
        if (supportedCiphers == null) {
            throw new NullPointerException("supportedCiphers");
        }

        final List<String> newCiphers;
        if (ciphers == null) {
            newCiphers = new ArrayList<String>(defaultCiphers.size());
            ciphers = defaultCiphers;
        } else {
            newCiphers = new ArrayList<String>(supportedCiphers.size());
        }
        for (String c : ciphers) {
            if (c == null) {
                break;
            }
            if (supportedCiphers.contains(c)) {
                newCiphers.add(c);
            }
        }
        return newCiphers.toArray(new String[newCiphers.size()]);
    }

}
