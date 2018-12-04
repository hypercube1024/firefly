package com.fireflysource.net.tcp;

import com.fireflysource.common.lifecycle.LifeCycle;
import kotlinx.coroutines.channels.Channel;

import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.function.Consumer;

/**
 * @author Pengtao Qiu
 */
public interface TcpServer extends LifeCycle {

    TcpServer enableSecureConnection();

    TcpServer onAccept(Consumer<TcpConnection> consumer);

    Channel<TcpConnection> getTcpConnectionChannel();

    void listen(SocketAddress address);

    default void listen(String host, int port) {
        listen(new InetSocketAddress(host, port));
    }

}
