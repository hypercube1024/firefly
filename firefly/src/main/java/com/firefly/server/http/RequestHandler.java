package com.firefly.server.http;

import java.io.IOException;

import com.firefly.mvc.web.DispatcherController;
import com.firefly.mvc.web.servlet.HttpServletDispatcherController;
import com.firefly.net.Session;
import com.firefly.utils.log.Log;
import com.firefly.utils.log.LogFactory;
import com.firefly.utils.time.Millisecond100Clock;

public abstract class RequestHandler {
	
	private static Log access = LogFactory.getInstance().getLog("firefly-access");
	private DispatcherController servletController;
	private DispatcherController fileController;
	
	public RequestHandler(HttpServletDispatcherController servletController, FileDispatcherController fileController) {
		this.servletController = servletController;
		this.fileController = fileController;
	}

	protected void doRequest(HttpServletRequestImpl request, int id) throws IOException {
		long start = Millisecond100Clock.currentTimeMillis();
	
		if (request.response.system) {
			request.response.outSystemData();
		} else {
			if(servletController.dispatcher(request, request.response))
				fileController.dispatcher(request, request.response);
		}
		request.releaseInputStreamData();
		
		long end = Millisecond100Clock.currentTimeMillis();
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
	
	abstract public void doRequest(Session session, HttpServletRequestImpl request) throws IOException;
	abstract public void shutdown();
}
