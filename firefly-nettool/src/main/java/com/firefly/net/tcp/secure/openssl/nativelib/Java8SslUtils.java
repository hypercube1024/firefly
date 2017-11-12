package com.firefly.net.tcp.secure.openssl.nativelib;

import javax.net.ssl.SNIHostName;
import javax.net.ssl.SNIMatcher;
import javax.net.ssl.SNIServerName;
import javax.net.ssl.SSLParameters;
import java.util.*;

final class Java8SslUtils {

    private Java8SslUtils() {
    }

    static List<String> getSniHostNames(SSLParameters sslParameters) {
        List<SNIServerName> names = sslParameters.getServerNames();
        if (names == null || names.isEmpty()) {
            return Collections.emptyList();
        }
        List<String> strings = new ArrayList<>(names.size());

        for (SNIServerName serverName : names) {
            if (serverName instanceof SNIHostName) {
                strings.add(((SNIHostName) serverName).getAsciiName());
            } else {
                throw new IllegalArgumentException("Only " + SNIHostName.class.getName()
                        + " instances are supported, but found: " + serverName);
            }
        }
        return strings;
    }

    static void setSniHostNames(SSLParameters sslParameters, List<String> names) {
        List<SNIServerName> sniServerNames = new ArrayList<>(names.size());
        for (String name : names) {
            sniServerNames.add(new SNIHostName(name));
        }
        sslParameters.setServerNames(sniServerNames);
    }

    static boolean getUseCipherSuitesOrder(SSLParameters sslParameters) {
        return sslParameters.getUseCipherSuitesOrder();
    }

    static void setUseCipherSuitesOrder(SSLParameters sslParameters, boolean useOrder) {
        sslParameters.setUseCipherSuitesOrder(useOrder);
    }

    @SuppressWarnings("unchecked")
    static void setSNIMatchers(SSLParameters sslParameters, Collection<?> matchers) {
        sslParameters.setSNIMatchers((Collection<SNIMatcher>) matchers);
    }

    @SuppressWarnings("unchecked")
    static boolean checkSniHostnameMatch(Collection<?> matchers, String hostname) {
        if (matchers != null && !matchers.isEmpty()) {
            SNIHostName name = new SNIHostName(hostname);
            Iterator<SNIMatcher> matcherIt = (Iterator<SNIMatcher>) matchers.iterator();
            while (matcherIt.hasNext()) {
                SNIMatcher matcher = matcherIt.next();
                // type 0 is for hostname
                if (matcher.getType() == 0 && matcher.matches(name)) {
                    return true;
                }
            }
            return false;
        }
        return true;
    }
}
