package com.firefly.server.http;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.firefly.mvc.web.WebContext;
import com.firefly.mvc.web.servlet.HttpServletDispatcherController;

public class ServerHttpServletDispatcherController extends HttpServletDispatcherController {

	public ServerHttpServletDispatcherController(WebContext webContext) {
		super(webContext);
	}
	
	@Override
	protected void controllerNotFoundResponse(HttpServletRequest request, HttpServletResponse response) {
		
	}

}
