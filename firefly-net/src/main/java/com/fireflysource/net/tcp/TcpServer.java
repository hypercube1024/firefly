package com.fireflysource.net.tcp;

import com.fireflysource.common.lifecycle.LifeCycle;
import com.fireflysource.net.tcp.secure.SecureEngineFactory;
import kotlinx.coroutines.channels.Channel;

import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.function.Consumer;

/**
 * @author Pengtao Qiu
 */
public interface TcpServer extends LifeCycle {

    TcpServer secureEngineFactory(SecureEngineFactory secureEngineFactory);

    TcpServer enableSecureConnection();

    TcpServer onAccept(Consumer<TcpConnection> consumer);

    Channel<TcpConnection> getTcpConnectionChannel();

    TcpServer listen(SocketAddress address);

    default TcpServer listen(String host, int port) {
        return listen(new InetSocketAddress(host, port));
    }

}
