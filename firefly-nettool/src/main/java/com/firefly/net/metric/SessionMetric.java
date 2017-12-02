package com.firefly.net.metric;

import com.codahale.metrics.Counter;
import com.codahale.metrics.Histogram;
import com.codahale.metrics.MetricRegistry;

/**
 * @author Pengtao Qiu
 */
public class SessionMetric {
    private final Counter activeSessionCount;
    private final Histogram duration;
    private final Histogram allocatedInputBufferSize;
    private final Histogram outputBufferQueueSize;
    private final Histogram mergedOutputBufferSize;

    public SessionMetric(MetricRegistry metrics, String prefix) {
        activeSessionCount = metrics.counter(prefix + ".activeSessionCount");
        duration = metrics.histogram(prefix + ".duration");
        outputBufferQueueSize = metrics.histogram(prefix + ".outputBufferQueueSize");
        mergedOutputBufferSize = metrics.histogram(prefix + ".mergedOutputBufferSize");
        allocatedInputBufferSize = metrics.histogram(prefix + ".allocatedInputBufferSize");
    }

    public Counter getActiveSessionCount() {
        return activeSessionCount;
    }

    public Histogram getDuration() {
        return duration;
    }

    public Histogram getAllocatedInputBufferSize() {
        return allocatedInputBufferSize;
    }

    public Histogram getOutputBufferQueueSize() {
        return outputBufferQueueSize;
    }

    public Histogram getMergedOutputBufferSize() {
        return mergedOutputBufferSize;
    }
}
