package com.firefly.mvc.web;

import java.io.File;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import javax.servlet.ServletContext;

import com.firefly.annotation.RequestMapping;
import com.firefly.core.XmlApplicationContext;
import com.firefly.core.support.BeanDefinition;
import com.firefly.core.support.annotation.ConfigReader;
import com.firefly.core.support.xml.XmlBeanReader;
import com.firefly.mvc.web.support.MvcMetaInfo;
import com.firefly.mvc.web.support.ViewHandle;
import com.firefly.mvc.web.support.WebBeanDefinition;
import com.firefly.mvc.web.support.WebBeanReader;
import com.firefly.mvc.web.support.view.FFTViewHandle;
import com.firefly.mvc.web.support.view.JsonViewHandle;
import com.firefly.mvc.web.support.view.JspViewHandle;
import com.firefly.mvc.web.support.view.RedirectHandle;
import com.firefly.mvc.web.support.view.TextViewHandle;
import com.firefly.utils.StringUtils;
import com.firefly.utils.VerifyUtils;
import com.firefly.utils.log.Log;
import com.firefly.utils.log.LogFactory;

/**
 * Web应用上下文默认实现
 * 
 * @author AlvinQiu
 * 
 */
public class AnnotationWebContext extends XmlApplicationContext implements
		WebContext {
	private static Log log = LogFactory.getInstance().getLog("firefly-system");

	public AnnotationWebContext(String file, ServletContext servletContext) {
		super(file);
		if (servletContext != null)
			FFTViewHandle.getInstance().init(
					servletContext.getRealPath(getViewPath()), getEncoding());
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
		FFTViewHandle.getInstance().init(
				new File(serverHome, getViewPath()).getAbsolutePath(),
				getEncoding());
		initContext();
	}

	private void initContext() {
		JspViewHandle.getInstance().init(getViewPath());
		TextViewHandle.getInstance().init(getEncoding());
		JsonViewHandle.getInstance().init(getEncoding());
		List<String> uriList = new ArrayList<String>();
		for (BeanDefinition beanDef : beanDefinitions) {
			if (beanDef instanceof WebBeanDefinition)
				addObjectToContext(beanDef, uriList);
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

	@SuppressWarnings("unchecked")
	private void addObjectToContext(BeanDefinition beanDef, List<String> uriList) {
		WebBeanDefinition beanDefinition = (WebBeanDefinition) beanDef;
		// 注册Controller里面声明的uri
		List<Method> list = beanDefinition.getReqMethods();
		if (list != null) {
			for (Method m : list) {
				m.setAccessible(true);
				final String uri = m.getAnnotation(RequestMapping.class)
						.value();
				final String method = m.getAnnotation(RequestMapping.class)
						.method();
				String view = m.getAnnotation(RequestMapping.class).view();
				view = VerifyUtils.isNotEmpty(view) ? view : ConfigReader
						.getInstance().getConfig().getViewType();
				String key = method + "@" + uri;

				// 构造请求uri对应的方法的元信息
				MvcMetaInfo mvcMetaInfo = new MvcMetaInfo(
						beanDefinition.getObject(), m, getViewHandle(view));
				map.put(key, mvcMetaInfo);
				uriList.add(key);

				if (key.charAt(key.length() - 1) == '/') {
					key = key.substring(0, key.length() - 1);
					log.info("register uri [{}], view [{}]", key, view);
				} else {
					log.info("register uri [{}], view [{}]", key, view);
					key += "/";
				}
				map.put(key, mvcMetaInfo);
				uriList.add(key);
			}
		}

		list = beanDefinition.getInterceptorMethods();
		if (list != null) {
			log.debug("interceptorMethods size [{}]", list.size());
			for (Method m : list) {
				m.setAccessible(true);
				List<String> l = getInterceptUri(
						beanDefinition.getUriPattern(), uriList);
				log.debug("interceptorUri size [{}]", l.size());
				for (String i : l) {
					String key = m.getName().charAt(0) + "#" + i;

					// 构造拦截器的元信息
					MvcMetaInfo mvcMetaInfo = new MvcMetaInfo(
							beanDefinition.getObject(), m,
							getViewHandle(beanDefinition.getView()));
					mvcMetaInfo.setInterceptOrder(beanDefinition.getOrder());
					Set<MvcMetaInfo> interceptorSet = (Set<MvcMetaInfo>) map
							.get(key);
					if (interceptorSet == null) {
						interceptorSet = new TreeSet<MvcMetaInfo>();
						interceptorSet.add(mvcMetaInfo);
						map.put(key, interceptorSet);
					} else {
						interceptorSet.add(mvcMetaInfo);
					}
				}
			}
		}

	}

	/**
	 * 根据拦截器模式获取所有注册的Uri
	 * 
	 * @param pattern
	 * @return
	 */
	private List<String> getInterceptUri(String pattern, List<String> uriList) {
		List<String> list = new ArrayList<String>();
		log.debug("uriList size [{}]", uriList.size());
		for (String uriAndMethod : uriList) {
			String uri = StringUtils.split(uriAndMethod, "@")[1];
			if (ignoreBackslashEquals(pattern, uri)) {
				log.debug("intercept uri[{}] pattern[{}]", uri, pattern);
				list.add(uri);
			}
		}
		return list;
	}

	/**
	 * 拦截地址匹配，忽略uri和pattern最后的'/'
	 * 
	 * @param pattern
	 * @param uri
	 * @return
	 */
	private boolean ignoreBackslashEquals(String pattern, String uri) {
		if (uri.charAt(uri.length() - 1) == '/')
			uri = uri.substring(0, uri.length() - 1);
		if (pattern.charAt(pattern.length() - 1) == '/')
			pattern = pattern.substring(0, pattern.length() - 1);
		return VerifyUtils.simpleWildcardMatch(pattern, uri);
	}

	private ViewHandle getViewHandle(String view) {
		ViewHandle viewHandle = null;
		if (view.equals(View.FFT)) {
			viewHandle = FFTViewHandle.getInstance();
		} else if (view.equals(View.JSP)) {
			viewHandle = JspViewHandle.getInstance();
		} else if (view.equals(View.TEXT)) {
			viewHandle = TextViewHandle.getInstance();
		} else if (view.equals(View.REDIRECT)) {
			viewHandle = RedirectHandle.getInstance();
		} else if (view.equals(View.JSON)) {
			viewHandle = JsonViewHandle.getInstance();
		}
		return viewHandle;
	}
}
