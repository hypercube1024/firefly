package com.fireflysource.net.tcp.secure;

import java.util.List;

/**
 * The TLS application layer protocol negotiation.
 *
 * @author Pengtao Qiu
 */
public interface ApplicationProtocolSelector {

    /**
     * The protocol negotiation result.
     *
     * @return The protocol negotiation result.
     */
    default String getApplicationProtocol() {
        return "";
    }

    /**
     * The current connection supports the protocols.
     *
     * @return The current connection supports the protocols.
     */
    List<String> getSupportedApplicationProtocols();

}
