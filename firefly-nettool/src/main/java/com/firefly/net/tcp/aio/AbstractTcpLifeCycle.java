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
import java.util.concurrent.ExecutorService;
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
            netExecutorService = new ForkJoinPool
                    (config.getAsynchronousCorePoolSize(), pool -> {
                        ForkJoinWorkerThread worker = ForkJoinPool.defaultForkJoinWorkerThreadFactory.newThread(pool);
                        worker.setName(getThreadName() + worker.getPoolIndex());
                        return worker;
                    }, null, true);
            group = AsynchronousChannelGroup.withThreadPool(netExecutorService);
            log.info(config.toString());
            EventManager eventManager = new DefaultEventManager(config);
            worker = new AsynchronousTcpWorker(config, eventManager);
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
        ILoggerFactory iLoggerFactory = LoggerFactory.getILoggerFactory();
        try {
            iLoggerFactory.getClass().getDeclaredMethod("stop").invoke(iLoggerFactory);
        } catch (IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
            System.out.println(e.getMessage());
        }
        Millisecond100Clock.stop();
    }
}
