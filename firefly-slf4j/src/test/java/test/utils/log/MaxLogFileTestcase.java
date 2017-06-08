package test.utils.log;

import com.firefly.utils.log.Log;
import com.firefly.utils.log.LogFactory;

/**
 * @author Pengtao Qiu
 */
public class MaxLogFileTestcase {

    private static final Log testMaxSize = LogFactory.getInstance().getLog("test.max.size");
    private static final Log testGBK = LogFactory.getInstance().getLog("test.gbk");

    public static void main(String[] args) throws Throwable {
        long data = 0;
        while (true) {
            testMaxSize.info("test 测试 {} data {}", "log", data);
            testGBK.info("测试中文gbk");
            data++;
            Thread.sleep(1000);
        }
    }
}
