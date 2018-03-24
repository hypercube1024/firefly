package com.firefly.net;

import com.firefly.utils.ServiceUtils;

public class Config {

    public static final int defaultTimeout = Integer.getInteger("com.firefly.net.defaultTimeout", 30 * 1000);
    public static final int defaultPoolSize = Integer.getInteger("com.firefly.net.defaultPoolSize", Runtime.getRuntime().availableProcessors() * 2);

    private int timeout = defaultTimeout;

    // I/O thread pool size
    private int asynchronousCorePoolSize = defaultPoolSize;

    private String serverName = "firefly-server";
    private String clientName = "firefly-client";

    private Decoder decoder;
    private Encoder encoder;
    private Handler handler;

    private boolean monitorEnable = true;
    private MetricReporterFactory metricReporterFactory = ServiceUtils.loadService(MetricReporterFactory.class, new DefaultMetricReporterFactory());

    /**
     * Get the max I/O idle time, the default value is 10 seconds.
     *
     * @return Max I/O idle time. The unit is millisecond.
     */
    public int getTimeout() {
        return timeout;
    }

    /**
     * Set the I/O timeout, if the last I/O timestamp before present over timeout
     * value, the session will close.
     *
     * @param timeout Max I/O idle time. The time unit is millisecond.
     */
    public void setTimeout(int timeout) {
        this.timeout = timeout;
    }

    /**
     * Get the server name. The I/O thread name contains server name. It helps you debug codes.
     *
     * @return server name
     */
    public String getServerName() {
        return serverName;
    }

    /**
     * Set the server name. The I/O thread name contains server name. It helps you debug codes.
     *
     * @param serverName Server name.
     */
    public void setServerName(String serverName) {
        this.serverName = serverName;
    }

    /**
     * Get the client name. If you start a client, the I/O thread name contains client name. It helps you debug codes.
     *
     * @return client name
     */
    public String getClientName() {
        return clientName;
    }

    /**
     * Set the client name. If you start a client, the I/O thread name contains client name. It helps you debug codes.
     *
     * @param clientName client name
     */
    public void setClientName(String clientName) {
        this.clientName = clientName;
    }

    /**
     * Get the decoder. When the server or client receives data, it will call the Decoder. You can write the protocol parser in Decoder.
     *
     * @return decoder
     */
    public Decoder getDecoder() {
        return decoder;
    }

    /**
     * Set the decoder. When the server or client receives data, it will call the Decoder. You can write the protocol parser in Decoder.
     *
     * @param decoder decoder
     */
    public void setDecoder(Decoder decoder) {
        this.decoder = decoder;
    }

    /**
     * Get the encoder. You can write the protocol generator in Encoder.
     *
     * @return encoder
     */
    public Encoder getEncoder() {
        return encoder;
    }

    /**
     * Set the encoder. You can write the protocol generator in Encoder.
     *
     * @param encoder encoder
     */
    public void setEncoder(Encoder encoder) {
        this.encoder = encoder;
    }

    /**
     * Get the handler. It is the handler of network events.
     * Such as creating a session, closing session, receiving a message and throwing the exception.
     *
     * @return Handler
     */
    public Handler getHandler() {
        return handler;
    }

    /**
     * Set the handler. It is the handler of network events.
     * Such as creating a session, closing session, receiving a message and throwing the exception.
     *
     * @param handler Handler
     */
    public void setHandler(Handler handler) {
        this.handler = handler;
    }

    /**
     * Get the I/O thread pool size. The default size equals the processor number.
     *
     * @return I/O thread pool size
     */
    public int getAsynchronousCorePoolSize() {
        return asynchronousCorePoolSize;
    }

    /**
     * Set the I/O thread pool size. The default size equals the processor number.
     *
     * @param asynchronousCorePoolSize I/O thread pool size
     */
    public void setAsynchronousCorePoolSize(int asynchronousCorePoolSize) {
        this.asynchronousCorePoolSize = asynchronousCorePoolSize;
    }

    /**
     * Get the MetricReporterFactory. The default reporter is slf4j.
     *
     * @return MetricReporterFactory
     */
    public MetricReporterFactory getMetricReporterFactory() {
        return metricReporterFactory;
    }

    /**
     * Set the MetricReporterFactory. The default reporter is slf4j.
     *
     * @param metricReporterFactory MetricReporterFactory
     */
    public void setMetricReporterFactory(MetricReporterFactory metricReporterFactory) {
        this.metricReporterFactory = metricReporterFactory;
    }

    /**
     * If the monitorEnable is true, the server or client will record runtime performance data to a metric reporter.
     *
     * @return monitorEnable The default value is true.
     */
    public boolean isMonitorEnable() {
        return monitorEnable;
    }

    /**
     * If the monitorEnable is true, the server or client will record runtime performance data to a metric reporter.
     *
     * @param monitorEnable monitorEnable. The default value is true.
     */
    public void setMonitorEnable(boolean monitorEnable) {
        this.monitorEnable = monitorEnable;
    }

    @Override
    public String toString() {
        return "Firefly asynchronous TCP configuration {" +
                "timeout=" + timeout +
                ", asynchronousCorePoolSize=" + asynchronousCorePoolSize +
                '}';
    }
}
