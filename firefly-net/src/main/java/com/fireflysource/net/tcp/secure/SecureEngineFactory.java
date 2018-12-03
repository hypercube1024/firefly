package com.fireflysource.net.tcp.secure;

import com.fireflysource.net.tcp.TcpConnection;

import java.io.IOException;
import java.util.List;

/**
 * @author Pengtao Qiu
 */
public interface SecureEngineFactory {

    SecureEngine create(TcpConnection connection, boolean clientMode, List<String> supportedProtocols) throws IOException;

    SecureEngine create(TcpConnection connection, boolean clientMode, String peerHost, int peerPort,
                        List<String> supportedProtocols) throws IOException;

}
