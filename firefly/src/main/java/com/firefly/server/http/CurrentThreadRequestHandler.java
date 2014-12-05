package com.firefly.server.http;

import java.io.IOException;

import com.firefly.mvc.web.servlet.HttpServletDispatcherController;
import com.firefly.net.Session;

public class CurrentThreadRequestHandler extends RequestHandler {

	public CurrentThreadRequestHandler(HttpServletDispatcherController servletController) {
		super(servletController);
	}

	@Override
	public void doRequest(Session session, HttpServletRequestImpl request) throws IOException {
		if (request.response.system) { // response HTTP decode error
			request.response.outSystemData();
		} else {
			doRequest(request);
		}
		
	}

	@Override
	public void shutdown() {
		
	}

}
