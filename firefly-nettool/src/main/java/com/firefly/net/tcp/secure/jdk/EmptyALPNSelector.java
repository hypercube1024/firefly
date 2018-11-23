package com.firefly.net.tcp.secure.jdk;

import com.firefly.net.ApplicationProtocolSelector;

import javax.net.ssl.SSLEngine;
import java.util.Collections;
import java.util.List;

/**
 * @author Pengtao Qiu
 */
public class EmptyALPNSelector implements ApplicationProtocolSelector {

    private final List<String> supportedProtocols;
    private volatile String applicationProtocol;

    public EmptyALPNSelector(SSLEngine sslEngine, List<String> supportedProtocols) {
        this.supportedProtocols = Collections.singletonList("http/1.1");
        this.applicationProtocol = this.supportedProtocols.get(0);
    }

    @Override
    public String getApplicationProtocol() {
        return applicationProtocol;
    }

    @Override
    public List<String> getSupportedApplicationProtocols() {
        return supportedProtocols;
    }
}
