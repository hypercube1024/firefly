package com.firefly.server.http;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.FutureTask;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.RunnableFuture;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import javax.servlet.http.HttpServletResponse;

import com.firefly.mvc.web.servlet.SystemHtmlPage;
import com.firefly.utils.log.Log;
import com.firefly.utils.log.LogFactory;
import com.firefly.utils.time.Millisecond100Clock;

public class ThreadPoolWrapper {
	
	private static Log log = LogFactory.getInstance().getLog("firefly-system");
	private static ExecutorService executor;
	
	public static void init(final Config config) {
		ThreadFactory threadFactory = new ThreadFactory(){

			@Override
			public Thread newThread(Runnable r) {
				return new Thread(r, "firefly business logic thread");
			}
		};
		
		RejectedExecutionHandler handler = new RejectedExecutionHandler(){

			@SuppressWarnings("unchecked")
			@Override
			public void rejectedExecution(Runnable r, ThreadPoolExecutor executor) {
				
				BusinessLogicTask businessLogicTask = ((BusinessLogicFutureTask<Void>)r).getCurrentRunnable();
				HttpServletRequestImpl request = (HttpServletRequestImpl)businessLogicTask.getAttachment();
				HttpServletResponseImpl response = request.response;
				
				log.error("The queue of business pool has been full|{}", request.getRequestURI());
				if(!response.isCommitted()) {
					response.setHeader("Retry-After", "60");
					response.setHeader("Connection", "close");
					String msg = "Server is temporarily overloaded, the queue of business pool is full";
					SystemHtmlPage.responseSystemPage(request, response, config.getEncoding(), HttpServletResponse.SC_SERVICE_UNAVAILABLE, msg);
				}
			}
		};
		
		log.info("corePoolSize [{}], maximumPoolSize [{}], poolQueueSize [{}]", config.getCorePoolSize(), config.getMaximumPoolSize(), config.getPoolQueueSize());
		log.info("poolKeepAliveTime [{}], poolWaitTimeout [{}]", config.getPoolKeepAliveTime(), config.getPoolWaitTimeout());
		
		executor = new ThreadPoolExecutor(config.getCorePoolSize(), 
				config.getMaximumPoolSize(), 
				config.getPoolKeepAliveTime(), 
				TimeUnit.MILLISECONDS, 
				new ArrayBlockingQueue<Runnable>(config.getPoolQueueSize()), 
				threadFactory, handler){
			
			@Override
			protected <T> RunnableFuture<T> newTaskFor(Runnable runnable, T value) {
				return new BusinessLogicFutureTask<T>((BusinessLogicTask)runnable, value);
			}
			
			@SuppressWarnings("unchecked")
			@Override
			protected void beforeExecute(Thread t, Runnable r) {
				BusinessLogicTask businessLogicTask = ((BusinessLogicFutureTask<Void>)r).getCurrentRunnable();
				HttpServletRequestImpl request = (HttpServletRequestImpl)businessLogicTask.getAttachment();
				HttpServletResponseImpl response = request.response;
				
				if(businessLogicTask.getTimeDifference() > config.getPoolWaitTimeout()) {
					try {
						log.error("Waiting for business process has been timeout |{}|{}", request.getRequestURI(), businessLogicTask.getTimeDifference());
						if(!response.isCommitted()) {
							response.setHeader("Retry-After", "60");
							response.setHeader("Connection", "close");
							String msg = "Server is temporarily overloaded, waiting for business process is timeout";
							SystemHtmlPage.responseSystemPage(request, response, config.getEncoding(), HttpServletResponse.SC_SERVICE_UNAVAILABLE, msg);
						} else {
							request.session.close(true);
						}
					} finally {
						((BusinessLogicFutureTask<Void>)r).cancel(false);
					}
				}
			}
			
			@SuppressWarnings("unchecked")
			@Override
			protected void afterExecute(Runnable r, Throwable t) {
				BusinessLogicTask businessLogicTask = ((BusinessLogicFutureTask<Void>)r).getCurrentRunnable();
				HttpServletRequestImpl request = (HttpServletRequestImpl)businessLogicTask.getAttachment();
				HttpServletResponseImpl response = request.response;
				
				if(t != null) {
					log.error("Business process error", t);
					if(!response.isCommitted()) {
						String msg = "Server internal error";
						SystemHtmlPage.responseSystemPage(request, response, config.getEncoding(), HttpServletResponse.SC_INTERNAL_SERVER_ERROR, msg);
					} else {
						request.session.close(true);
					}
				}
			}
		};
	}
	
	abstract public static class BusinessLogicTask implements Runnable {
		public final long createdTime = Millisecond100Clock.currentTimeMillis();
		private Object attachment;
		
		public BusinessLogicTask(Object attachment) {
			this.attachment = attachment;
		}
		
		public Object getAttachment() {
			return attachment;
		}

		public void setAttachment(Object attachment) {
			this.attachment = attachment;
		}

		public long getCreatedTime() {
			return createdTime;
		}
		
		public long getTimeDifference() {
			return Millisecond100Clock.currentTimeMillis() - createdTime;
		}
	}
	
	public static class BusinessLogicFutureTask<T> extends FutureTask<T> {
		
		private BusinessLogicTask currentRunnable;

		public BusinessLogicFutureTask(BusinessLogicTask runnable, T result) {
			super(runnable, result);
			currentRunnable = runnable;
		}

		public BusinessLogicTask getCurrentRunnable() {
			return currentRunnable;
		}
		
	}
	
	public static Future<?> submit(BusinessLogicTask task) {
		return executor.submit(task);
	}
	
	public static void shutdown() {
		executor.shutdown();
	}
}
