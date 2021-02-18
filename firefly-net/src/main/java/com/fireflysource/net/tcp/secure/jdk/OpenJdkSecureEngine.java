package com.fireflysource.net.tcp.secure.jdk;

import com.fireflysource.net.tcp.secure.AbstractAsyncSecureEngine;
import com.fireflysource.net.tcp.secure.ApplicationProtocolSelector;
import kotlinx.coroutines.CoroutineScope;

import javax.net.ssl.SSLEngine;

public class OpenJdkSecureEngine extends AbstractAsyncSecureEngine {

    public OpenJdkSecureEngine(
            CoroutineScope coroutineScope,
            SSLEngine sslEngine,
            ApplicationProtocolSelector applicationProtocolSelector) {
        super(coroutineScope, sslEngine, applicationProtocolSelector);
    }
}
