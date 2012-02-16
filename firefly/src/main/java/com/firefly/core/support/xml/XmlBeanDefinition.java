package com.firefly.core.support.xml;

import java.util.Map;

import com.firefly.core.support.BeanDefinition;

/**
 * Xml方式Bean定义
 * @author 杰然不同
 * @date 2011-3-5
 */
public interface XmlBeanDefinition extends BeanDefinition {

	/**
	 * 取得属性集合
	 * @return
	 */
	public abstract Map<String, Object> getProperties();
	
	/**
	 * 设置属性集合
	 * @param properties
	 */
	public abstract void setProperties(Map<String, Object> properties);
}
