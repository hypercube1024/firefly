package com.firefly.mvc.web.support;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import com.firefly.annotation.Component;
import com.firefly.annotation.Controller;
import com.firefly.annotation.Interceptor;
import com.firefly.annotation.RequestMapping;
import com.firefly.core.support.BeanDefinition;
import com.firefly.core.support.annotation.AnnotationBeanDefinition;
import com.firefly.core.support.annotation.AnnotationBeanReader;
import com.firefly.utils.ReflectUtils;
import com.firefly.utils.log.Log;
import com.firefly.utils.log.LogFactory;

public class WebBeanReader extends AnnotationBeanReader {

	private static Log log = LogFactory.getInstance().getLog("firefly-system");

	public WebBeanReader() {
		this(null);
	}

	public WebBeanReader(String file) {
		super(file);
	}

	@Override
	protected BeanDefinition getBeanDefinition(Class<?> c) {
		if (c.isAnnotationPresent(Controller.class) || c.isAnnotationPresent(Component.class)) {
			log.info("classes [{}]", c.getName());
			return componentParser(c);
		} 
//		else if (c.isAnnotationPresent(Interceptor.class)) {
//			log.info("classes [{}]", c.getName());
//			return interceptorParser(c);
//		} 
		else
			return null;
	}

	@Override
	protected BeanDefinition componentParser(Class<?> c) {
		ControllerBeanDefinition beanDefinition = new ControllerAnnotatedBeanDefinition();
		setWebBeanDefinition(beanDefinition, c);

		List<Method> reqMethods = getReqMethods(c);
		beanDefinition.setReqMethods(reqMethods);
		return beanDefinition;
	}

	private BeanDefinition interceptorParser(Class<?> c) {
//		ControllerBeanDefinition webBeanDefinition = new ControllerAnnotatedBeanDefinition();
//		setWebBeanDefinition(webBeanDefinition, c);

		// TODO 拦截器
//		List<Method> interceptorMethods = getInterceptors(c);
//		webBeanDefinition.setInterceptorMethods(interceptorMethods);
//
//		String uriPattern = c.getAnnotation(Interceptor.class).uri();
//		webBeanDefinition.setUriPattern(uriPattern);
//
//		String view = c.getAnnotation(Interceptor.class).view();
//		webBeanDefinition.setView(view);
//
//		Integer order = c.getAnnotation(Interceptor.class).order();
//		webBeanDefinition.setOrder(order);

		return null;
	}

	private void setWebBeanDefinition(AnnotationBeanDefinition beanDefinition,
			Class<?> c) {
		beanDefinition.setClassName(c.getName());

		String id = getId(c);
		beanDefinition.setId(id);

		String[] names = ReflectUtils.getInterfaceNames(c);
		beanDefinition.setInterfaceNames(names);

		List<Field> fields = getInjectField(c);
		beanDefinition.setInjectFields(fields);

		List<Method> methods = getInjectMethod(c);
		beanDefinition.setInjectMethods(methods);

		try {
			Object object = c.newInstance();
			beanDefinition.setObject(object);
		} catch (Throwable t) {
			log.error("set web bean error", t);
		}
	}

	private String getId(Class<?> c) {
		if (c.isAnnotationPresent(Controller.class))
			return c.getAnnotation(Controller.class).value();
		else if (c.isAnnotationPresent(Interceptor.class))
			return c.getAnnotation(Interceptor.class).value();
		else if (c.isAnnotationPresent(Component.class))
			return c.getAnnotation(Component.class).value();
		else
			return "";
	}

	private List<Method> getReqMethods(Class<?> c) {
		Method[] methods = c.getMethods();
		List<Method> list = new ArrayList<Method>();
		for (Method m : methods) {
			if (m.isAnnotationPresent(RequestMapping.class)) {
				list.add(m);
			}
		}
		return list;
	}

//	private List<Method> getInterceptors(Class<?> c) {
//		Method[] methods = c.getMethods();
//		List<Method> list = new ArrayList<Method>();
//		for (Method m : methods) {// 验证方法名
//			if (m.getName().equals("before") || m.getName().equals("after")) {
//				list.add(m);
//			}
//		}
//		return list;
//	}
}
