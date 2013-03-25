package com.firefly.server.http;

import java.io.IOException;

import com.firefly.mvc.web.DispatcherController;
import com.firefly.mvc.web.servlet.HttpServletDispatcherController;
import com.firefly.net.Session;
import com.firefly.utils.VerifyUtils;
import com.firefly.utils.log.Log;
import com.firefly.utils.log.LogFactory;
import com.firefly.utils.time.Millisecond100Clock;

public abstract class RequestHandler {
	
	private static Log access = LogFactory.getInstance().getLog("firefly-access");
	private DispatcherController servletController;
	
	public RequestHandler(HttpServletDispatcherController servletController) {
		this.servletController = servletController;
	}

	protected void doRequest(HttpServletRequestImpl request) throws IOException {
		long start = Millisecond100Clock.currentTimeMillis();
		try {
			servletController.dispatcher(request, request.response);
		} finally {
			request.releaseInputStreamData();
		}
		long end = Millisecond100Clock.currentTimeMillis();
		access.info("{}|{}|{}|{}|{}|{}|{}|{}", 
				request.session.getSessionId(), 
				getClientAddress(request),
				request.response.getStatus(),
				request.getProtocol(),
				request.getMethod(),
				request.getRequestURI(),
				request.getQueryString(),
				(end - start));
	}
	
	private final String getClientAddress(HttpServletRequestImpl request) {
		String address = request.getHeader("X-Forwarded-For");
		if(VerifyUtils.isNotEmpty(address))
			return address;

		return request.getRemoteAddr();
	}
	
	abstract public void doRequest(Session session, HttpServletRequestImpl request) throws IOException;
	abstract public void shutdown();
}
