package com.firefly.mvc.web.support;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import com.firefly.annotation.HttpParam;
import com.firefly.utils.ReflectUtils;
import com.firefly.utils.log.Log;
import com.firefly.utils.log.LogFactory;

/**
 * 保存请求key对应的对象
 * 
 * @author alvinqiu
 * 
 */
public class MvcMetaInfo implements Comparable<MvcMetaInfo> {
	
	private static Log log = LogFactory.getInstance().getLog("firefly-system");

	private final Object object; // controller的实例对象
	private final Method method; // 请求uri对应的方法
	private final ParamMetaInfo[] paramMetaInfos; // @HttpParam标注的类的元信息
	private final byte[] methodParam; // 请求方法参数类型
	private final ViewHandle viewHandle; // 返回的视图
	private Integer interceptOrder; // 拦截链顺序

	public MvcMetaInfo(Object object, Method method, ViewHandle viewHandle) {
		super();
		this.object = object;
		this.method = method;
		this.viewHandle = viewHandle;

		Class<?>[] paraTypes = method.getParameterTypes();
		methodParam = new byte[paraTypes.length];
		// 构造参数对象
		paramMetaInfos = new ParamMetaInfo[paraTypes.length];
		Annotation[][] annotations = method.getParameterAnnotations();
		for (int i = 0; i < paraTypes.length; i++) {
			HttpParam httpParam = getHttpParam(annotations[i]);
			if (httpParam != null) {
				ParamMetaInfo paramMetaInfo = new ParamMetaInfo(paraTypes[i],
						ReflectUtils.getSetterMethods(paraTypes[i]),
						httpParam.value());
				paramMetaInfos[i] = paramMetaInfo;
				methodParam[i] = MethodParam.HTTP_PARAM;
			} else {
				if (paraTypes[i].equals(HttpServletRequest.class))
					methodParam[i] = MethodParam.REQUEST;
				else if (paraTypes[i].equals(HttpServletResponse.class))
					methodParam[i] = MethodParam.RESPONSE;
				else if (paraTypes[i].equals(String.class))
					methodParam[i] = MethodParam.CONTROLLER_RETURN;
			}
		}
	}

	private HttpParam getHttpParam(Annotation[] annotations) {
		for (Annotation a : annotations) {
			if (a.annotationType().equals(HttpParam.class))
				return (HttpParam) a;
		}
		return null;
	}

	public ParamMetaInfo[] getParamMetaInfos() {
		return paramMetaInfos;
	}

	public Integer getInterceptOrder() {
		return interceptOrder;
	}

	public void setInterceptOrder(Integer interceptOrder) {
		this.interceptOrder = interceptOrder;
	}

	public ViewHandle getViewHandle() {
		return viewHandle;
	}

	public Object getObject() {
		return object;
	}

	public Method getMethod() {
		return method;
	}

	public Object invoke(Object[] args) {
		Object ret = null;
		try {
			ret = method.invoke(object, args);
		} catch (Throwable t) {
			log.error("controller invoke error", t);
		}
		return ret;
	}

	public byte[] getMethodParam() {
		return methodParam;
	}

	@Override
	public int compareTo(MvcMetaInfo o) {
		if (method.getName().equals("before"))
			return interceptOrder.compareTo(o.getInterceptOrder());
		else
			return o.getInterceptOrder().compareTo(interceptOrder);
	}
}
