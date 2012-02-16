package com.firefly.core.support.annotation;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.List;

public class AnnotatedBeanDefinition implements AnnotationBeanDefinition {

	private String id, className;
	private String[] names;
	private List<Field> fields;
	private List<Method> methods;
	private Object object;

	@Override
	public Object getObject() {
		return object;
	}

	@Override
	public void setObject(Object object) {
		this.object = object;
	}

	@Override
	public List<Field> getInjectFields() {
		return fields;
	}

	@Override
	public List<Method> getInjectMethods() {
		return methods;
	}

	@Override
	public String[] getInterfaceNames() {
		return names;
	}

	@Override
	public void setInjectFields(List<Field> fields) {
		this.fields = fields;
	}

	@Override
	public void setInjectMethods(List<Method> methods) {
		this.methods = methods;
	}

	@Override
	public void setInterfaceNames(String[] names) {
		this.names = names;

	}

	@Override
	public String getClassName() {
		return className;
	}

	@Override
	public String getId() {
		return id;
	}

	@Override
	public void setClassName(String className) {
		this.className = className;
	}

	@Override
	public void setId(String id) {
		this.id = id;
	}

}
