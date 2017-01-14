package com.firefly.net;

import com.firefly.net.tcp.ssl.SSLSession;

public interface SSLEventHandler {
    void handshakeFinished(SSLSession sslSession);
}
