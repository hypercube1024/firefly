package com.firefly.net.tcp.aio;

import com.codahale.metrics.ScheduledReporter;
import com.firefly.net.Config;
import com.firefly.net.EventManager;
import com.firefly.net.event.DefaultEventManager;
import com.firefly.net.exception.NetException;
import com.firefly.utils.lang.AbstractLifeCycle;
import com.firefly.utils.time.Millisecond100Clock;
import org.slf4j.ILoggerFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.nio.channels.AsynchronousChannelGroup;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.ForkJoinWorkerThread;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author Pengtao Qiu
 */
abstract public class AbstractTcpLifeCycle extends AbstractLifeCycle {

    protected static Logger log = LoggerFactory.getLogger("firefly-system");

    protected Config config;
    protected AtomicInteger sessionId = new AtomicInteger();
    protected AsynchronousChannelGroup group;
    protected AsynchronousTcpWorker worker;
    protected ScheduledReporter reporter;

    abstract protected String getThreadName();

    @Override
    protected void init() {
        if (config == null)
            throw new NetException("server configuration is null");

        try {
            group = AsynchronousChannelGroup.withThreadPool(new ForkJoinPool
                    (config.getAsynchronousCorePoolSize(),
                            pool -> {
                                ForkJoinWorkerThread worker = ForkJoinPool.defaultForkJoinWorkerThreadFactory.newThread(pool);
                                worker.setName(getThreadName() + worker.getPoolIndex());
                                return worker;
                            },
                            null, true));
            log.info(config.toString());
            EventManager eventManager = new DefaultEventManager(config);
            worker = new AsynchronousTcpWorker(config, eventManager);
            if (config.isMonitorEnable()) {
                reporter = config.getReporterFactory().call(config.getMetrics());
                reporter.start(10, TimeUnit.SECONDS);
            }
        } catch (IOException e) {
            log.error("initialization server channel group error", e);
        }
    }

    @Override
    protected void destroy() {
        if (group != null) {
            group.shutdown();
        }
        if (config.isMonitorEnable()) {
            reporter.stop();
        }
        ILoggerFactory iLoggerFactory = LoggerFactory.getILoggerFactory();
        try {
            iLoggerFactory.getClass().getDeclaredMethod("stop").invoke(iLoggerFactory);
        } catch (IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
            System.out.println(e.getMessage());
        }
        Millisecond100Clock.stop();
    }
}
