package com.firefly;

import com.firefly.client.http2.SimpleHTTPClient;
import com.firefly.client.http2.SimpleHTTPClientConfiguration;
import com.firefly.net.tcp.SimpleTcpClient;
import com.firefly.net.tcp.SimpleTcpServer;
import com.firefly.net.tcp.TcpConfiguration;
import com.firefly.net.tcp.TcpServerConfiguration;
import com.firefly.server.http2.HTTP2ServerBuilder;
import com.firefly.server.http2.SimpleHTTPServer;
import com.firefly.server.http2.SimpleHTTPServerConfiguration;
import com.firefly.server.http2.router.RouterManager;
import com.firefly.server.http2.router.handler.body.HTTPBodyConfiguration;

/**
 * Unsorted utilities. The main functions of Firefly start from here.
 *
 * @author Pengtao Qiu
 */
abstract public class $ {

    public static SimpleHTTPClient createHTTPClient() {
        return new SimpleHTTPClient();
    }

    public static SimpleHTTPClient createHTTPClient(SimpleHTTPClientConfiguration configuration) {
        return new SimpleHTTPClient(configuration);
    }

    public static SimpleHTTPServer createHTTPServer() {
        return new SimpleHTTPServer();
    }

    public static SimpleHTTPServer createHTTPServer(SimpleHTTPServerConfiguration configuration) {
        return new SimpleHTTPServer(configuration);
    }

    public static SimpleTcpClient createTCPClient() {
        return new SimpleTcpClient();
    }

    public static SimpleTcpClient createTCPClient(TcpConfiguration configuration) {
        return new SimpleTcpClient(configuration);
    }

    public static SimpleTcpServer createTCPServer() {
        return new SimpleTcpServer();
    }

    public static SimpleTcpServer createTCPServer(TcpServerConfiguration configuration) {
        return new SimpleTcpServer(configuration);
    }

    public static RouterManager createRouterManager() {
        return RouterManager.create();
    }

    public static RouterManager create(HTTPBodyConfiguration configuration) {
        return RouterManager.create(configuration);
    }

    public static RouterManager createEmpty() {
        return RouterManager.createEmpty();
    }

    public static HTTP2ServerBuilder httpServer() {
        return new HTTP2ServerBuilder().httpServer();
    }

    public static HTTP2ServerBuilder httpServer(SimpleHTTPServerConfiguration serverConfiguration,
                                                HTTPBodyConfiguration httpBodyConfiguration) {
        return new HTTP2ServerBuilder().httpServer(serverConfiguration, httpBodyConfiguration);
    }
}
