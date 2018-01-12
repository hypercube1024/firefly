package com.firefly.net.tcp.flex.server;

import com.firefly.net.tcp.TcpServerConfiguration;
import com.firefly.net.tcp.codec.flex.stream.FlexConfiguration;

/**
 * @author Pengtao Qiu
 */
public class MultiplexingServerConfiguration extends FlexConfiguration {

    private TcpServerConfiguration tcpServerConfiguration = new TcpServerConfiguration();

    public TcpServerConfiguration getTcpServerConfiguration() {
        return tcpServerConfiguration;
    }

    public void setTcpServerConfiguration(TcpServerConfiguration tcpServerConfiguration) {
        this.tcpServerConfiguration = tcpServerConfiguration;
    }
}
