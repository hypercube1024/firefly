package com.firefly.db;

import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.ScheduledReporter;

/**
 * @author Pengtao Qiu
 */
public interface MetricReporterFactory {

    ScheduledReporter getScheduledReporter();

    MetricRegistry getMetricRegistry();

}
