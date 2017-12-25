package com.firefly.net.tcp.flex.client;

import com.firefly.net.tcp.TcpConfiguration;
import com.firefly.net.tcp.codec.flex.stream.FlexConfiguration;

/**
 * @author Pengtao Qiu
 */
public class MultiplexingClientConfiguration extends FlexConfiguration {

    private TcpConfiguration tcpConfiguration = new TcpConfiguration();

    public TcpConfiguration getTcpConfiguration() {
        return tcpConfiguration;
    }

    public void setTcpConfiguration(TcpConfiguration tcpConfiguration) {
        this.tcpConfiguration = tcpConfiguration;
    }
}
