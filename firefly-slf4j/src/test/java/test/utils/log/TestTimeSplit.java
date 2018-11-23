package test.utils.log;

import com.firefly.utils.concurrent.ThreadUtils;
import com.firefly.utils.log.Log;
import com.firefly.utils.log.LogFactory;

import java.util.concurrent.TimeUnit;

/**
 * @author Pengtao Qiu
 */
public class TestTimeSplit {

    private static final Log log = LogFactory.getInstance().getLog("time-split-minute");

    public static void main(String[] args) {
        while (true) {
            log.info("test1");
            ThreadUtils.sleep(5, TimeUnit.SECONDS);
        }
    }
}
