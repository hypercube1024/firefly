package com.firefly.server.http2.router;

import com.firefly.server.http2.router.impl.RouterManagerImpl;

/**
 * @author Pengtao Qiu
 */
public interface RouterManager extends RequestAcceptor {

    Router register();

    static RouterManager create() {
        return new RouterManagerImpl();
    }

    static RouterManager createEmptyRouterManager() {
        return new RouterManagerImpl();
    }
}
