package com.firefly.net.tcp.ssl;

import com.firefly.net.SSLContextFactory;
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

    public AbstractOpenSSLContextFactory(ByteBufAllocator byteBufAllocator) {
        this.byteBufAllocator = byteBufAllocator;
    }

    @Override
    public SSLEngine createSSLEngine(boolean clientMode) {
        if (sslContext == null) {
            synchronized (this) {
                if (sslContext == null) {
                    createSSLContext(clientMode);
                    return sslContext.newEngine(byteBufAllocator);
                } else {
                    return sslContext.newEngine(byteBufAllocator);
                }
            }
        } else {
            return sslContext.newEngine(byteBufAllocator);
        }
    }

    abstract public void createSSLContext(boolean clientMode);
}
