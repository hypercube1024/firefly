package com.firefly.net.tcp.ssl;

import com.firefly.net.ApplicationProtocolSelector;
import io.netty.handler.ssl.SslHandler;

import javax.net.ssl.SSLEngine;

/**
 * @author Pengtao Qiu
 */
public class OpenSSLApplicationProtocolSelector implements ApplicationProtocolSelector {


    private final SslHandler sslHandler;

    public OpenSSLApplicationProtocolSelector(SSLEngine sslEngine) {
        this(new SslHandler(sslEngine));
    }

    public OpenSSLApplicationProtocolSelector(SslHandler sslHandler) {
        this.sslHandler = sslHandler;
    }

    @Override
    public String applicationProtocol() {
        return sslHandler.applicationProtocol();
    }
}
