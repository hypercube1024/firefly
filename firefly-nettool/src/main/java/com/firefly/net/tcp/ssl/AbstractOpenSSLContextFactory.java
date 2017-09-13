package com.firefly.net.tcp.ssl;

import com.firefly.net.ApplicationProtocolSelector;
import com.firefly.net.SSLContextFactory;
import com.firefly.utils.lang.Pair;
import io.netty.buffer.ByteBufAllocator;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.handler.ssl.SslContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.net.ssl.SSLEngine;

/**
 * @author Pengtao Qiu
 */
public abstract class AbstractOpenSSLContextFactory implements SSLContextFactory {

    protected static final Logger log = LoggerFactory.getLogger("firefly-system");

    protected volatile SslContext sslContext;
    protected ByteBufAllocator byteBufAllocator;

    public AbstractOpenSSLContextFactory() {
        byteBufAllocator = PooledByteBufAllocator.DEFAULT;
    }

    public ByteBufAllocator getByteBufAllocator() {
        return byteBufAllocator;
    }

    public void setByteBufAllocator(ByteBufAllocator byteBufAllocator) {
        this.byteBufAllocator = byteBufAllocator;
    }

    @Override
    public Pair<SSLEngine, ApplicationProtocolSelector> createSSLEngine(boolean clientMode) {
        SSLEngine sslEngine = getSslContext(clientMode).newEngine(byteBufAllocator);
        ApplicationProtocolSelector selector = new OpenSSLApplicationProtocolSelector(sslEngine);
        return new Pair<>(sslEngine, selector);
    }

    @Override
    public Pair<SSLEngine, ApplicationProtocolSelector> createSSLEngine(boolean clientMode, String peerHost, int peerPort) {
        SSLEngine sslEngine = getSslContext(clientMode).newEngine(byteBufAllocator, peerHost, peerPort);
        ApplicationProtocolSelector selector = new OpenSSLApplicationProtocolSelector(sslEngine);
        return new Pair<>(sslEngine, selector);
    }

    public SslContext getSslContext(boolean clientMode) {
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

    abstract public SslContext createSSLContext(boolean clientMode);
}
