package com.firefly.server.http2.router.handler.session;

import com.firefly.server.http2.router.Handler;
import com.firefly.server.http2.router.RoutingContext;
import com.firefly.server.http2.router.spi.HTTPSessionHandlerSPI;
import com.firefly.utils.concurrent.Scheduler;
import com.firefly.utils.concurrent.Schedulers;
import com.firefly.utils.lang.AbstractLifeCycle;

/**
 * @author Pengtao Qiu
 */
abstract public class AbstractSessionHandler extends AbstractLifeCycle implements Handler {

    protected final SessionStore sessionStore;
    protected final HTTPSessionConfiguration configuration;
    protected final Scheduler scheduler;

    public AbstractSessionHandler(HTTPSessionConfiguration configuration) {
        this.configuration = configuration;
        this.sessionStore = createSessionStore();
        this.scheduler = Schedulers.createScheduler(configuration.getSchedulerPoolSize());
        start();
    }

    abstract public SessionStore createSessionStore();

    @Override
    public void handle(RoutingContext ctx) {
        HTTPSessionHandlerSPI sessionHandlerSPI = new HTTPSessionHandlerSPIImpl(sessionStore, ctx, scheduler, configuration);
        ctx.setHTTPSessionHandlerSPI(sessionHandlerSPI);
        ctx.next();
    }

    @Override
    protected void init() {
    }

    @Override
    protected void destroy() {
        scheduler.stop();
    }
}
