package com.firefly.codec.common;

import java.net.URL;
import java.net.URLConnection;
import java.net.URLStreamHandler;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * @author Pengtao Qiu
 */
abstract public class ProtocolRegister {

    private static final Set<String> protocols = new HashSet<>();

    static {
        URL.setURLStreamHandlerFactory(protocol -> protocols.contains(protocol) ? new URLStreamHandler() {
            protected URLConnection openConnection(URL url) {
                return new URLConnection(url) {
                    public void connect() {
                    }
                };
            }
        } : null);
    }

    public static void register(String protocol) {
        protocols.add(protocol);
    }

    public static void register(List<String> protocols) {
        ProtocolRegister.protocols.addAll(protocols);
    }

    public static void remove(String protocol) {
        protocols.remove(protocol);
    }
}
