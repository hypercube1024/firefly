package com.firefly.mvc.web.servlet;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.firefly.mvc.web.AnnotationWebContext;
import com.firefly.mvc.web.DispatcherController;
import com.firefly.mvc.web.View;
import com.firefly.mvc.web.WebContext;
import com.firefly.mvc.web.WebHandler;
import com.firefly.utils.log.Log;
import com.firefly.utils.log.LogFactory;

/**
 * 前端控制器
 * 
 * @author alvinqiu
 * 
 */
public class HttpServletDispatcherController implements DispatcherController {

	private static Log log = LogFactory.getInstance().getLog("firefly-system");

	protected WebContext webContext;
	
	public HttpServletDispatcherController(String initParam, ServletContext servletContext) {
		webContext = new AnnotationWebContext(initParam, servletContext);
	}
	
	public HttpServletDispatcherController(WebContext webContext) {
		this.webContext = webContext;
	}

	@Override
	public boolean dispatcher(HttpServletRequest request, HttpServletResponse response) {
		String encoding = webContext.getEncoding();
		try {
			request.setCharacterEncoding(encoding);
		} catch (Throwable t) {
			log.error("dispatcher error", t);
		}
		response.setCharacterEncoding(encoding);

		StringBuilder uriBuilder = new StringBuilder(request.getRequestURI());
		uriBuilder.delete(0, request.getContextPath().length() + request.getServletPath().length());
		if(uriBuilder.length() <= 0) {
			controllerNotFoundResponse(request, response);
			return true;
		}
		
		//TODO 获取controller 
		String invokeUri = uriBuilder.toString();
		WebHandler handler = webContext.match(invokeUri);
		if(handler == null) {
			controllerNotFoundResponse(request, response);
			return true;
		}	
		
		View v = handler.invoke(request, response);
		if(v != null) {
			try {
				v.render(request, response);
			} catch (Throwable t) {
				log.error("dispatcher error", t);
			}
		}
		
		return false;
	}
	
	protected void controllerNotFoundResponse(HttpServletRequest request, HttpServletResponse response) {
		String msg = request.getRequestURI() + " not register";
		SystemHtmlPage.responseSystemPage(request, response, getEncoding(), HttpServletResponse.SC_NOT_FOUND, msg);
	}

	

	protected String getEncoding() {
		return webContext.getEncoding();
	}
}
