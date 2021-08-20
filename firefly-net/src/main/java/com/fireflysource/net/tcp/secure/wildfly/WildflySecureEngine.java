package com.fireflysource.net.tcp.secure.wildfly;

import com.fireflysource.net.tcp.secure.AbstractAsyncSecureEngine;
import com.fireflysource.net.tcp.secure.ApplicationProtocolSelector;
import kotlinx.coroutines.CoroutineScope;

import javax.net.ssl.SSLEngine;

public class WildflySecureEngine extends AbstractAsyncSecureEngine {

    public WildflySecureEngine(
            CoroutineScope coroutineScope,
            SSLEngine sslEngine,
            ApplicationProtocolSelector applicationProtocolSelector) {
        super(coroutineScope, sslEngine, applicationProtocolSelector);
    }
}
