package com.firefly.net.tcp.ffsocks.client;

import com.firefly.net.tcp.TcpConfiguration;
import com.firefly.net.tcp.codec.ffsocks.stream.FfsocksConfiguration;

/**
 * @author Pengtao Qiu
 */
public class ClientFfsocksConfiguration extends FfsocksConfiguration {

    private TcpConfiguration tcpConfiguration = new TcpConfiguration();

    public TcpConfiguration getTcpConfiguration() {
        return tcpConfiguration;
    }

    public void setTcpConfiguration(TcpConfiguration tcpConfiguration) {
        this.tcpConfiguration = tcpConfiguration;
    }
}
