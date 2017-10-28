package com.firefly.net;

import java.util.List;

/**
 * @author Pengtao Qiu
 */
public interface ApplicationProtocolSelector {

    String getApplicationProtocol();

    List<String> getSupportedProtocols();

}
