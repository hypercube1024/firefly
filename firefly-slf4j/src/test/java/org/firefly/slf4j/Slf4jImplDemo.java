package org.firefly.slf4j;

import com.firefly.utils.concurrent.ThreadUtils;
import com.firefly.utils.log.slf4j.ext.LazyLogger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

public class Slf4jImplDemo {

    private static final LazyLogger logger = LazyLogger.create();

    public static void main(String[] args) {
        MDC.put("reqId", "hello req");
        MDC.put("current user", "fuck f");
        // logger level is INFO
        logger.info(() -> "test lazy log: " + dumpLargeData());
        logger.info(() -> "what's");
        logger.debug(() -> "debug dump data: " + dumpLargeData());
    }

    static String dumpLargeData() {
        ThreadUtils.sleep(2000);
        return "large data";
    }

}
