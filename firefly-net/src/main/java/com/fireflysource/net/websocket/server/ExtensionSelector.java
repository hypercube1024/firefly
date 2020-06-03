package com.fireflysource.net.websocket.server;

import java.util.List;

/**
 * @author Pengtao Qiu
 */
public interface ExtensionSelector {

    /**
     * Select the supported extensions.
     *
     * @param extensions The client supported extensions.
     * @return The server selected extensions.
     */
    List<String> select(List<String> extensions);

}
