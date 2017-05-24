package com.firefly.server.http2.router;

import com.firefly.server.http2.SimpleRequest;

/**
 * @author Pengtao Qiu
 */
public interface RequestAcceptor {

    void accept(SimpleRequest request);

}
