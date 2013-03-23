package com.firefly.server.http;

import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

import javax.servlet.http.HttpServletResponse;

import com.firefly.mvc.web.servlet.HttpServletDispatcherController;
import com.firefly.mvc.web.servlet.SystemHtmlPage;
import com.firefly.net.Session;
import com.firefly.utils.log.Log;
import com.firefly.utils.log.LogFactory;

public class CachedThreadPoolRequestHandler extends RequestHandler {

	private static Log log = LogFactory.getInstance().getLog("firefly-system");
	private ExecutorService executor = Executors.newCachedThreadPool(new ThreadFactory(){

		@Override
		public Thread newThread(Runnable r) {
			return new Thread(r, "firefly http handler thread");
		}
	});

	public CachedThreadPoolRequestHandler(HttpServletDispatcherController servletController) {
		super(servletController);
	}

	@Override
	public void shutdown() {
		executor.shutdown();
	}

	@Override
	public void doRequest(final Session session, final HttpServletRequestImpl request) throws IOException {
		if (request.response.system) { // 系统错误响应
			request.response.outSystemData();
		} else {
			if(request.isSupportPipeline()) {
				doRequest(request);
			} else {
				executor.submit(new Runnable(){
					@Override
					public void run() {
						try {
							doRequest(request);
						} catch (Throwable e) {
							log.error("http handle thread error", e);
							if(!request.response.isCommitted()) {
								String msg = "Server internal error";
								SystemHtmlPage.responseSystemPage(request, request.response, request.config.getEncoding(), HttpServletResponse.SC_INTERNAL_SERVER_ERROR, msg);
							} else {
								session.close(true);
							}
						}
					}
				});
			}
		}
	}

}
