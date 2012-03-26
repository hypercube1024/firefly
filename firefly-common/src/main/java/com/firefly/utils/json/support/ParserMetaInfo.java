package com.firefly.utils.json.support;

import java.lang.reflect.Method;

import com.firefly.utils.json.Parser;

public class ParserMetaInfo {
	private Class<?> clazz;
	private Parser parser;
	private Method method;
	private String propertyName;

	public Class<?> getClazz() {
		return clazz;
	}

	public void setClazz(Class<?> clazz) {
		this.clazz = clazz;
	}

	public Parser getParser() {
		return parser;
	}

	public void setParser(Parser parser) {
		this.parser = parser;
	}

	public Method getMethod() {
		return method;
	}

	public void setMethod(Method method) {
		this.method = method;
	}

	public String getPropertyName() {
		return propertyName;
	}

	public void setPropertyName(String propertyName) {
		this.propertyName = propertyName;
	}

}
