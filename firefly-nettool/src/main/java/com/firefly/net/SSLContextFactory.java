package com.firefly.net;

import javax.net.ssl.SSLEngine;

public interface SSLContextFactory {

    SSLEngine createSSLEngine(boolean clientMode);

    SSLEngine createSSLEngine(boolean clientMode, String peerHost, int peerPort);

}
