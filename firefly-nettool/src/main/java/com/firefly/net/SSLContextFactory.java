package com.firefly.net;

import javax.net.ssl.SSLEngine;

public interface SSLContextFactory {

    SSLEngine createSSLEngine(boolean clientMode);

}
