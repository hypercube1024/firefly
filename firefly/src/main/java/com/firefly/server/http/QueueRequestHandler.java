package com.firefly.server.http;

import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import com.firefly.mvc.web.servlet.HttpServletDispatcherController;
import com.firefly.net.Session;
import com.firefly.utils.log.Log;
import com.firefly.utils.log.LogFactory;

public class QueueRequestHandler extends RequestHandler {

	private static Log log = LogFactory.getInstance().getLog("firefly-system");
	private ExecutorService executor = Executors.newCachedThreadPool();

	public QueueRequestHandler(HttpServletDispatcherController servletController) {
		super(servletController);
	}

	@Override
	public void shutdown() {
		executor.shutdown();
	}

	@Override
	public void doRequest(Session session, final HttpServletRequestImpl request)
			throws IOException {
		if (request.response.system) { // 系统错误响应
			request.response.outSystemData();
		} else {
			if(request.isSupportPipeline()) {
				doRequest(request);
			} else { // 非等幂请求使用cache线程池
				executor.submit(new Runnable(){
					@Override
					public void run() {
						try {
							doRequest(request);
						} catch (IOException e) {
							log.error("http handle thread error", e);
						}
					}
				});
			}
		}
	}

}
