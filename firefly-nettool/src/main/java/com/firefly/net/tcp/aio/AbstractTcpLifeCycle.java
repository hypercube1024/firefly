package com.firefly.net.tcp.aio;

import com.codahale.metrics.ScheduledReporter;
import com.firefly.net.Config;
import com.firefly.net.event.DefaultNetEvent;
import com.firefly.net.exception.NetException;
import com.firefly.utils.lang.AbstractLifeCycle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.channels.AsynchronousChannelGroup;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author Pengtao Qiu
 */
abstract public class AbstractTcpLifeCycle extends AbstractLifeCycle {

    protected static Logger log = LoggerFactory.getLogger("firefly-system");

    protected Config config;
    protected AtomicInteger sessionId = new AtomicInteger();
    protected AsynchronousChannelGroup group;
    protected ExecutorService netExecutorService;
    protected AsynchronousTcpWorker worker;
    protected ScheduledReporter reporter;

    abstract protected String getThreadName();

    public ExecutorService getNetExecutorService() {
        return netExecutorService;
    }

    @Override
    protected void init() {
        if (config == null)
            throw new NetException("server configuration is null");

        try {
            if (config.getAsynchronousCorePoolSize() <= 0) {
                ThreadFactory threadFactory = new ThreadFactory() {

                    private AtomicInteger threadId = new AtomicInteger();

                    @Override
                    public Thread newThread(Runnable r) {
                        return new Thread(r, getThreadName() + threadId.getAndIncrement());
                    }
                };
                netExecutorService = Executors.newCachedThreadPool(threadFactory);
                log.info("init the cached thread pool");
            } else if (config.getAsynchronousCorePoolSize() <= Config.defaultPoolSize * 2) {
                netExecutorService = new ForkJoinPool
                        (config.getAsynchronousCorePoolSize(), pool -> {
                            ForkJoinWorkerThread worker = ForkJoinPool.defaultForkJoinWorkerThreadFactory.newThread(pool);
                            worker.setName(getThreadName() + worker.getPoolIndex());
                            return worker;
                        }, null, true);
                log.info("init the fork join pool");
            } else {
                ThreadFactory threadFactory = new ThreadFactory() {

                    private AtomicInteger threadId = new AtomicInteger();

                    @Override
                    public Thread newThread(Runnable r) {
                        return new Thread(r, getThreadName() + threadId.getAndIncrement());
                    }
                };
                netExecutorService = new ThreadPoolExecutor(Config.defaultPoolSize, config.getAsynchronousCorePoolSize(),
                        15L, TimeUnit.SECONDS,
                        new LinkedBlockingQueue<>(),
                        threadFactory);
                log.info("init the fixed size thread pool");
            }
            group = AsynchronousChannelGroup.withThreadPool(netExecutorService);
            log.info(config.toString());
            worker = new AsynchronousTcpWorker(config, new DefaultNetEvent(config));
            if (config.isMonitorEnable()) {
                reporter = config.getMetricReporterFactory().getScheduledReporter();
                try {
                    reporter.start(10, TimeUnit.SECONDS);
                } catch (Exception e) {
                    log.error("start metric reporter exception -> {}", e.getMessage());
                }
            }
        } catch (IOException e) {
            log.error("initialization server channel group error", e);
        }
    }

    @Override
    protected void destroy() {
        if (group != null) {
            try {
                group.shutdown();
            } catch (Exception e) {
                log.error("aio tcp thread group shutdown exception -> {}", e.getMessage());
            }
        }
        if (config.isMonitorEnable()) {
            try {
                reporter.stop();
            } catch (Exception e) {
                log.error("stop metric reporter exception -> {}", e.getMessage());
            }
        }
    }
}
