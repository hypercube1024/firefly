package com.firefly.net.tcp.codec.ffsocks.stream;

import com.firefly.net.Connection;

/**
 * @author Pengtao Qiu
 */
public interface FfsocksConnection extends Connection {

    Stream getStream();

    Session getSession();

}
