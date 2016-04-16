package org.firefly.slf4j;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Slf4jImplDemo {
	
	private static final Logger logger = LoggerFactory.getLogger("firefly-common");

	public static void main(String[] args) {
		logger.info("test slf4j log");
	}

}
