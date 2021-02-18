package com.fireflysource.net.websocket.server;

import java.util.List;

/**
 * @author Pengtao Qiu
 */
public interface SubProtocolSelector {

    /**
     * Select the supported sub protocols.
     *
     * @param protocols The client supported sub protocols.
     * @return The server selected sub protocols.
     */
    List<String> select(List<String> protocols);

}
