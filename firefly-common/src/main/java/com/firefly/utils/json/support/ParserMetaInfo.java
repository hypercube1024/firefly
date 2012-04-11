package com.firefly.utils.json.support;

import java.lang.reflect.Method;

import com.firefly.utils.json.Parser;

public class ParserMetaInfo implements Comparable<ParserMetaInfo> {

	private Class<?> type;
	private Parser parser;
	private Method method;
	private char[] propertyName;
	private String propertyNameString;
	
	public void invoke(Object obj, JsonStringReader reader) {
		try {
			method.invoke(obj, parser.convertTo(reader, type));
		} catch (Throwable e) {
			e.printStackTrace();
		} 
	}
	
	public Class<?> getType() {
		return type;
	}

	public void setType(Class<?> type) {
		this.type = type;
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

	public String getPropertyNameString() {
		return propertyNameString;
	}

	public void setPropertyNameString(String propertyNameString) {
		this.propertyNameString = propertyNameString;
		propertyName = propertyNameString.toCharArray();
	}

	public char[] getPropertyName() {
		return propertyName;
	}

	@Override
	public int compareTo(ParserMetaInfo o) {
		return propertyNameString.compareTo(o.getPropertyNameString());
	}

}
