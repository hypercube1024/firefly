package com.fireflysource.net.tcp;

import com.fireflysource.common.lifecycle.LifeCycle;

import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.function.Consumer;

/**
 * @author Pengtao Qiu
 */
public interface TcpServer extends LifeCycle {

    TcpServer enableSecureConnection();

    TcpServer onAccept(Consumer<TcpConnection> consumer);

    void listen(SocketAddress address);

    default void listen(String host, int port) {
        listen(new InetSocketAddress(host, port));
    }

}
