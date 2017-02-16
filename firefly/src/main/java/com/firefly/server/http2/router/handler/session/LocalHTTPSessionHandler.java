package com.firefly.server.http2.router.handler.session;

import com.firefly.server.http2.router.Handler;
import com.firefly.server.http2.router.RoutingContext;
import com.firefly.utils.concurrent.Scheduler;
import com.firefly.utils.concurrent.Schedulers;
import com.firefly.utils.lang.AbstractLifeCycle;

/**
 * @author Pengtao Qiu
 */
public class LocalHTTPSessionHandler extends AbstractLifeCycle implements Handler {

    private final SessionStore sessionStore;
    private final HTTPSessionConfiguration configuration;
    private final Scheduler scheduler;

    public LocalHTTPSessionHandler(HTTPSessionConfiguration configuration) {
        this.configuration = configuration;
        this.sessionStore = new LocalSessionStore();
        this.scheduler = Schedulers.createScheduler(configuration.getSchedulerPoolSize());
        start();
    }

    @Override
    public void handle(RoutingContext ctx) {
        HTTPSessionHandlerSPIImpl sessionHandlerSPI = new HTTPSessionHandlerSPIImpl(sessionStore, ctx, scheduler, configuration);
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
