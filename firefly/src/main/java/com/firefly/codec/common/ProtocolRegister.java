package com.firefly.codec.common;

import java.net.URL;
import java.net.URLConnection;
import java.net.URLStreamHandler;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * @author Pengtao Qiu
 */
abstract public class ProtocolRegister {

    private static final Map<String, Integer> protocols = new HashMap<>();

    static {
        URL.setURLStreamHandlerFactory(protocol -> protocols.containsKey(protocol) ? new URLStreamHandler() {

            @Override
            protected URLConnection openConnection(URL url) {
                return new URLConnection(url) {
                    public void connect() {
                    }
                };
            }

            @Override
            protected int getDefaultPort() {
                return Optional.ofNullable(protocols.get(protocol)).orElse(-1);
            }
        } : null);
    }

    public static void register(String protocol, int defaultPort) {
        protocols.put(protocol, defaultPort);
    }

    public static void remove(String protocol) {
        protocols.remove(protocol);
    }
}
