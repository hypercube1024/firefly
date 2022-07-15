package com.fireflysource.net.udp;

import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.function.Consumer;

public interface UdpServer {

    /**
     * The UDP connection open event listener.
     *
     * @param consumer The UDP connection.
     * @return The UDP server.
     */
    UdpServer onOpen(Consumer<UdpConnection> consumer);

    /**
     * Bind a server UDP address
     *
     * @param address The server UDP address.
     * @return The UDP server.
     */
    UdpServer listen(SocketAddress address);

    /**
     * Bind a server UDP address
     *
     * @param host The server host.
     * @param port The server port.
     * @return The UDP server.
     */
    default UdpServer listen(String host, int port) {
        return listen(new InetSocketAddress(host, port));
    }
}
