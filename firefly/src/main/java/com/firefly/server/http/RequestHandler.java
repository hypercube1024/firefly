package com.firefly.server.http;

import java.io.IOException;
import java.util.LinkedList;
import java.util.List;

import com.firefly.mvc.web.DispatcherController;
import com.firefly.mvc.web.servlet.HttpServletDispatcherController;
import com.firefly.net.Session;
import com.firefly.utils.log.Log;
import com.firefly.utils.log.LogFactory;

public abstract class RequestHandler {
	
	private static Log access = LogFactory.getInstance().getLog("firefly-access");
	private List<DispatcherController> controllers = new LinkedList<DispatcherController>();
	
	public RequestHandler(HttpServletDispatcherController servletController, FileDispatcherController fileController) {
		controllers.add(servletController);
		controllers.add(fileController);
	}

	protected void doRequest(HttpServletRequestImpl request, int id) throws IOException {
		long start = com.firefly.net.Config.TIME_PROVIDER.currentTimeMillis();
	
		if (request.response.system) {
			request.response.outSystemData();
		} else {
			for(DispatcherController controller : controllers) {
				if(!controller.dispatcher(request, request.response))
					break;
			}
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
	
	abstract public void doRequest(Session session, HttpServletRequestImpl request) throws IOException;
	abstract public void shutdown();
}
