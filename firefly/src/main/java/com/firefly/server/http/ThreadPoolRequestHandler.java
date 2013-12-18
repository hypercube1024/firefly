package com.firefly.server.http;

import java.io.IOException;

import com.firefly.mvc.web.servlet.HttpServletDispatcherController;
import com.firefly.net.Session;
import com.firefly.server.http.ThreadPoolWrapper.BusinessLogicTask;

public class ThreadPoolRequestHandler extends RequestHandler {

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
						doRequest(request);
					}
				});
			}
		}
	}

}
