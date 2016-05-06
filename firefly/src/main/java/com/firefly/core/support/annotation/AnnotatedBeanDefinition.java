package com.firefly.core.support.annotation;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.List;

public class AnnotatedBeanDefinition implements AnnotationBeanDefinition {

	private String id, className;
	private String[] names;
	private List<Field> fields;
	private List<Method> methods;
	private Constructor<?> constructor;
	private Object injectedInstance;
	private Method initMethod;

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

	@Override
	public Constructor<?> getConstructor() {
		return constructor;
	}

	@Override
	public void setConstructor(Constructor<?> constructor) {
		this.constructor = constructor;
	}

	@Override
	public Object getInjectedInstance() {
		return injectedInstance;
	}

	@Override
	public void setInjectedInstance(Object injectedInstance) {
		this.injectedInstance = injectedInstance;
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
