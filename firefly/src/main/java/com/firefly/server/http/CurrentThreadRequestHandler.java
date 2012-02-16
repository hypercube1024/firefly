package com.firefly.server.http;

import java.io.IOException;

import com.firefly.mvc.web.servlet.HttpServletDispatcherController;
import com.firefly.net.Session;

public class CurrentThreadRequestHandler extends RequestHandler {

	public CurrentThreadRequestHandler(String appPrefix,
			HttpServletDispatcherController servletController,
			FileDispatcherController fileController) {
		super(appPrefix, servletController, fileController);
	}

	@Override
	public void doRequest(Session session, HttpServletRequestImpl request)
			throws IOException {
		doRequest(request, 0);
	}

	@Override
	public void shutdown() {

	}

}
