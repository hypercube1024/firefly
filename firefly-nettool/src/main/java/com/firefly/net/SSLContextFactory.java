package com.firefly.net;

import io.netty.handler.ssl.SslContext;

import javax.net.ssl.SSLEngine;

public interface SSLContextFactory {

    SSLEngine createSSLEngine(boolean clientMode);

    SslContext getSslContext(boolean clientMode);

}
