package com.fireflysource.log.demo;

import com.fireflysource.log.slf4j.LazyLogger;
import org.slf4j.MDC;

public class Slf4jImplDemo {

    private static final LazyLogger logger = LazyLogger.create();

    public static void main(String[] args) {
        MDC.put("reqId", "hello req");
        MDC.put("current user", "fuck f");

        logger.info(() -> "test lazy log: " + dumpLargeData());
        logger.info(() -> "what's");

        // logger level is INFO
        logger.debug(() -> "debug dump data: " + dumpLargeData());
        if (logger.isDebugEnabled()) {
            logger.getLogger().debug("debug dump data: ", dumpLargeData());
        }
    }

    private static String dumpLargeData() {
        System.out.println("dump......");
        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return "large data";
    }

}
