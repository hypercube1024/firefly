package com.firefly.server.http;

import java.io.IOException;

import javax.servlet.http.HttpServletResponse;

import com.firefly.mvc.web.servlet.HttpServletDispatcherController;
import com.firefly.mvc.web.servlet.SystemHtmlPage;
import com.firefly.net.Session;
import com.firefly.server.http.ThreadPoolWrapper.BusinessLogicTask;
import com.firefly.utils.log.Log;
import com.firefly.utils.log.LogFactory;

public class ThreadPoolRequestHandler extends RequestHandler {

	private static Log log = LogFactory.getInstance().getLog("firefly-system");

	public ThreadPoolRequestHandler(HttpServletDispatcherController servletController) {
		super(servletController);
	}

	@Override
	public void shutdown() {
		ThreadPoolWrapper.shutdown();
	}

	@Override
	public void doRequest(final Session session, final HttpServletRequestImpl request) throws IOException {
		if (request.response.system) { // response HTTP decode error
			request.response.outSystemData();
		} else {
			if(request.isSupportPipeline()) {
				doRequest(request);
			} else {
				ThreadPoolWrapper.submit(new BusinessLogicTask(request){
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
