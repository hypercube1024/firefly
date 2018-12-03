package com.fireflysource.net.tcp;

import com.fireflysource.common.lifecycle.LifeCycle;

import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.concurrent.CompletableFuture;

/**
 * @author Pengtao Qiu
 */
public interface TcpClient extends LifeCycle {

    TcpClient enableSecureConnection();

    CompletableFuture<TcpConnection> connect(SocketAddress address);

    default CompletableFuture<TcpConnection> connect(String host, int port) {
        return connect(new InetSocketAddress(host, port));
    }

}
