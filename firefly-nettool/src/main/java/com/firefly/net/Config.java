package com.firefly.net;

import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.ScheduledReporter;
import com.codahale.metrics.Slf4jReporter;
import com.firefly.utils.function.Func0;
import org.slf4j.LoggerFactory;

import java.util.concurrent.TimeUnit;

public class Config {

    private int timeout = 30 * 1000;

    // asynchronous I/O thread pool settings
    private int asynchronousCorePoolSize = Runtime.getRuntime().availableProcessors();
    private int asynchronousMaximumPoolSize = Runtime.getRuntime().availableProcessors() * 2;
    private int asynchronousPoolKeepAliveTime = 15 * 1000;

    private String serverName = "firefly-server";
    private String clientName = "firefly-client";

    private Decoder decoder;
    private Encoder encoder;
    private Handler handler;

    private boolean monitorEnable = true;
    private MetricRegistry metrics = new MetricRegistry();
    private Func0<ScheduledReporter> reporterFactory = () -> Slf4jReporter.forRegistry(this.metrics)
                                                                          .outputTo(LoggerFactory.getLogger("firefly-monitor"))
                                                                          .convertRatesTo(TimeUnit.SECONDS)
                                                                          .convertDurationsTo(TimeUnit.MILLISECONDS)
                                                                          .build();

    /**
     * The max I/O idle time, the default value is 10 seconds.
     *
     * @return The max I/O idle time，the unit is MS.
     */
    public int getTimeout() {
        return timeout;
    }

    /**
     * The I/O timeout, if the last I/O timestamp before present over timeout
     * value, the session will close.
     *
     * @param timeout the max I/O idle time，the unit is MS.
     */
    public void setTimeout(int timeout) {
        this.timeout = timeout;
    }

    public String getServerName() {
        return serverName;
    }

    public void setServerName(String serverName) {
        this.serverName = serverName;
    }

    public String getClientName() {
        return clientName;
    }

    public void setClientName(String clientName) {
        this.clientName = clientName;
    }

    public Decoder getDecoder() {
        return decoder;
    }

    public void setDecoder(Decoder decoder) {
        this.decoder = decoder;
    }

    public Encoder getEncoder() {
        return encoder;
    }

    public void setEncoder(Encoder encoder) {
        this.encoder = encoder;
    }

    public Handler getHandler() {
        return handler;
    }

    public void setHandler(Handler handler) {
        this.handler = handler;
    }

    public int getAsynchronousMaximumPoolSize() {
        return asynchronousMaximumPoolSize;
    }

    public void setAsynchronousMaximumPoolSize(int asynchronousMaximumPoolSize) {
        this.asynchronousMaximumPoolSize = asynchronousMaximumPoolSize;
    }

    public int getAsynchronousCorePoolSize() {
        return asynchronousCorePoolSize;
    }

    public void setAsynchronousCorePoolSize(int asynchronousCorePoolSize) {
        this.asynchronousCorePoolSize = asynchronousCorePoolSize;
    }

    public int getAsynchronousPoolKeepAliveTime() {
        return asynchronousPoolKeepAliveTime;
    }

    public void setAsynchronousPoolKeepAliveTime(int asynchronousPoolKeepAliveTime) {
        this.asynchronousPoolKeepAliveTime = asynchronousPoolKeepAliveTime;
    }

    public MetricRegistry getMetrics() {
        return metrics;
    }

    public void setMetrics(MetricRegistry metrics) {
        this.metrics = metrics;
    }

    public Func0<ScheduledReporter> getReporterFactory() {
        return reporterFactory;
    }

    public void setReporterFactory(Func0<ScheduledReporter> reporterFactory) {
        this.reporterFactory = reporterFactory;
    }

    public boolean isMonitorEnable() {
        return monitorEnable;
    }

    public void setMonitorEnable(boolean monitorEnable) {
        this.monitorEnable = monitorEnable;
    }

    @Override
    public String toString() {
        return "Asynchronous TCP configuration [timeout=" + timeout + ", asynchronousCorePoolSize="
                + asynchronousCorePoolSize + ", asynchronousMaximumPoolSize=" + asynchronousMaximumPoolSize
                + ", asynchronousPoolKeepAliveTime=" + asynchronousPoolKeepAliveTime + "]";
    }
}
