package com.fireflysource.net.tcp;

import com.fireflysource.net.tcp.aio.AioTcpClient;
import com.fireflysource.net.tcp.aio.TcpConfig;

abstract public class TcpClientFactory {

    public static TcpClient create() {
        return new AioTcpClient();
    }

    public static TcpClient create(TcpConfig config) {
        return new AioTcpClient(config);
    }
}
