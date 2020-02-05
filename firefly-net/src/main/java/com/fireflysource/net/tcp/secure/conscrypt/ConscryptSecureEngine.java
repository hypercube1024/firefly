package com.fireflysource.net.tcp.secure.conscrypt;

import com.fireflysource.net.tcp.TcpConnection;
import com.fireflysource.net.tcp.secure.AbstractAsyncSecureEngine;
import com.fireflysource.net.tcp.secure.ApplicationProtocolSelector;

import javax.net.ssl.SSLEngine;

/**
 * @author Pengtao Qiu
 */
public class ConscryptSecureEngine extends AbstractAsyncSecureEngine {

    public ConscryptSecureEngine(TcpConnection tcpConnection, SSLEngine sslEngine,
                                 ApplicationProtocolSelector applicationProtocolSelector) {
        super(tcpConnection, sslEngine, applicationProtocolSelector);
    }

}
