package com.fireflysource.net.tcp.secure.conscrypt;

import com.fireflysource.net.tcp.secure.ApplicationProtocolSelector;
import com.fireflysource.net.tcp.secure.SecureEngine;
import com.fireflysource.net.tcp.secure.common.AbstractSecureEngineFactory;
import kotlinx.coroutines.CoroutineScope;
import org.conscrypt.Conscrypt;

import javax.net.ssl.SSLEngine;
import java.security.Provider;
import java.security.Security;
import java.util.List;

/**
 * @author Pengtao Qiu
 */
abstract public class AbstractConscryptSecureEngineFactory extends AbstractSecureEngineFactory {

    public static final String SECURE_PROTOCOL = "TLSv1.3";

    private static String providerName;

    static {
        Provider provider = Conscrypt.newProvider();
        providerName = provider.getName();
        Security.addProvider(provider);
        LOG.info("Add Conscrypt security provider. info: {}", provider.getInfo());
    }

    @Override
    public String getProviderName() {
        return providerName;
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
        return new ConscryptSecureEngine(coroutineScope, sslEngine, applicationProtocolSelector);
    }

    @Override
    public ApplicationProtocolSelector createApplicationProtocolSelector(
            SSLEngine sslEngine, List<String> supportedProtocolList) {
        return new ConscryptApplicationProtocolSelector(sslEngine, supportedProtocolList);
    }

}
