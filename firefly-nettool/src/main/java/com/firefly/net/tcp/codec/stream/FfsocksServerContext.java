package com.firefly.net.tcp.codec.stream;

import java.io.OutputStream;

/**
 * @author Pengtao Qiu
 */
public interface FfsocksServerContext extends FfsocksContext {

    void end();

    OutputStream getOutputStream();

}
