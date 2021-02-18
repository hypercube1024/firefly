package com.fireflysource.net.tcp;

import com.fireflysource.common.collection.CollectionUtils;
import com.fireflysource.common.lifecycle.AbstractLifeCycle;
import com.fireflysource.common.object.Assert;
import com.fireflysource.net.tcp.secure.SecureEngineFactory;

import java.net.InetSocketAddress;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public class TcpClientConnectionFactory extends AbstractLifeCycle {

    private TcpChannelGroup tcpChannelGroup;
    private boolean stopTcpChannelGroup;
    private long timeout;
    private SecureEngineFactory secureEngineFactory;
    private TcpClient tcpClient;
    private TcpClient secureTcpClient;

    public TcpClientConnectionFactory() {
    }

    public TcpClientConnectionFactory(TcpChannelGroup tcpChannelGroup, boolean stopTcpChannelGroup, long timeout, SecureEngineFactory secureEngineFactory) {
        this.tcpChannelGroup = tcpChannelGroup;
        this.stopTcpChannelGroup = stopTcpChannelGroup;
        this.timeout = timeout;
        this.secureEngineFactory = secureEngineFactory;
    }

    public TcpChannelGroup getTcpChannelGroup() {
        return tcpChannelGroup;
    }

    public void setTcpChannelGroup(TcpChannelGroup tcpChannelGroup) {
        this.tcpChannelGroup = tcpChannelGroup;
    }

    public boolean isStopTcpChannelGroup() {
        return stopTcpChannelGroup;
    }

    public void setStopTcpChannelGroup(boolean stopTcpChannelGroup) {
        this.stopTcpChannelGroup = stopTcpChannelGroup;
    }

    public long getTimeout() {
        return timeout;
    }

    public void setTimeout(long timeout) {
        this.timeout = timeout;
    }

    public SecureEngineFactory getSecureEngineFactory() {
        return secureEngineFactory;
    }

    public void setSecureEngineFactory(SecureEngineFactory secureEngineFactory) {
        this.secureEngineFactory = secureEngineFactory;
    }

    public TcpClientConnectionFactory isStopTcpChannelGroup(boolean isStopTcpChannelGroup) {
        this.stopTcpChannelGroup = isStopTcpChannelGroup;
        return this;
    }

    public TcpClientConnectionFactory timeout(long timeout) {
        this.timeout = timeout;
        return this;
    }

    public TcpClientConnectionFactory tcpChannelGroup(TcpChannelGroup tcpChannelGroup) {
        this.tcpChannelGroup = tcpChannelGroup;
        return this;
    }

    public TcpClientConnectionFactory secureEngineFactory(SecureEngineFactory secureEngineFactory) {
        this.secureEngineFactory = secureEngineFactory;
        return this;
    }

    public CompletableFuture<TcpConnection> connect(InetSocketAddress inetSocketAddress, boolean secure) {
        return connect(inetSocketAddress, secure, Collections.emptyList());
    }

    public CompletableFuture<TcpConnection> connect(InetSocketAddress inetSocketAddress, boolean secure, List<String> supportedProtocols) {
        CompletableFuture<TcpConnection> future;
        if (secure) {
            if (CollectionUtils.isEmpty(supportedProtocols)) {
                future = secureTcpClient.connect(inetSocketAddress);
            } else {
                future = secureTcpClient.connect(inetSocketAddress, supportedProtocols);
            }
        } else {
            future = tcpClient.connect(inetSocketAddress);
        }
        return future;
    }

    @Override
    protected void init() {
        Assert.notNull(tcpChannelGroup, "The tcp channel group must be not null");
        tcpClient = TcpClientFactory
                .create()
                .tcpChannelGroup(tcpChannelGroup)
                .stopTcpChannelGroup(stopTcpChannelGroup)
                .timeout(timeout);
        secureTcpClient = TcpClientFactory
                .create()
                .tcpChannelGroup(tcpChannelGroup)
                .stopTcpChannelGroup(stopTcpChannelGroup)
                .timeout(timeout)
                .enableSecureConnection();
        if (secureEngineFactory != null) {
            secureTcpClient.secureEngineFactory(secureEngineFactory);
        }
    }

    @Override
    protected void destroy() {
        tcpClient.stop();
        secureTcpClient.stop();
    }
}
