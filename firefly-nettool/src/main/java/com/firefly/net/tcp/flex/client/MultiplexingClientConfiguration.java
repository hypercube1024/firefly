package com.firefly.net.tcp.flex.client;

import com.firefly.net.tcp.TcpConfiguration;
import com.firefly.net.tcp.codec.flex.stream.FlexConfiguration;

import java.util.Set;

/**
 * @author Pengtao Qiu
 */
public class MultiplexingClientConfiguration extends FlexConfiguration {

    private TcpConfiguration tcpConfiguration = new TcpConfiguration();
    private Set<String> serverUrlSet;
    private AddressProvider addressProvider;

    public TcpConfiguration getTcpConfiguration() {
        return tcpConfiguration;
    }

    public void setTcpConfiguration(TcpConfiguration tcpConfiguration) {
        this.tcpConfiguration = tcpConfiguration;
    }

    public Set<String> getServerUrlSet() {
        return serverUrlSet;
    }

    public void setServerUrlSet(Set<String> serverUrlSet) {
        this.serverUrlSet = serverUrlSet;
    }

    public AddressProvider getAddressProvider() {
        return addressProvider;
    }

    public void setAddressProvider(AddressProvider addressProvider) {
        this.addressProvider = addressProvider;
    }
}
