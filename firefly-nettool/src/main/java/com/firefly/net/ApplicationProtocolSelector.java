package com.firefly.net;

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
