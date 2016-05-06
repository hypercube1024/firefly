package com.firefly.core.support.xml;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * XML bean configuration 
 *
 * @author JJ Xu &amp; Alvin Qiu
 */
public class XmlGenericBeanDefinition implements XmlBeanDefinition {

	private String id;
	private String className;
	private Map<String, XmlManagedNode> properties = new HashMap<String, XmlManagedNode>();
	private String[] interfaceNames;
	private List<XmlManagedNode> contructorParameters = new ArrayList<XmlManagedNode>();
	private Constructor<?> constructor;
	private Method initMethod;

	@Override
	public String getId() {
		return id;
	}

	@Override
	public void setId(String id) {
		this.id = id;
	}

	@Override
	public String getClassName() {
		return className;
	}

	@Override
	public void setClassName(String className) {
		this.className = className;
	}

	@Override
	public String[] getInterfaceNames() {
		return interfaceNames;
	}

	@Override
	public void setInterfaceNames(String[] interfaceNames) {
		this.interfaceNames = interfaceNames;
	}

	@Override
	public Map<String, XmlManagedNode> getProperties() {
		return properties;
	}

	@Override
	public void setProperties(Map<String, XmlManagedNode> properties) {
		this.properties = properties;
	}

	@Override
	public List<XmlManagedNode> getContructorParameters() {
		return contructorParameters;
	}

	@Override
	public void setContructorParameters(List<XmlManagedNode> contructorParameters) {
		this.contructorParameters = contructorParameters;
	}

	@Override
	public Constructor<?> getConstructor() {
		return constructor;
	}

	@Override
	public void setConstructor(Constructor<?> constructor) {
		this.constructor = constructor;
	}

	@Override
	public Method getInitMethod() {
		return initMethod;
	}

	@Override
	public void setInitMethod(Method initMethod) {
		this.initMethod = initMethod;
	}
	
}
