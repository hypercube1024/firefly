package com.firefly.core.support.xml;

import java.util.HashMap;
import java.util.Map;

/**
 * Xml方式Bean实现
 *
 * @author 杰然不同
 * @date 2011-3-5
 */
public class XmlGenericBeanDefinition implements XmlBeanDefinition {

	// id
	private String id;

	// className
	private String className;

	// 属性集合
	private Map<String, Object> properties = new HashMap<String, Object>();
	private String[] names;
	private Object object;

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getClassName() {
		return className;
	}

	public void setClassName(String className) {
		this.className = className;
	}

	public Map<String, Object> getProperties() {
		return properties;
	}

	public void setProperties(Map<String, Object> properties) {
		this.properties = properties;
	}

	@Override
	public String[] getInterfaceNames() {
		return names;
	}

	@Override
	public Object getObject() {
		return object;
	}

	@Override
	public void setInterfaceNames(String[] names) {
		this.names = names;
	}

	@Override
	public void setObject(Object object) {
		this.object = object;
	}
}
