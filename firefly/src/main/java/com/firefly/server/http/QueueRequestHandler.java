package com.firefly.server.http;

import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import com.firefly.mvc.web.servlet.HttpServletDispatcherController;
import com.firefly.mvc.web.servlet.SystemHtmlPage;
import com.firefly.net.Session;
import com.firefly.utils.collection.LinkedTransferQueue;
import com.firefly.utils.log.Log;
import com.firefly.utils.log.LogFactory;

public class QueueRequestHandler extends RequestHandler {

	private static Log log = LogFactory.getInstance().getLog("firefly-system");
	private ExecutorService executor = Executors.newCachedThreadPool();
	private HttpQueueHandler[] queues;
	private Config config;
	private AtomicInteger currentQueueSize = new AtomicInteger();

	public QueueRequestHandler(HttpServletDispatcherController servletController, FileDispatcherController fileController, Config config) {
		super(servletController, fileController);
		this.config = config;
		queues = new HttpQueueHandler[config.getHandlerSize()];
		for (int i = 0; i < queues.length; i++) {
			queues[i] = new HttpQueueHandler(i);
		}
	}

	private class HttpQueueHandler {
		private int id;
		private boolean start = true;
		private LinkedTransferQueue<HttpServletRequestImpl> queue = new LinkedTransferQueue<HttpServletRequestImpl>();
		private Thread thread = new Thread(new Runnable() {

			@Override
			public void run() {
				while (start) {
					try {
						for (HttpServletRequestImpl request = null; (request = queue.poll(
								1000, TimeUnit.MILLISECONDS)) != null;) {
							currentQueueSize.decrementAndGet();
							doRequest(request, id);
						}
					} catch (Throwable e) {
						log.error("http queue error", e);
					}
				}

			}
		}, "http queue " + id);

		public HttpQueueHandler(int id) {
			this.id = id;
			thread.start();
		}

		public void add(HttpServletRequestImpl request) {
			queue.offer(request);
			currentQueueSize.incrementAndGet();
		}

		public void shutdown() {
			start = false;
		}

	}

	@Override
	public void shutdown() {
		for (HttpQueueHandler h : queues)
			h.shutdown();
		
		executor.shutdown();
	}

	@Override
	public void doRequest(Session session, final HttpServletRequestImpl request)
			throws IOException {
		int s = currentQueueSize.get();
		if(s >= config.getMaxHandlerQueueSize()) { // 队列过载保护
			log.warn("http request queue size is {}, more than {}", s, config.getMaxHandlerQueueSize());
			request.response.setHeader("Retry-After", "30");
			SystemHtmlPage.responseSystemPage(request, request.response, config.getEncoding(), 503, "Service unavailable, please try again later.");
		} else {
			
			if(request.isSupportPipeline()) { // pipeline请求
				int sessionId = session.getSessionId();
				int handlerIndex = Math.abs(sessionId) % queues.length;
				queues[handlerIndex].add(request);
			} else {
				executor.submit(new Runnable(){

					@Override
					public void run() {
						try {
							doRequest(request, -1);
						} catch (IOException e) {
							log.error("http handle thread error", e);
						}
					}
				});
			}
		}
	}

}
