package com.fireflysource.log.demo;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.TimeUnit;

/**
 * @author Pengtao Qiu
 */
public class TimeSplitDemo {

    public static final Logger log = LoggerFactory.getLogger("time-split-minute");

    public static void main(String[] args) throws Exception {
        int i = 0;
        while (true) {
            log.info("test1 {}, {}", i++, System.currentTimeMillis());
            Thread.sleep(TimeUnit.SECONDS.toMillis(5));
        }
    }
}
