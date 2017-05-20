package org.firefly.slf4j;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

public class Slf4jImplDemo {

    private static final Logger logger = LoggerFactory.getLogger("firefly-common");

    public static void main(String[] args) {
        MDC.put("reqId", "hello req");
        MDC.put("current user", "fuck f");
        logger.info("test slf4j log");
        logger.info("what's");
    }

}
