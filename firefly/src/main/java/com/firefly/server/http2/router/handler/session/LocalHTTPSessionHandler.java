package com.firefly.server.http2.router.handler.session;

import com.firefly.server.http2.router.Handler;
import com.firefly.server.http2.router.RoutingContext;
import com.firefly.utils.concurrent.Scheduler;
import com.firefly.utils.concurrent.Schedulers;
import com.firefly.utils.lang.AbstractLifeCycle;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * @author Pengtao Qiu
 */
public class LocalHTTPSessionHandler extends AbstractLifeCycle implements Handler {

    private final ConcurrentMap<String, HTTPSessionImpl> sessionMap;
    private final LocalHTTPSessionConfiguration configuration;
    private final Scheduler scheduler;

    public LocalHTTPSessionHandler(LocalHTTPSessionConfiguration configuration) {
        this.configuration = configuration;
        this.sessionMap = new ConcurrentHashMap<>();
        this.scheduler = Schedulers.createScheduler(configuration.getSchedulerPoolSize());
        start();
    }

    @Override
    public void handle(RoutingContext ctx) {
        HTTPSessionHandlerSPIImpl sessionHandlerSPI = new HTTPSessionHandlerSPIImpl(sessionMap, ctx, scheduler, configuration);
        ctx.setHTTPSessionHandlerSPI(sessionHandlerSPI);
    }

    @Override
    protected void init() {
    }

    @Override
    protected void destroy() {
        scheduler.stop();
    }
}
