package com.firefly.mvc.web;

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
import com.firefly.mvc.web.servlet.SystemHtmlPage;
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
//import com.firefly.utils.log.Log;
//import com.firefly.utils.log.LogFactory;

/**
 * Web应用上下文默认实现
 * 
 * @author AlvinQiu
 * 
 */
public class AnnotationWebContext extends XmlApplicationContext implements WebContext {
	
	protected final Resource resource;
	protected final List<InterceptorMetaInfo> interceptorList = new LinkedList<InterceptorMetaInfo>();

	public AnnotationWebContext(String file) {
		super(file);
		resource = new Resource(getEncoding());
		initContext();
	}
	
	public AnnotationWebContext(String file, ServletContext servletContext) {
		this(file);
		
		if (servletContext != null)
			TemplateView.init(servletContext.getRealPath(getViewPath()), getEncoding());
	}

	private void initContext() {
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
		
		TextView.setEncoding(getEncoding());
		JsonView.setEncoding(getEncoding());
		JspView.setViewPath(getViewPath());
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
	public HandlerChain match(String uri, String servletURI) {
		final HandlerChainImpl chain = new HandlerChainImpl();
		addInterceptor(uri, servletURI, chain);
		addLastHandler(uri, servletURI, chain);
		chain.init();
		return chain;
	}
	
	protected void addLastHandler(String uri, String servletURI, final HandlerChainImpl chain) {
		if(servletURI == null)
			return;
		
		WebHandler last = resource.match(servletURI);
		if(last != null)
			chain.add(last);
	}
	
	protected void addInterceptor(String uri, String servletURI, final HandlerChainImpl chain) {
		if(servletURI == null)
			return;
		
		for(final InterceptorMetaInfo interceptor : interceptorList) {
			if(interceptor.getPattern().match(servletURI) != null) {
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
	}
	
	protected class HandlerChainImpl implements HandlerChain {
		private List<WebHandler> list = new LinkedList<WebHandler>();
		private Iterator<WebHandler> iterator;
		
		public void add(WebHandler webHandler) {
			list.add(webHandler);
		}
		
		private void init() {
			if(list.size() == 0) { // 没有找到处理器输出404响应
				list.add(new WebHandler(){

					@Override
					public View invoke(HttpServletRequest request, HttpServletResponse response) {
						String msg = request.getRequestURI() + " not found";
						SystemHtmlPage.responseSystemPage(request, response, getEncoding(), HttpServletResponse.SC_NOT_FOUND, msg);
						return null;
					}
				});
			}
			
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
