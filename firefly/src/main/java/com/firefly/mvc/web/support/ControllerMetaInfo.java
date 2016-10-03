package com.firefly.mvc.web.support;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.firefly.annotation.HttpParam;
import com.firefly.annotation.PathVariable;
import com.firefly.annotation.RequestMapping;
import com.firefly.utils.ReflectUtils;

public class ControllerMetaInfo extends HandlerMetaInfo {

	// HTTP parameter meta information
	private final ParamMetaInfo[] paramMetaInfos;
	private final Set<String> allowHttpMethod;

	public ControllerMetaInfo(Object object, Method method) {
		super(object, method);
		allowHttpMethod = new HashSet<String>(Arrays.asList(method.getAnnotation(RequestMapping.class).method()));
		Class<?>[] paraTypes = method.getParameterTypes();

		// construct parameters
		paramMetaInfos = new ParamMetaInfo[paraTypes.length];
		Annotation[][] annotations = method.getParameterAnnotations();
		for (int i = 0; i < paraTypes.length; i++) {
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
				if (paraTypes[i].equals(HttpServletRequest.class))
					methodParam[i] = MethodParam.REQUEST;
				else if (paraTypes[i].equals(HttpServletResponse.class))
					methodParam[i] = MethodParam.RESPONSE;
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

	public ParamMetaInfo[] getParamMetaInfos() {
		return paramMetaInfos;
	}

	public boolean allowMethod(String method) {
		return allowHttpMethod.contains(method);
	}

	public String getAllowMethod() {
		StringBuilder s = new StringBuilder();
		for (String m : allowHttpMethod) {
			s.append(m).append(',');
		}
		s.deleteCharAt(s.length() - 1);
		return s.toString();
	}

	@Override
	public String toString() {
		return "ControllerMetaInfo [allowHttpMethod=" + allowHttpMethod + "]";
	}

}
