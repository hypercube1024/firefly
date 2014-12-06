package test.utils.log;

import com.firefly.utils.log.Log;
import com.firefly.utils.log.LogFactory;

public class LogDemo {

	private static final Log log = LogFactory.getInstance().getLog(
			"firefly-common");
	private static final Log log2 = LogFactory.getInstance().getLog(
			"test-TRACE");
	private static final Log log3 = LogFactory.getInstance().getLog(
			"test-DEBUG");
	private static final Log log4 = LogFactory.getInstance().getLog(
			"test-ERROR");
	private static final Log log5 = LogFactory.getInstance()
			.getLog("test-WARN");
	private static final Log logConsole = LogFactory.getInstance().getLog(
			"test-console");
	private static final Log defaultLog = LogFactory.getInstance().getLog(
			"firefly-system");

	public static void main(String[] args) throws Throwable {
		while(true) {
			log.info("test {} aa {}", "log1", 2);
			Thread.sleep(1000);
		}
	}
	
	public static void main2(String[] args) {
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

			try {
				test3();
			} catch (Throwable t) {
				log4.error("test exception", t);
			}
		} finally {
			LogFactory.getInstance().shutdown();
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
