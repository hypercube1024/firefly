package com.firefly.mvc.web.servlet;

import java.util.Enumeration;
import java.util.Set;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.firefly.mvc.web.AnnotationWebContext;
import com.firefly.mvc.web.DispatcherController;
import com.firefly.mvc.web.WebContext;
import com.firefly.mvc.web.support.MethodParam;
import com.firefly.mvc.web.support.MvcMetaInfo;
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

		String uri = request.getRequestURI();
		String prePath = request.getContextPath() + request.getServletPath();
		String invokeUri = uri.substring(prePath.length());
		String key = request.getMethod() + "@" + invokeUri;
		log.debug("uri map [{}]", key);
		
		// 获取controller
		MvcMetaInfo mvcMetaInfo = webContext.getBean(key);
		if (mvcMetaInfo == null) {
			controllerNotFoundResponse(request, response);
			return true;
		}
		
		// 获取拦截器
		String beforeIntercept = "b#" + invokeUri;
		String afterIntercept = "a#" + invokeUri;
		Set<MvcMetaInfo> beforeSet = webContext.getBean(beforeIntercept);
		Set<MvcMetaInfo> afterSet = webContext.getBean(afterIntercept);
		
		Object ret = null;
		Object beforeRet = null; // 前置拦截器的返回值
		MvcMetaInfo lastBefore = null; // 最后得到的前置拦截器
		Object afterRet = null; // 后置拦截器的返回值
		MvcMetaInfo lastAfter = null; // 最后得到的后置拦截器

		// 前置拦截栈调用
		if (beforeSet != null) {
			for (MvcMetaInfo before : beforeSet) {
				Object[] beforeP = getParams(request, response, before,
						null);
				beforeRet = before.invoke(beforeP);
				if (beforeRet != null) {
					lastBefore = before;
					break;
				}
			}
		}

		if (beforeRet == null) {
			// controller调用
			Object[] p = getParams(request, response, mvcMetaInfo, null);
			ret = mvcMetaInfo.invoke(p);

			// 后置拦截栈调用
			if (afterSet != null) {
				for (MvcMetaInfo after : afterSet) {
					Object[] afterP = getParams(request, response, after,
							ret);
					afterRet = after.invoke(afterP);
					if (afterRet != null) {
						lastAfter = after;
						break;
					}
				}
			}
		}

		// 视图渲染
		try {
			if (afterRet != null) {
				lastAfter.getViewHandle().render(request, response,
						afterRet);
			} else if (beforeRet != null) {
				lastBefore.getViewHandle().render(request, response,
						beforeRet);
			} else {
				mvcMetaInfo.getViewHandle().render(request, response, ret);
			}
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
	private Object[] getParams(HttpServletRequest request,
			HttpServletResponse response, MvcMetaInfo mvcMetaInfo,
			Object controllerReturn) {
		byte[] methodParam = mvcMetaInfo.getMethodParam();
		ParamMetaInfo[] paramMetaInfos = mvcMetaInfo.getParamMetaInfos();
		Object[] p = new Object[methodParam.length];
		boolean firstString = true;
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
			case MethodParam.CONTROLLER_RETURN:
				p[i] = firstString ? (String) controllerReturn : null;
				firstString = false;
				break;
			}
		}
		return p;
	}

	protected String getEncoding() {
		return webContext.getEncoding();
	}
}
