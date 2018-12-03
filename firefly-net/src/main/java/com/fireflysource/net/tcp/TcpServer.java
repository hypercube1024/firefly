package com.fireflysource.net.tcp;

import com.fireflysource.common.lifecycle.LifeCycle;

import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.function.Consumer;

/**
 * @author Pengtao Qiu
 */
public interface TcpServer extends LifeCycle {

    TcpServer onAccept(Consumer<TcpConnection> consumer);

    void bind(SocketAddress address);

    default void bind(String host, int port) {
        bind(new InetSocketAddress(host, port));
    }

}
