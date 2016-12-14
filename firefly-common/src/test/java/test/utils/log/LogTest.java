package test.utils.log;

import com.firefly.utils.StringUtils;
import com.firefly.utils.io.FileUtils;
import com.firefly.utils.log.Log;
import com.firefly.utils.log.LogFactory;
import com.firefly.utils.log.file.FileLog;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import test.utils.log.foo.Foo;
import test.utils.log.foo.bar.Bar;

import java.io.File;
import java.io.IOException;
import java.util.Date;

public class LogTest {

    private static final Log log = LogFactory.getInstance().getLog("firefly-common");
    private static final Log log2 = LogFactory.getInstance().getLog("test-TRACE");
    private static final Log log3 = LogFactory.getInstance().getLog("test-DEBUG");
    private static final Log log4 = LogFactory.getInstance().getLog("test-ERROR");
    private static final Log log5 = LogFactory.getInstance().getLog("test-WARN");
    private static final Log log6 = LogFactory.getInstance().getLog("test-INFO");
    private static final Log logConsole = LogFactory.getInstance().getLog("test-console");
    private static final Log defaultLog = LogFactory.getInstance().getLog("firefly-system");
    private static final Log illegalLog = LogFactory.getInstance().getLog("test-illegal");

    private static final Log logFoo = LogFactory.getInstance().getLog(Foo.class);
    private static final Log logBar = LogFactory.getInstance().getLog(Bar.class);

    private static final Log testMaxSize = LogFactory.getInstance().getLog("test.max.size");
    private static final Log testGBK = LogFactory.getInstance().getLog("test.gbk");

    @Before
    public void init() {
        deleteLog(log2);
        deleteLog(log3);
        deleteLog(log4);
        deleteLog(log5);
        deleteLog(log6);
        deleteLog(logFoo);
        deleteLog(logBar);
    }

    private void deleteLog(Log log) {
        File file = getFile(log);
        if (file != null && file.exists())
            file.delete();
    }

    private File getFile(Log log) {
        if (log instanceof FileLog) {
            FileLog fileLog = (FileLog) log;
            File file = new File(fileLog.getPath(), fileLog.getName() + "." + LogFactory.DAY_DATE_FORMAT.format(new Date()) + ".txt");
            if (file.exists())
                return file;
            else
                return null;
        }
        return null;
    }

    @Test
    public void test() {
        // test trace
        log2.trace("trace log");
        log2.debug("debug log");
        log2.info("info log");
        log2.warn("warn log");
        log2.error("error log");

        // test debug
        log3.trace("trace log");
        log3.debug("debug log");
        log3.info("info log");
        log3.warn("warn log");
        log3.error("error log");

        // test info
        log6.trace("trace log");
        log6.debug("debug log");
        log6.info("info log");
        log6.warn("warn log");
        log6.error("error log");

        // test warn
        log5.trace("trace log");
        log5.debug("debug log");
        log5.info("info log");
        log5.warn("warn log");
        log5.error("error log");

        // test error
        log4.trace("trace log");
        log4.debug("debug log");
        log4.info("info log");
        log4.warn("warn log");
        log4.error("error log");

        logFoo.info("testFoo");
        logBar.info("testBar");

        try {
            Thread.sleep(2000L);
        } catch (InterruptedException e1) {
            e1.printStackTrace();
        }
        try {
            FileUtils.read(getFile(logFoo), (text, num) -> {
                String[] data = StringUtils.split(text, '\t');
                Assert.assertEquals("testFoo", data[1]);
            }, "utf-8");

            FileUtils.read(getFile(logBar), (text, num) -> {
                String[] data = StringUtils.split(text, '\t');
                Assert.assertEquals("testBar", data[1]);
            }, "utf-8");

            // test trace
            FileUtils.read(getFile(log2), (text, num) -> {
                String[] data = StringUtils.split(text, '\t');
                switch (num) {
                    case 1:
                        Assert.assertEquals("trace log", data[1]);
                        break;
                    case 2:
                        Assert.assertEquals("debug log", data[1]);
                        break;
                    case 3:
                        Assert.assertEquals("info log", data[1]);
                        break;
                    case 4:
                        Assert.assertEquals("warn log", data[1]);
                        break;
                    case 5:
                        Assert.assertEquals("error log", data[1]);
                        break;
                    default:
                        break;
                }

            }, "UTF-8");

            // test debug
            FileUtils.read(getFile(log3), (text, num) -> {
                String[] data = StringUtils.split(text, '\t');
                switch (num) {
                    case 1:
                        Assert.assertEquals("debug log", data[1]);
                        break;
                    case 2:
                        Assert.assertEquals("info log", data[1]);
                        break;
                    case 3:
                        Assert.assertEquals("warn log", data[1]);
                        break;
                    case 4:
                        Assert.assertEquals("error log", data[1]);
                        break;
                    default:
                        break;
                }

            }, "UTF-8");

            // test info
            FileUtils.read(getFile(log6), (text, num) -> {
                String[] data = StringUtils.split(text, '\t');
                switch (num) {
                    case 1:
                        Assert.assertEquals("info log", data[1]);
                        break;
                    case 2:
                        Assert.assertEquals("warn log", data[1]);
                        break;
                    case 3:
                        Assert.assertEquals("error log", data[1]);
                        break;
                    default:
                        break;
                }

            }, "UTF-8");

            // test warn
            FileUtils.read(getFile(log5), (text, num) -> {
                String[] data = StringUtils.split(text, '\t');
                switch (num) {
                    case 1:
                        Assert.assertEquals("warn log", data[1]);
                        break;
                    case 2:
                        Assert.assertEquals("error log", data[1]);
                        break;
                    default:
                        break;
                }

            }, "UTF-8");

            // test error
            FileUtils.read(getFile(log4), (text, num) -> {
                String[] data = StringUtils.split(text, '\t');
                switch (num) {
                    case 1:
                        Assert.assertEquals("error log", data[1]);
                        break;
                    default:
                        break;
                }

            }, "UTF-8");
        } catch (IOException e) {
            e.printStackTrace();
        }
        LogFactory.getInstance().stop();
    }

    public static void main(String[] args) throws Throwable {
        long data = 0;
        while (true) {
            testMaxSize.info("test 测试 {} data {}", "log", data);
			testGBK.info("测试中文gbk");
            data++;
            Thread.sleep(1000);
        }
    }

    public static void main2(String[] args) throws InterruptedException {
        try {
            log.info("test {} aa {}", "log1", 2);
            log.info("test {} bb {}", "log1", 2);
            log.info("test {} cc {}", "log1", 2);
            log.debug("cccc");
            log.warn("warn hello");

            log2.trace("test trace");
            log2.trace("log2 {} dfdfdf", 3, 5);
            log2.debug("cccc");

            log3.debug("log3", "dfd");
            log3.info("ccccddd");

            log4.error("log4");
            log4.warn("ccc");

            log5.warn("log5 {} {}", "warn");
            log5.error("log5 {}", "error");
            log5.trace("ccsc");

            logConsole.info("test {} console", "hello");
            logConsole.debug("ccc");
            logConsole.warn("dsfsf {} cccc", 33);

            defaultLog.debug("default log debug");
            defaultLog.info("default log info");
            defaultLog.warn("default log warn");
            defaultLog.error("default log error");

            illegalLog.trace("test log trace");
            illegalLog.debug("test log debug");
            illegalLog.info("test log info");
            illegalLog.warn("test log warn");
            illegalLog.error("test log error");

            try {
                test3();
            } catch (Throwable t) {
                log4.error("test exception", t);
            }
            Thread.sleep(3000L);
        } finally {
            LogFactory.getInstance().stop();
        }
    }

    public static void test1() {
        throw new RuntimeException();
    }

    public static void test2() {
        test1();
    }

    public static void test3() {
        test2();
    }

}
