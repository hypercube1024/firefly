package com.fireflysource.net.udp;

import java.net.SocketAddress;

public interface UdpClient {

    /**
     * Create a UDP connection.
     *
     * @param address The server address.
     * @return The UDP connection.
     */
    UdpConnection connect(SocketAddress address);
}
