package com.firefly.net;

import com.firefly.utils.lang.Pair;

import javax.net.ssl.SSLEngine;

public interface SSLContextFactory {

    Pair<SSLEngine, ApplicationProtocolSelector> createSSLEngine(boolean clientMode);

    Pair<SSLEngine, ApplicationProtocolSelector> createSSLEngine(boolean clientMode, String peerHost, int peerPort);

}
