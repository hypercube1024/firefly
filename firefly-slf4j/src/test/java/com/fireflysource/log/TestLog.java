package com.fireflysource.log;

import com.fireflysource.log.foo.Foo;
import com.fireflysource.log.foo.bar.Bar;
import com.fireflysource.log.internal.utils.StringUtils;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;


class TestLog {

    private static final Log logTrace = LogFactory.getInstance().getLog("test-TRACE");
    private static final Log logDebug = LogFactory.getInstance().getLog("test-DEBUG");
    private static final Log logInfo = LogFactory.getInstance().getLog("test-INFO");
    private static final Log logWarn = LogFactory.getInstance().getLog("test-WARN");
    private static final Log logError = LogFactory.getInstance().getLog("test-ERROR");
    private static final Log testMdc = LogFactory.getInstance().getLog("test-request-id");
    private static final Log logFoo = LogFactory.getInstance().getLog(Foo.class);
    private static final Log logBar = LogFactory.getInstance().getLog(Bar.class);
    private static final Log logDefaultName = LogFactory.getInstance().getLog("test-illegal");
    private static final Log logConsole = LogFactory.getInstance().getLog("test-console");
    private static final Log logCommon = LogFactory.getInstance().getLog("firefly-common");
    private static final Log logErrorStack = LogFactory.getInstance().getLog("error-stack");

    private static MappedDiagnosticContext mdc;

    @BeforeAll
    static void init() {
        mdc = MappedDiagnosticContextFactory.getInstance().getMappedDiagnosticContext();
        deleteAll();
    }

    @AfterAll
    static void destroy() {
        deleteAll();
    }

    private static void deleteAll() {
        deleteLog(logTrace);
        deleteLog(logDebug);
        deleteLog(logError);
        deleteLog(logWarn);
        deleteLog(logInfo);
        deleteLog(logFoo);
        deleteLog(logBar);
        deleteLog(testMdc);
        deleteLog(logDefaultName);
        deleteLog(logConsole);
        deleteLog(logCommon);
        deleteLog(logErrorStack);
    }

    private static void deleteLog(Log log) {
        File file = getFile(log);
        if (file != null && file.exists()) {
            boolean success = file.delete();
            System.out.println("delete file " + file.getAbsolutePath() + " | " + success);
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

    private static List<String> readAllLines(Log log) throws IOException {
        final File file = getFile(log);
        if (file == null) return Collections.emptyList();

        return Files.readAllLines(file.toPath(), StandardCharsets.UTF_8);
    }

    private static void sleep() {
        try {
            Thread.sleep(1000L);
        } catch (InterruptedException e1) {
            e1.printStackTrace();
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

    @Test
    @DisplayName("should create log by class name successfully")
    void testLogFactory() {
        Log log = LogFactory.getInstance().getLog(Bar.class);
        assertTrue(log instanceof ClassNameLogWrap);
        assertFalse(log.isDebugEnabled());
        assertTrue(log.isInfoEnabled());
    }

    @Test
    @DisplayName("should get log by class name successfully")
    void testClassNameLog() throws IOException {
        logFoo.info("testFoo");
        logBar.info("testBar");
        sleep();

        readAllLines(logFoo).forEach(text -> {
            String[] data = StringUtils.split(text, '\t');
            assertEquals("testFoo", data[1]);
        });

        readAllLines(logBar).forEach(text -> {
            String[] data = StringUtils.split(text, '\t');
            assertEquals("testBar", data[1]);
        });
    }

    @Test
    @DisplayName("should print log the level is greater than and equals trace successfully")
    void testTrace() throws IOException {
        logTrace.trace("trace log");
        logTrace.debug("debug log");
        logTrace.info("info log");
        logTrace.warn("warn log");
        logTrace.error("error log");
        sleep();

        List<String> lines = readAllLines(logTrace);
        assertTrue(lines.get(0).contains("trace log"));
        assertTrue(lines.get(1).contains("debug log"));
        assertTrue(lines.get(2).contains("info log"));
        assertTrue(lines.get(3).contains("warn log"));
        assertTrue(lines.get(4).contains("error log"));
    }

    @Test
    @DisplayName("should print log the level is greater than and equals debug successfully")
    void testDebug() throws IOException {
        logDebug.trace("trace log");
        logDebug.debug("debug log");
        logDebug.info("info log");
        logDebug.warn("warn log");
        logDebug.error("error log");
        sleep();

        List<String> lines = readAllLines(logDebug);
        assertTrue(lines.get(0).contains("debug log"));
        assertTrue(lines.get(1).contains("info log"));
        assertTrue(lines.get(2).contains("warn log"));
        assertTrue(lines.get(3).contains("error log"));
    }

    @Test
    @DisplayName("should print log the level is greater than and equals info successfully")
    void testInfo() throws IOException {
        logInfo.trace("trace log");
        logInfo.debug("debug log");
        logInfo.info("info log");
        logInfo.warn("warn log");
        logInfo.error("error log");
        sleep();

        List<String> lines = readAllLines(logInfo);
        assertTrue(lines.get(0).contains("info log"));
        assertTrue(lines.get(1).contains("warn log"));
        assertTrue(lines.get(2).contains("error log"));
    }

    @Test
    @DisplayName("should print log the level is greater than and equals warn successfully")
    void testWarn() throws IOException {
        logWarn.trace("trace log");
        logWarn.debug("debug log");
        logWarn.info("info log");
        logWarn.warn("warn log");
        logWarn.error("error log");
        sleep();

        List<String> lines = readAllLines(logWarn);
        assertTrue(lines.get(0).contains("warn log"));
        assertTrue(lines.get(1).contains("error log"));
    }

    @Test
    @DisplayName("should print error log successfully")
    void testError() throws IOException {
        logError.trace("trace log");
        logError.debug("debug log");
        logError.info("info log");
        logError.warn("warn log");
        logError.error("error log");
        sleep();

        List<String> lines = readAllLines(logError);
        assertTrue(lines.get(0).contains("error log"));
    }

    @Test
    @DisplayName("should put MDC successfully")
    void testMdc() throws IOException {
        mdc.put("reqId", "hello_req_id");
        testMdc.info("oooooooooo");
        testMdc.info("bbbbbbbbbb");
        sleep();

        List<String> lines = readAllLines(testMdc);
        assertTrue(lines.get(0).contains("hello_req_id"));
        assertTrue(lines.get(1).contains("hello_req_id"));
    }

    @Test
    @DisplayName("should create default log successfully when the log name not exists")
    void testDefaultLogName() throws IOException {
        logDefaultName.info("default name 0");
        sleep();

        List<String> lines = readAllLines(logDefaultName);
        assertTrue(lines.get(0).contains("default name 0"));
    }

    @Test
    @DisplayName("should print log to console and file successfully")
    void testConsoleAndFile() throws IOException {
        logConsole.info("test console.");
        logCommon.info("test common.");
        sleep();

        List<String> lines = readAllLines(logConsole);
        assertTrue(lines.get(0).contains("test console."));

        lines = readAllLines(logCommon);
        assertTrue(lines.get(0).contains("test common."));
    }

    @Test
    void testErrorStack() throws IOException {
        try {
            test3();
        } catch (Exception e) {
            logErrorStack.error("exception", e);
        }
        sleep();

        List<String> lines = readAllLines(logErrorStack);
        assertTrue(lines.get(0).contains("exception"));
        assertTrue(lines.size() > 1);
        assertTrue(lines.get(1).contains("$err_start"));
        assertTrue(lines.get(2).contains("java.lang.RuntimeException"));
    }

}
