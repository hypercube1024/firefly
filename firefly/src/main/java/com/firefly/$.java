package com.firefly;

import com.firefly.client.http2.SimpleHTTPClient;
import com.firefly.client.http2.SimpleHTTPClientConfiguration;
import com.firefly.net.tcp.SimpleTcpClient;
import com.firefly.net.tcp.SimpleTcpServer;
import com.firefly.net.tcp.TcpConfiguration;
import com.firefly.net.tcp.TcpServerConfiguration;
import com.firefly.server.http2.SimpleHTTPServer;
import com.firefly.server.http2.SimpleHTTPServerConfiguration;

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
}
