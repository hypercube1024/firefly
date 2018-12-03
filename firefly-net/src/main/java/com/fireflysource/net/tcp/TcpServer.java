package com.fireflysource.net.tcp;

import com.fireflysource.common.lifecycle.LifeCycle;

import java.net.SocketAddress;
import java.util.function.Consumer;

/**
 * @author Pengtao Qiu
 */
public interface TcpServer extends LifeCycle {

    TcpServer accept(Consumer<TcpConnection> consumer);

    TcpServer bind(SocketAddress address);

    TcpServer bind(String host, int port);

}
