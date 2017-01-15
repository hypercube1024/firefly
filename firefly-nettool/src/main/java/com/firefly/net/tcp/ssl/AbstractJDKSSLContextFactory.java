package com.firefly.net.tcp.ssl;

import com.firefly.net.SSLContextFactory;
import io.netty.handler.ssl.SslContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLEngine;

/**
 * @author Pengtao Qiu
 */
public abstract class AbstractJDKSSLContextFactory implements SSLContextFactory {

    protected static final Logger log = LoggerFactory.getLogger("firefly-system");

    protected volatile SSLContext sslContext;

    @Override
    public SSLEngine createSSLEngine(boolean clientMode) {
        SSLEngine sslEngine = getSslContext(clientMode).createSSLEngine();
        sslEngine.setUseClientMode(clientMode);
        return sslEngine;
    }

    public SSLContext getSslContext(boolean clientMode) {
        init(clientMode);
        return sslContext;
    }

    private void init(boolean clientMode) {
        if (sslContext == null) {
            synchronized (this) {
                if (sslContext == null) {
                    sslContext = createSSLContext(clientMode);
                }
            }
        }
    }

    abstract public SSLContext createSSLContext(boolean clientMode);
}
