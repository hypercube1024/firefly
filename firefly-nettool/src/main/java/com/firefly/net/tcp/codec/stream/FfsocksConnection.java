package com.firefly.net.tcp.codec.stream;

import com.firefly.net.Connection;

/**
 * @author Pengtao Qiu
 */
public interface FfsocksConnection extends Connection {

    Stream getStream();

    Session getSession();

}
