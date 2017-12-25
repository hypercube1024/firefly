package com.firefly.net.tcp.ffsocks.server;

import com.firefly.net.tcp.TcpServerConfiguration;
import com.firefly.net.tcp.codec.ffsocks.stream.FfsocksConfiguration;

/**
 * @author Pengtao Qiu
 */
public class ServerFfsocksConfiguration extends FfsocksConfiguration {

    private TcpServerConfiguration tcpServerConfiguration = new TcpServerConfiguration();

    public TcpServerConfiguration getTcpServerConfiguration() {
        return tcpServerConfiguration;
    }

    public void setTcpServerConfiguration(TcpServerConfiguration tcpServerConfiguration) {
        this.tcpServerConfiguration = tcpServerConfiguration;
    }
}
