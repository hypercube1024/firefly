package com.firefly.mvc.web;

import java.io.File;
import java.lang.reflect.Method;
import java.util.List;

import javax.servlet.ServletContext;

import com.firefly.annotation.RequestMapping;
import com.firefly.core.XmlApplicationContext;
import com.firefly.core.support.BeanDefinition;
import com.firefly.core.support.annotation.ConfigReader;
import com.firefly.core.support.xml.XmlBeanReader;
import com.firefly.mvc.web.support.ControllerMetaInfo;
import com.firefly.mvc.web.support.ControllerBeanDefinition;
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

	public AnnotationWebContext(String file, ServletContext servletContext) {
		super(file);
		resource = new Resource(getEncoding());
		if (servletContext != null)
			TemplateView.init(servletContext.getRealPath(getViewPath()), getEncoding());
		
		initContext();
	}

	/**
	 * 用于http服务器
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
			}
		}
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
	public WebHandler match(String uri) {
		return resource.match(uri);
	}

}
