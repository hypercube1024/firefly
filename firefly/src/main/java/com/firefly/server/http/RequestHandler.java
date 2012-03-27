package com.firefly.server.http;

import java.io.IOException;

import com.firefly.mvc.web.servlet.HttpServletDispatcherController;
import com.firefly.net.Session;
import com.firefly.utils.log.Log;
import com.firefly.utils.log.LogFactory;

public abstract class RequestHandler {
	
	private static Log access = LogFactory.getInstance().getLog("firefly-access");
	private String appPrefix;
	private HttpServletDispatcherController servletController;
	private FileDispatcherController fileController;
	
	public RequestHandler(String appPrefix,
			HttpServletDispatcherController servletController,
			FileDispatcherController fileController) {
		this.appPrefix = appPrefix;
		this.servletController = servletController;
		this.fileController = fileController;
	}

	protected void doRequest(HttpServletRequestImpl request, int id) throws IOException {
		long start = com.firefly.net.Config.TIME_PROVIDER.currentTimeMillis();
	
		if (request.response.system) {
			request.response.outSystemData();
		} else {
			if (isServlet(request.getRequestURI()))
				servletController.dispatcher(request, request.response);
			else
				fileController.dispatcher(request, request.response);
		}
		request.releaseInputStreamData();
		
		long end = com.firefly.net.Config.TIME_PROVIDER.currentTimeMillis();
		access.info("{}|{}|{}|{}|{}|{}|{}|{}|{}|{}|{}", 
				request.session.getSessionId(), 
				id, 
				request.getRemoteAddr(),
				request.response.getStatus(),
				request.getProtocol(),
				request.getMethod(),
				request.getRequestURI(),
				request.getQueryString(),
				request.session.getReadBytes(),
				request.session.getWrittenBytes(),
				(end - start));
	}
	
	private boolean isServlet(String URI) {
		if (URI.length() < 2)
			return false;

		int j = URI.length();
		for (int i = 1; i < URI.length(); i++) {
			if (URI.charAt(i) == '/') {
				j = i;
				break;
			}
		}

		if (j == URI.length())
			return appPrefix.equals(URI);
		else
			return appPrefix.equals(URI.substring(0, j));
	}
	
	abstract public void doRequest(Session session, HttpServletRequestImpl request) throws IOException;
	abstract public void shutdown();
}
