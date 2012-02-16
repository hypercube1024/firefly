package com.firefly.server.http;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import com.firefly.mvc.web.servlet.HttpServletDispatcherController;
import com.firefly.net.Session;
import com.firefly.utils.collection.LinkedTransferQueue;
import com.firefly.utils.log.Log;
import com.firefly.utils.log.LogFactory;

public class QueueRequestHandler extends RequestHandler {

	private static Log log = LogFactory.getInstance().getLog("firefly-system");
	private HttpQueueHandler[] queues;

	public QueueRequestHandler(String appPrefix,
			HttpServletDispatcherController servletController,
			FileDispatcherController fileController, int size) {
		super(appPrefix, servletController, fileController);
		queues = new HttpQueueHandler[size];
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
		}

		public void shutdown() {
			start = false;
		}

	}

	@Override
	public void shutdown() {
		for (HttpQueueHandler h : queues)
			h.shutdown();
	}

	@Override
	public void doRequest(Session session, HttpServletRequestImpl request)
			throws IOException {
		int sessionId = session.getSessionId();
		int handlerIndex = Math.abs(sessionId) % queues.length;
		queues[handlerIndex].add(request);
	}

}
