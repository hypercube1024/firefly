package com.firefly.net.tcp.secure.openssl.nativelib;

import com.firefly.net.ApplicationProtocolSelector;

/**
 * OpenSSL version of {@link ApplicationProtocolSelector}.
 *
 * @deprecated use {@link ApplicationProtocolConfig}
 */
@Deprecated
public interface OpenSslApplicationProtocolNegotiator extends ApplicationProtocolSelector {

    /**
     * Returns the {@link ApplicationProtocolConfig.Protocol} which should be used.
     */
    ApplicationProtocolConfig.Protocol protocol();

    /**
     * Get the desired behavior for the peer who selects the application protocol.
     */
    ApplicationProtocolConfig.SelectorFailureBehavior selectorFailureBehavior();

    /**
     * Get the desired behavior for the peer who is notified of the selected protocol.
     */
    ApplicationProtocolConfig.SelectedListenerFailureBehavior selectedListenerFailureBehavior();
}
