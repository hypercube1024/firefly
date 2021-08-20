package com.fireflysource.net.tcp.secure.jdk;

import com.fireflysource.common.slf4j.LazyLogger;
import com.fireflysource.common.sys.JavaVersion;
import com.fireflysource.common.sys.SystemLogger;
import com.fireflysource.net.tcp.secure.ApplicationProtocolSelector;
import com.fireflysource.net.tcp.secure.SecureEngine;
import com.fireflysource.net.tcp.secure.common.AbstractSecureEngineFactory;
import kotlinx.coroutines.CoroutineScope;
import org.openjsse.net.ssl.OpenJSSE;

import javax.net.ssl.SSLEngine;
import java.security.Provider;
import java.security.Security;
import java.util.List;

abstract public class AbstractOpenJdkSecureEngineFactory extends AbstractSecureEngineFactory {

    protected static final LazyLogger LOG = SystemLogger.create(AbstractOpenJdkSecureEngineFactory.class);

    private static final String SECURE_PROTOCOL = "TLSv1.3";
    private static final String PROVIDER_NAME;

    static {
        if (JavaVersion.VERSION.getPlatform() < 9) {
            Provider provider = new OpenJSSE();
            PROVIDER_NAME = provider.getName();
            Security.addProvider(provider);
            LOG.info("Add Openjsse security provider. info: {}, name: {}", provider.getInfo(), PROVIDER_NAME);
        } else {
            PROVIDER_NAME = "SunJSSE";
            Provider provider = Security.getProvider(PROVIDER_NAME);
            LOG.info("Select {} security provider. info: {}", PROVIDER_NAME, provider.getInfo());
        }
    }

    @Override
    public String getProviderName() {
        return PROVIDER_NAME;
    }

    @Override
    public String getSecureProtocol() {
        return SECURE_PROTOCOL;
    }

    @Override
    public SecureEngine createSecureEngine(
            CoroutineScope coroutineScope,
            SSLEngine sslEngine,
            ApplicationProtocolSelector applicationProtocolSelector) {
        return new OpenJdkSecureEngine(coroutineScope, sslEngine, applicationProtocolSelector);
    }

    @Override
    public ApplicationProtocolSelector createApplicationProtocolSelector(
            SSLEngine sslEngine, List<String> supportedProtocolList) {
        return new OpenJdkApplicationProtocolSelector(sslEngine, supportedProtocolList);
    }

}
