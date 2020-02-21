package com.fireflysource.net.tcp;

import com.fireflysource.net.tcp.aio.AioTcpServer;
import com.fireflysource.net.tcp.aio.TcpConfig;

abstract public class TcpServerFactory {

    public static TcpServer create() {
        return new AioTcpServer();
    }

    public static TcpServer create(TcpConfig config) {
        return new AioTcpServer(config);
    }
}
