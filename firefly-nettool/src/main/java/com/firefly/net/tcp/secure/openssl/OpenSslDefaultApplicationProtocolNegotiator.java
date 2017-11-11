package com.firefly.net.tcp.secure.openssl;

import com.firefly.net.ApplicationProtocolSelector;

import java.util.List;

import static com.firefly.net.tcp.secure.openssl.ObjectUtil.checkNotNull;

/**
 * OpenSSL {@link ApplicationProtocolSelector} for ALPN.
 *
 * @deprecated use {@link ApplicationProtocolConfig}.
 */
@Deprecated
public final class OpenSslDefaultApplicationProtocolNegotiator implements OpenSslApplicationProtocolNegotiator {

    private final ApplicationProtocolConfig config;

    public OpenSslDefaultApplicationProtocolNegotiator(ApplicationProtocolConfig config) {
        this.config = checkNotNull(config, "config");
    }

    @Override
    public List<String> getSupportedApplicationProtocols() {
        return config.supportedProtocols();
    }

    @Override
    public ApplicationProtocolConfig.Protocol protocol() {
        return config.protocol();
    }

    @Override
    public ApplicationProtocolConfig.SelectorFailureBehavior selectorFailureBehavior() {
        return config.selectorFailureBehavior();
    }

    @Override
    public ApplicationProtocolConfig.SelectedListenerFailureBehavior selectedListenerFailureBehavior() {
        return config.selectedListenerFailureBehavior();
    }
}
