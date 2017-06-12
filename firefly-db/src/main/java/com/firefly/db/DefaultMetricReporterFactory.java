package com.firefly.db;

import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.ScheduledReporter;
import com.codahale.metrics.Slf4jReporter;
import org.slf4j.LoggerFactory;

import java.util.concurrent.TimeUnit;

/**
 * @author Pengtao Qiu
 */
public class DefaultMetricReporterFactory implements MetricReporterFactory {

    private ScheduledReporter scheduledReporter;
    private MetricRegistry metricRegistry;

    public DefaultMetricReporterFactory() {
        this.metricRegistry = new MetricRegistry();
        this.scheduledReporter = Slf4jReporter.forRegistry(metricRegistry)
                                              .outputTo(LoggerFactory.getLogger("firefly-monitor"))
                                              .convertRatesTo(TimeUnit.SECONDS)
                                              .convertDurationsTo(TimeUnit.MILLISECONDS)
                                              .build();
    }

    @Override
    public ScheduledReporter getScheduledReporter() {
        return scheduledReporter;
    }

    @Override
    public MetricRegistry getMetricRegistry() {
        return metricRegistry;
    }

}
