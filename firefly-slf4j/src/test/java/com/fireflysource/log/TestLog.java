package com.fireflysource.log;

import com.fireflysource.log.foo.Foo;
import com.fireflysource.log.foo.bar.Bar;
import com.fireflysource.log.internal.utils.StringUtils;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;


class TestLog {

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
    private static final Log testRequestId = LogFactory.getInstance().getLog("test-request-id");
    private static MappedDiagnosticContext mdc;

    @BeforeAll
    static void init() {
        mdc = MappedDiagnosticContextFactory.getInstance().getMappedDiagnosticContext();
        deleteLog(log2);
        deleteLog(log3);
        deleteLog(log4);
        deleteLog(log5);
        deleteLog(log6);
        deleteLog(logFoo);
        deleteLog(logBar);
        deleteLog(testRequestId);
    }

    private static void deleteLog(Log log) {
        File file = getFile(log);
        if (file != null && file.exists()) {
            System.out.println("delete file " + file.getAbsolutePath());
            file.delete();
        }
    }

    private static File getFile(Log log) {
        System.out.println("getFile: " + log.getClass().getName());
        if (log instanceof ClassNameLogWrap) {
            ClassNameLogWrap classNameLogWrap = (ClassNameLogWrap) log;
            if (classNameLogWrap.getLog() instanceof FileLog) {
                FileLog fileLog = (FileLog) classNameLogWrap.getLog();
                File file = new File(fileLog.getPath(), fileLog.getName() + ".txt");
                System.out.println("getFile: " + fileLog.getPath() + ", " + fileLog.getName());
                System.out.println("getFile: " + file.exists());
                if (file.exists()) {
                    return file;
                }
            }
        }
        return null;
    }

    @AfterAll
    static void destroy() {
        LogFactory.getInstance().close();
    }

    public static void test1() {
        throw new RuntimeException();
    }

    public static void test2() {
        test1();
    }

//    public static void main(String[] args) {
//        try {
//            log.info("test {} aa {}", "log1", 2);
//            log.info("test {} bb {}", "log1", 2);
//            log.info("test {} cc {}", "log1", 2);
//            log.debug("cccc");
//            log.warn("warn hello");
//
//            log2.trace("test trace");
//            log2.trace("log2 {} dfdfdf", 3, 5);
//            log2.debug("cccc");
//
//            log3.debug("log3", "dfd");
//            log3.info("ccccddd");
//
//            log4.error("log4");
//            log4.warn("ccc");
//
//            log5.warn("log5 {} {}", "warn");
//            log5.error("log5 {}", "error");
//            log5.trace("ccsc");
//
//            logConsole.info("test {} console", "hello");
//            logConsole.debug("ccc");
//            logConsole.warn("dsfsf {} cccc", 33);
//
//            defaultLog.debug("default log debug");
//            defaultLog.info("default log info");
//            defaultLog.warn("default log warn");
//            defaultLog.error("default log error");
//
//            illegalLog.trace("test log trace");
//            illegalLog.debug("test log debug");
//            illegalLog.info("test log info");
//            illegalLog.warn("test log warn");
//            illegalLog.error("test log error");
//
//            try {
//                test3();
//            } catch (Throwable t) {
//                log4.error("test exception", t);
//            }
////            Thread.sleep(3000L);
//        } finally {
//            LogFactory.getInstance().close();
//        }
//    }

    public static void test3() {
        test2();
    }

    @Test
    void testLogFactory() {
        Log log = LogFactory.getInstance().getLog(Bar.class);
        assertTrue(log instanceof ClassNameLogWrap);
        assertFalse(log.isDebugEnabled());
        assertTrue(log.isInfoEnabled());
    }

    @Test
    void test() {
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

        mdc.put("reqId", "hello_req_id");
        testRequestId.info("oooooooooo");
        testRequestId.info("bbbbbbbbbb");

        try {
            Thread.sleep(2000L);
        } catch (InterruptedException e1) {
            e1.printStackTrace();
        }
        try {
            FileUtils.read(getFile(logFoo), (text, num) -> {
                String[] data = StringUtils.split(text, '\t');
                assertEquals("testFoo", data[1]);
            }, "utf-8");

            FileUtils.read(getFile(logBar), (text, num) -> {
                String[] data = StringUtils.split(text, '\t');
                assertEquals("testBar", data[1]);
            }, "utf-8");

            // test trace
            FileUtils.read(getFile(log2), (text, num) -> {
                String[] data = StringUtils.split(text, '\t');
                switch (num) {
                    case 1:
                        assertEquals("trace log", data[1]);
                        break;
                    case 2:
                        assertEquals("debug log", data[1]);
                        break;
                    case 3:
                        assertEquals("info log", data[1]);
                        break;
                    case 4:
                        assertEquals("warn log", data[1]);
                        break;
                    case 5:
                        assertEquals("error log", data[1]);
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
                        assertEquals("debug log", data[1]);
                        break;
                    case 2:
                        assertEquals("info log", data[1]);
                        break;
                    case 3:
                        assertEquals("warn log", data[1]);
                        break;
                    case 4:
                        assertEquals("error log", data[1]);
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
                        assertEquals("info log", data[1]);
                        break;
                    case 2:
                        assertEquals("warn log", data[1]);
                        break;
                    case 3:
                        assertEquals("error log", data[1]);
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
                        assertEquals("warn log", data[1]);
                        break;
                    case 2:
                        assertEquals("error log", data[1]);
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
                        assertEquals("error log", data[1]);
                        break;
                    default:
                        break;
                }

            }, "UTF-8");


            FileUtils.read(getFile(testRequestId), (text, num) -> {
                String[] data = StringUtils.split(text, '\t');
                assertTrue(data[0].contains("hello_req_id"));
            }, "UTF-8");
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

}
