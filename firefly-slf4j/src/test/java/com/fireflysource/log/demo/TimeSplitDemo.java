package com.fireflysource.log.demo;

import com.fireflysource.log.slf4j.LazyLogger;

import java.util.concurrent.TimeUnit;

/**
 * @author Pengtao Qiu
 */
public class TimeSplitDemo {

    public static final LazyLogger log = LazyLogger.create("time-split-minute");

    public static void main(String[] args) throws Exception {
        while (true) {
            log.info(() -> "test1");
            Thread.sleep(TimeUnit.SECONDS.toMillis(5));
        }
    }
}
