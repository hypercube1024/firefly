package com.firefly.net.tcp.flex.metric;

import com.codahale.metrics.Counter;
import com.codahale.metrics.Meter;
import com.codahale.metrics.MetricRegistry;

/**
 * @author Pengtao Qiu
 */
public class FlexMetric {

    private final Counter activeStreamCount;
    private final Meter requestMeter;

    public FlexMetric(MetricRegistry metrics, String prefix) {
        activeStreamCount = metrics.counter(prefix + ".activeStreamCount");
        requestMeter = metrics.meter(prefix + ".requestMeter");
    }

    public Counter getActiveStreamCount() {
        return activeStreamCount;
    }

    public Meter getRequestMeter() {
        return requestMeter;
    }
}
