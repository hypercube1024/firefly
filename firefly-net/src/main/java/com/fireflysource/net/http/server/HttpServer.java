package com.fireflysource.net.http.server;

import com.fireflysource.common.lifecycle.LifeCycle;

/**
 * @author Pengtao Qiu
 */
public interface HttpServer extends LifeCycle {

    /**
     * Register a new router.
     *
     * @return The new router.
     */
    HttpServerBuilder router();

    /**
     * Register a new router.
     *
     * @param id The router id.
     * @return The new router.
     */
    HttpServerBuilder router(int id);
}
