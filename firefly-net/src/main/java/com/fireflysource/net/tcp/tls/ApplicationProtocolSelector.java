package com.fireflysource.net.tcp.tls;

import java.util.List;

/**
 * @author Pengtao Qiu
 */
public interface ApplicationProtocolSelector {

    default String getApplicationProtocol() {
        return null;
    }

    List<String> getSupportedApplicationProtocols();

}
