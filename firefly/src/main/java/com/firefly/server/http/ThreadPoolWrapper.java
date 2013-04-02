package com.firefly.server.http;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

import com.firefly.utils.log.Log;
import com.firefly.utils.log.LogFactory;

public class ThreadPoolWrapper {
	
	private static Log log = LogFactory.getInstance().getLog("firefly-system");
	private static ExecutorService executor;
	
	public static void init(int maxThreadNumber) {
		ThreadFactory threadFactory = new ThreadFactory(){

			@Override
			public Thread newThread(Runnable r) {
				return new Thread(r, "firefly http handler thread");
			}
		};
		
		if(maxThreadNumber <= 0) {
			log.info("max business thread num [cached]");
			executor = Executors.newCachedThreadPool(threadFactory);
		} else {
			log.info("max business thread num [{}]", maxThreadNumber);
			executor = Executors.newFixedThreadPool(maxThreadNumber, threadFactory);
		}
	}
	
	public static ExecutorService getExecutorService() {
		return executor;
	}
}
