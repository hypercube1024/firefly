package com.fireflysource.net.tcp.secure.conscrypt;

import com.fireflysource.net.tcp.secure.AbstractAsyncSecureEngine;
import com.fireflysource.net.tcp.secure.ApplicationProtocolSelector;
import kotlinx.coroutines.CoroutineScope;

import javax.net.ssl.SSLEngine;

/**
 * @author Pengtao Qiu
 */
public class ConscryptSecureEngine extends AbstractAsyncSecureEngine {

    public ConscryptSecureEngine(
            CoroutineScope coroutineScope,
            SSLEngine sslEngine,
            ApplicationProtocolSelector applicationProtocolSelector) {
        super(coroutineScope, sslEngine, applicationProtocolSelector);
    }

}
