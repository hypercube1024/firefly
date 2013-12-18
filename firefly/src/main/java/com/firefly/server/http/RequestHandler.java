package com.firefly.server.http;

import java.io.IOException;

import com.firefly.mvc.web.DispatcherController;
import com.firefly.mvc.web.servlet.HttpServletDispatcherController;
import com.firefly.net.Session;

public abstract class RequestHandler {
	
	private DispatcherController servletController;
	
	public RequestHandler(HttpServletDispatcherController servletController) {
		this.servletController = servletController;
	}

	protected void doRequest(HttpServletRequestImpl request) {
		try {
			servletController.dispatch(request, request.response);
		} finally {
			request.releaseInputStreamData();
		}
	}
	
	abstract public void doRequest(Session session, HttpServletRequestImpl request) throws IOException;
	abstract public void shutdown();
}
