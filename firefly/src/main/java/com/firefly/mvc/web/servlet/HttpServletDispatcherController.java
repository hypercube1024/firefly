package com.firefly.mvc.web.servlet;

import java.util.Enumeration;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.firefly.mvc.web.AnnotationWebContext;
import com.firefly.mvc.web.DispatcherController;
import com.firefly.mvc.web.Resource.Result;
import com.firefly.mvc.web.View;
import com.firefly.mvc.web.WebContext;
import com.firefly.mvc.web.support.ControllerMetaInfo;
import com.firefly.mvc.web.support.MethodParam;
import com.firefly.mvc.web.support.ParamMetaInfo;
import com.firefly.utils.VerifyUtils;
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
		Result result = webContext.match(invokeUri);
		if(result == null) {
			controllerNotFoundResponse(request, response);
			return true;
		}
		
		Object[] p = getParams(request, response, result);
		View v = result.resource.getController().invoke(p);
		try {
			v.render(request, response);
		} catch (Throwable t) {
			log.error("dispatcher error", t);
		}
		
		return false;
	}
	
	protected void controllerNotFoundResponse(HttpServletRequest request, HttpServletResponse response) {
		String msg = request.getRequestURI() + " not register";
		SystemHtmlPage.responseSystemPage(request, response, getEncoding(), HttpServletResponse.SC_NOT_FOUND, msg);
	}

	/**
	 * controller方法参数注入
	 * 
	 * @param request
	 * @param response
	 * @param mvcMetaInfo
	 * @return
	 */
	@SuppressWarnings("unchecked")
	private Object[] getParams(HttpServletRequest request, HttpServletResponse response, Result result) {
		ControllerMetaInfo info = result.resource.getController();
		byte[] methodParam = info.getMethodParam();
		ParamMetaInfo[] paramMetaInfos = info.getParamMetaInfos();
		Object[] p = new Object[methodParam.length];

		for (int i = 0; i < p.length; i++) {
			switch (methodParam[i]) {
			case MethodParam.REQUEST:
				p[i] = request;
				break;
			case MethodParam.RESPONSE:
				p[i] = response;
				break;
			case MethodParam.HTTP_PARAM:
				// 请求参数封装到javabean
				Enumeration<String> enumeration = request.getParameterNames();
				ParamMetaInfo paramMetaInfo = paramMetaInfos[i];
				p[i] = paramMetaInfo.newParamInstance();

				// 把http参数赋值给参数对象
				while (enumeration.hasMoreElements()) {
					String httpParamName = enumeration.nextElement();
					String paramValue = request.getParameter(httpParamName);
					paramMetaInfo.setParam(p[i], httpParamName, paramValue);
				}
				if (VerifyUtils.isNotEmpty(paramMetaInfo.getAttribute())) {
					request.setAttribute(paramMetaInfo.getAttribute(), p[i]);
				}
				break;
			case MethodParam.PATH_VARIBLE:
				p[i] = result.params;
				break;
			}
		}
		return p;
	}

	protected String getEncoding() {
		return webContext.getEncoding();
	}
}
