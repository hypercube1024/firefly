package com.fireflysource.net.tcp.secure.wildfly;

import com.fireflysource.common.slf4j.LazyLogger;
import com.fireflysource.common.sys.SystemLogger;
import com.fireflysource.net.tcp.secure.ApplicationProtocolSelector;
import com.fireflysource.net.tcp.secure.SecureEngine;
import com.fireflysource.net.tcp.secure.common.AbstractSecureEngineFactory;
import kotlinx.coroutines.CoroutineScope;
import org.wildfly.openssl.OpenSSLProvider;

import javax.net.ssl.SSLEngine;
import java.util.List;

abstract public class AbstractWildflySecureEngineFactory extends AbstractSecureEngineFactory {

    protected static final LazyLogger LOG = SystemLogger.create(AbstractWildflySecureEngineFactory.class);

    private static final String SECURE_PROTOCOL = "openssl.TLS";
    private static final String PROVIDER_NAME;

    static {
        OpenSSLProvider.register();
        PROVIDER_NAME = OpenSSLProvider.INSTANCE.getName();
        LOG.info("Add wildfly openssl security provider. info: {}, name: {}", OpenSSLProvider.INSTANCE.getInfo(), PROVIDER_NAME);
    }

    @Override
    public String getSecureProtocol() {
        return SECURE_PROTOCOL;
    }

    @Override
    public String getProviderName() {
        return PROVIDER_NAME;
    }

    @Override
    public SecureEngine createSecureEngine(CoroutineScope coroutineScope, SSLEngine sslEngine, ApplicationProtocolSelector applicationProtocolSelector) {
        return new WildflySecureEngine(coroutineScope, sslEngine, applicationProtocolSelector);
    }

    @Override
    public ApplicationProtocolSelector createApplicationProtocolSelector(SSLEngine sslEngine, List<String> supportedProtocolList) {
        return new WildflyOpenSSLApplicationProtocolSelector(sslEngine, supportedProtocolList);
    }
}
