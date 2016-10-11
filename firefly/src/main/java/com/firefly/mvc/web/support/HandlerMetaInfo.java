package com.firefly.mvc.web.support;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Enumeration;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.firefly.annotation.HttpParam;
import com.firefly.annotation.PathVariable;
import com.firefly.mvc.web.HandlerChain;
import com.firefly.mvc.web.View;
import com.firefly.mvc.web.support.exception.WebException;
import com.firefly.utils.ReflectUtils;
import com.firefly.utils.VerifyUtils;
import com.firefly.utils.ReflectUtils.MethodProxy;
import com.firefly.utils.log.Log;
import com.firefly.utils.log.LogFactory;

public abstract class HandlerMetaInfo {

	private static Log log = LogFactory.getInstance().getLog("firefly-system");

	// controller or intercepter instance
	protected final Object object;
	// mapped method of the URI
	protected final MethodProxy proxy;
	// method type
	protected final MethodParam[] methodParam;
	// HTTP parameter meta information
	protected final ParamMetaInfo[] paramMetaInfos;

	public HandlerMetaInfo(Object object, Method method) {
		this.object = object;
		try {
			this.proxy = ReflectUtils.getMethodProxy(method);
		} catch (Throwable e) {
			log.error("handler init error", e);
			throw new WebException("handler invoke error");
		}
		Class<?>[] paraTypes = method.getParameterTypes();
		this.methodParam = new MethodParam[paraTypes.length];
		this.paramMetaInfos = new ParamMetaInfo[paraTypes.length];

		for (int i = 0; i < paraTypes.length; i++) {
			if (paraTypes[i].equals(HttpServletRequest.class)) {
				methodParam[i] = MethodParam.REQUEST;
			} else if (paraTypes[i].equals(HttpServletResponse.class)) {
				methodParam[i] = MethodParam.RESPONSE;
			} else if (paraTypes[i].equals(HandlerChain.class)) {
				methodParam[i] = MethodParam.HANDLER_CHAIN;
			} else {
				Annotation[][] annotations = method.getParameterAnnotations();
				Annotation anno = getAnnotation(annotations[i]);
				if (anno != null) {
					if (anno.annotationType().equals(HttpParam.class)) {
						HttpParam httpParam = (HttpParam) anno;
						ParamMetaInfo paramMetaInfo = new ParamMetaInfo(paraTypes[i],
								ReflectUtils.getSetterMethods(paraTypes[i]), httpParam.value());
						paramMetaInfos[i] = paramMetaInfo;
						methodParam[i] = MethodParam.HTTP_PARAM;
					} else if (anno.annotationType().equals(PathVariable.class)) {
						if (paraTypes[i].equals(String[].class))
							methodParam[i] = MethodParam.PATH_VARIBLE;
					}
				} else {
					ParamMetaInfo paramMetaInfo = new ParamMetaInfo(paraTypes[i],
							ReflectUtils.getSetterMethods(paraTypes[i]), "");
					paramMetaInfos[i] = paramMetaInfo;
					methodParam[i] = MethodParam.HTTP_PARAM;
				}
			}
		}
	}

	private Annotation getAnnotation(Annotation[] annotations) {
		for (Annotation a : annotations) {
			if (a.annotationType().equals(HttpParam.class) || a.annotationType().equals(PathVariable.class))
				return a;
		}
		return null;
	}

	public MethodParam[] getMethodParam() {
		return methodParam;
	}

	public final View invoke(Object[] args) {
		return (View) proxy.invoke(object, args);
	}

	public ParamMetaInfo[] getParamMetaInfos() {
		return paramMetaInfos;
	}

	public Object[] getParameters(HttpServletRequest request, HttpServletResponse response, HandlerChain chain,
			String[] pathParameters) {
		Object[] p = new Object[methodParam.length];

		for (int i = 0; i < p.length; i++) {
			switch (methodParam[i]) {
			case REQUEST:
				p[i] = request;
				break;
			case RESPONSE:
				p[i] = response;
				break;
			case HANDLER_CHAIN:
				p[i] = chain;
				break;
			case HTTP_PARAM:
				Enumeration<String> enumeration = request.getParameterNames();
				ParamMetaInfo paramMetaInfo = paramMetaInfos[i];
				p[i] = paramMetaInfo.newParamInstance();

				// convert HTTP parameters to objects
				while (enumeration.hasMoreElements()) {
					String httpParamName = enumeration.nextElement();
					String paramValue = request.getParameter(httpParamName);
					paramMetaInfo.setParam(p[i], httpParamName, paramValue);
				}
				if (VerifyUtils.isNotEmpty(paramMetaInfo.getAttribute())) {
					request.setAttribute(paramMetaInfo.getAttribute(), p[i]);
				}
				break;
			case PATH_VARIBLE:
				p[i] = pathParameters;
				break;
			default:
				break;
			}
		}
		return p;
	}

	@Override
	public String toString() {
		return "HandlerMetaInfo [method=" + proxy.method() + ", methodParam=" + Arrays.toString(methodParam) + "]";
	}

}
