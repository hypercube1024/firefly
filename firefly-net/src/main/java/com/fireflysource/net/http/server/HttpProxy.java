package com.fireflysource.net.http.server;

import com.fireflysource.common.lifecycle.LifeCycle;

import java.net.InetSocketAddress;
import java.net.SocketAddress;

public interface HttpProxy extends LifeCycle {

    /**
     * Bind a server TCP address
     *
     * @param address The server TCP address.
     */
    void listen(SocketAddress address);

    /**
     * Bind the server host and port.
     *
     * @param host The server host.
     * @param port The server port.
     */
    default void listen(String host, int port) {
        listen(new InetSocketAddress(host, port));
    }
}
