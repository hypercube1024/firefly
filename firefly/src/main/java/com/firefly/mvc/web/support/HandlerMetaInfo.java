package com.firefly.mvc.web.support;

import com.firefly.annotation.HttpParam;
import com.firefly.annotation.JsonBody;
import com.firefly.annotation.PathVariable;
import com.firefly.mvc.web.HandlerChain;
import com.firefly.mvc.web.View;
import com.firefly.mvc.web.support.exception.WebException;
import com.firefly.server.http2.servlet.HttpStringBodyRequest;
import com.firefly.utils.ReflectUtils;
import com.firefly.utils.ReflectUtils.MethodProxy;
import com.firefly.utils.VerifyUtils;
import com.firefly.utils.io.IO;
import com.firefly.utils.json.Json;
import com.firefly.utils.json.JsonArray;
import com.firefly.utils.json.JsonObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.InputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Set;

public abstract class HandlerMetaInfo {

	private static Logger log = LoggerFactory.getLogger("firefly-system");
	private static final Set<Class<?>> ANNOTATION_TYPES = new HashSet<>(
			Arrays.asList(HttpParam.class, PathVariable.class, JsonBody.class));

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
					} else if (anno.annotationType().equals(JsonBody.class)) {
						ParamMetaInfo paramMetaInfo = new ParamMetaInfo(paraTypes[i],
								ReflectUtils.getSetterMethods(paraTypes[i]), "");
						paramMetaInfos[i] = paramMetaInfo;
						methodParam[i] = MethodParam.JSON_BODY;
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
			if (ANNOTATION_TYPES.contains(a.annotationType()))
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
			case HTTP_PARAM: {
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
			}
				break;
			case PATH_VARIBLE:
				p[i] = pathParameters;
				break;
			case JSON_BODY: {
				if (request instanceof HttpStringBodyRequest) {
					HttpStringBodyRequest r = (HttpStringBodyRequest) request;
					ParamMetaInfo paramMetaInfo = paramMetaInfos[i];
					if (paramMetaInfo.getParamClass().equals(JsonObject.class)) {
						p[i] = r.getJsonObjectBody();
					} else if (paramMetaInfo.getParamClass().equals(JsonArray.class)) {
						p[i] = r.getJsonArrayBody();
					} else {
						p[i] = r.getJsonBody(paramMetaInfo.getParamClass());
					}
				} else {
					ParamMetaInfo paramMetaInfo = paramMetaInfos[i];
					try (InputStream in = request.getInputStream()) {
						String stringBody = IO.toString(in, request.getCharacterEncoding());
						if (paramMetaInfo.getParamClass().equals(JsonObject.class)) {
							p[i] = Json.toJsonObject(stringBody);
						} else if (paramMetaInfo.getParamClass().equals(JsonArray.class)) {
							p[i] = Json.toJsonArray(stringBody);
						} else {
							p[i] = Json.toObject(stringBody, paramMetaInfo.getParamClass());
						}
					} catch (IOException e) {
						log.error("get http request string body exception", e);
					}
				}
			}
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
