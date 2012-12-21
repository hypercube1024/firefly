package com.firefly.utils.json.support;

import java.io.IOException;
import java.util.Arrays;

import com.firefly.utils.json.JsonReader;
import com.firefly.utils.json.Parser;

public class ParserMetaInfo extends MetaInfo {

	private Class<?> type;
	private Parser parser;
	
	public void invoke(Object obj, JsonReader reader) {
		try {
			propertyInvoke.set(obj, getValue(reader));
		} catch (Throwable e) {
			e.printStackTrace();
		} 
	}
	
	public void setPropertyNameString(String propertyNameString) {
		this.propertyNameString = propertyNameString;
		propertyName = propertyNameString.toCharArray();
	}
	
	public Object getValue(JsonReader reader) throws IOException {
		return parser.convertTo(reader, type);
	}
	
	public boolean equals(char[] field) {
		return Arrays.equals(propertyName, field);
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
}
