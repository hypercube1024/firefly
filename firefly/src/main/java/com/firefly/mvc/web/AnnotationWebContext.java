package com.firefly.mvc.web;

import java.io.File;
import java.lang.reflect.Method;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.firefly.annotation.RequestMapping;
import com.firefly.core.XmlApplicationContext;
import com.firefly.core.support.BeanDefinition;
import com.firefly.core.support.annotation.ConfigReader;
import com.firefly.core.support.xml.XmlBeanReader;
import com.firefly.mvc.web.support.ControllerMetaInfo;
import com.firefly.mvc.web.support.ControllerBeanDefinition;
import com.firefly.mvc.web.support.InterceptorBeanDefinition;
import com.firefly.mvc.web.support.InterceptorMetaInfo;
import com.firefly.mvc.web.support.MethodParam;
import com.firefly.mvc.web.support.WebBeanReader;
import com.firefly.mvc.web.view.JsonView;
import com.firefly.mvc.web.view.JspView;
import com.firefly.mvc.web.view.TemplateView;
import com.firefly.mvc.web.view.TextView;
import com.firefly.utils.log.Log;
import com.firefly.utils.log.LogFactory;

/**
 * Web应用上下文默认实现
 * 
 * @author AlvinQiu
 * 
 */
public class AnnotationWebContext extends XmlApplicationContext implements WebContext {
	
	private static Log log = LogFactory.getInstance().getLog("firefly-system");
	private final Resource resource;
	private final List<InterceptorMetaInfo> interceptorList = new LinkedList<InterceptorMetaInfo>();
	private boolean thirdpartyWebServer;

	public AnnotationWebContext(String file, ServletContext servletContext) {
		super(file);
		resource = new Resource(getEncoding());
		if (servletContext != null)
			TemplateView.init(servletContext.getRealPath(getViewPath()), getEncoding());
		
		initContext();
		thirdpartyWebServer = true;
	}

	/**
	 * 用于firefly http服务器
	 * 
	 * @param file
	 *            firefly配置文件
	 * @param serverHome
	 *            http服务根目录
	 */
	public AnnotationWebContext(String file, String serverHome) {
		super(file);
		resource = new Resource(getEncoding());
		TemplateView.init(new File(serverHome, getViewPath()).getAbsolutePath(), getEncoding());
		initContext();
		thirdpartyWebServer = false;
	}

	private void initContext() {
		TextView.setEncoding(getEncoding());
		JsonView.setEncoding(getEncoding());
		JspView.setViewPath(getViewPath());

		for (BeanDefinition beanDef : beanDefinitions) {
			if (beanDef instanceof ControllerBeanDefinition) {
				ControllerBeanDefinition beanDefinition = (ControllerBeanDefinition) beanDef;
				List<Method> list = beanDefinition.getReqMethods();
				if (list != null) {
					for (Method m : list) {
						m.setAccessible(true);
						final String uri = m.getAnnotation(RequestMapping.class).value();
						ControllerMetaInfo c = new ControllerMetaInfo(beanDefinition.getObject(), m);
						resource.add(uri, c);
					}
				}
			} else if (beanDef instanceof InterceptorBeanDefinition) {
				InterceptorBeanDefinition beanDefinition = (InterceptorBeanDefinition) beanDef;
				if(beanDefinition.getDisposeMethod() != null) {
					beanDefinition.getDisposeMethod().setAccessible(true);
					InterceptorMetaInfo interceptor = new InterceptorMetaInfo(beanDefinition.getObject(), 
							beanDefinition.getDisposeMethod(),
							beanDefinition.getUriPattern(), 
							beanDefinition.getOrder());
					interceptorList.add(interceptor);
				}
			}
		}
		if(interceptorList.size() > 0)
			Collections.sort(interceptorList);
	}

	@Override
	protected List<BeanDefinition> getBeanDefinitions(String file) {
		List<BeanDefinition> list1 = new WebBeanReader(file)
				.loadBeanDefinitions();
		List<BeanDefinition> list2 = new XmlBeanReader(file)
				.loadBeanDefinitions();
		if (list1 != null && list2 != null) {
			list1.addAll(list2);
			return list1;
		} else if (list1 != null)
			return list1;
		else if (list2 != null)
			return list2;
		return null;
	}

	@Override
	public String getEncoding() {
		return ConfigReader.getInstance().getConfig().getEncoding();
	}

	@Override
	public String getViewPath() {
		return ConfigReader.getInstance().getConfig().getViewPath();
	}
	
	@Override
	public HandlerChain match(String uri) {
		final HandlerChainImpl chain = new HandlerChainImpl();
		
		for(final InterceptorMetaInfo interceptor : interceptorList) {
			if(interceptor.getPattern().match(uri) != null) {
				chain.add(new WebHandler(){

					@Override
					public View invoke(HttpServletRequest request, HttpServletResponse response) {
						return interceptor.invoke(getParams(request, response));
					}
					
					private Object[] getParams(HttpServletRequest request, HttpServletResponse response) {
						byte[] methodParam = interceptor.getMethodParam();
						Object[] p = new Object[methodParam.length];

						for (int i = 0; i < p.length; i++) {
							switch (methodParam[i]) {
							case MethodParam.REQUEST:
								p[i] = request;
								break;
							case MethodParam.RESPONSE:
								p[i] = response;
								break;
							case MethodParam.HANDLER_CHAIN:
								p[i] = chain;
								break;
							}
						}
						return p;
					}
				});
			}
				
		}
		
		WebHandler last = resource.match(uri);
		if(last != null) {
			chain.add(last);
		} else {
			if(!thirdpartyWebServer) {
			// TODO 使用firefly httpserver时还要增加文件响应
			}
		}
		
		chain.init();
		return chain;
	}
	
	private class HandlerChainImpl implements HandlerChain {
		private List<WebHandler> list = new LinkedList<WebHandler>();
		private Iterator<WebHandler> iterator;
		
		private void add(WebHandler webHandler) {
			list.add(webHandler);
		}
		
		private void init() {
			if(iterator == null)
				iterator = list.iterator();
		}

		@Override
		public View doNext(HttpServletRequest request, HttpServletResponse response, HandlerChain chain) {
			if(iterator.hasNext())
				return iterator.next().invoke(request, response);
			else
				return null;
		}
	}

}
