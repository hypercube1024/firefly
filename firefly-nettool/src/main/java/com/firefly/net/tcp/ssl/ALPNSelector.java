package com.firefly.net.tcp.ssl;

import com.firefly.net.ApplicationProtocolSelector;

import javax.net.ssl.SSLEngine;

/**
 * @author Pengtao Qiu
 */
public class ALPNSelector implements ApplicationProtocolSelector {

    private final SSLEngine sslEngine;

    public ALPNSelector(SSLEngine sslEngine) {
        this.sslEngine = sslEngine;
    }

    @Override
    public String applicationProtocol() {
        return null;
    }
}
