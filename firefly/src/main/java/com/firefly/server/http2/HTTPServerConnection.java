package com.firefly.server.http2;

import com.firefly.codec.http2.stream.HTTPConnection;
import com.firefly.codec.http2.stream.HTTPTunnelConnection;
import com.firefly.utils.concurrent.Promise;

import java.util.concurrent.CompletableFuture;

public interface HTTPServerConnection extends HTTPConnection {

    void upgradeHTTPTunnel(Promise<HTTPTunnelConnection> promise);

    CompletableFuture<HTTPTunnelConnection> upgradeHTTPTunnel();

}
