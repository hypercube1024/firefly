package com.firefly.db;

import com.codahale.metrics.MetricRegistry;
import com.firefly.utils.time.SafeSimpleDateFormat;
import com.github.rollingmetrics.top.Top;
import com.github.rollingmetrics.top.TopMetricSet;

import java.time.Duration;
import java.util.Date;
import java.util.concurrent.TimeUnit;

/**
 * @author Pengtao Qiu
 */
public class LatencyTopTracker {

    private final Top top;

    public LatencyTopTracker(MetricRegistry metricRegistry) {
        this.top = Top.builder(10) // 10 positions in the top
                      .withLatencyThreshold(Duration.ofMillis(25)) // do not care about queries which shorter than 25ms
                      .resetPositionsPeriodicallyByChunks(Duration.ofSeconds(60), 3) // position recorded in the top will take effect 60-80 seconds
                      .build();
        metricRegistry.registerAll(new TopMetricSet("jdbc-query-top", top, TimeUnit.MILLISECONDS, 3));
    }

    public void update(String sql, Exception exception, long currentTimestamp, long latencyTime) {
        top.update(currentTimestamp, latencyTime, TimeUnit.MILLISECONDS, () -> sql +
                (exception != null ? ("| " + exception.getMessage() + "| ") : "| ") +
                SafeSimpleDateFormat.defaultDateFormat.format(new Date(currentTimestamp)));
    }
}
