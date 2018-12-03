package com.fireflysource.net.tcp;

import com.fireflysource.common.lifecycle.LifeCycle;

import java.net.SocketAddress;
import java.util.concurrent.CompletableFuture;

/**
 * @author Pengtao Qiu
 */
public interface TcpClient extends LifeCycle {

    CompletableFuture<TcpConnection> connect(SocketAddress address);

    CompletableFuture<TcpConnection> connect(String host, int port);

}
