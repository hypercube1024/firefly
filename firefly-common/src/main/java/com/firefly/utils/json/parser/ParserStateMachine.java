package com.firefly.utils.json.parser;

import java.util.Collection;
import java.util.Map;

import com.firefly.utils.collection.IdentityHashMap;
import com.firefly.utils.json.Parser;
import com.firefly.utils.json.support.JsonStringReader;

public class ParserStateMachine {
	
	private static final IdentityHashMap<Class<?>, Parser> PARSER_MAP = new IdentityHashMap<Class<?>, Parser>();
	
	static {
		PARSER_MAP.put(int.class, new IntParser());
		PARSER_MAP.put(Integer.class, PARSER_MAP.get(int.class));
	}
	
	public static Parser getParser(Class<?> clazz) {
		Parser ret = PARSER_MAP.get(clazz);
		if(ret == null) {
			if (clazz.isEnum()) {
				
			} else if (Map.class.isAssignableFrom(clazz)) {
				
			} else if (Collection.class.isAssignableFrom(clazz)) {
				
			} else if (clazz.isArray()) {
				
			} else {
				ret = new ObjectParser(clazz);
			}
			PARSER_MAP.put(clazz, ret);
		}
		return ret;
	}
	
	
	@SuppressWarnings("unchecked")
	public static <T> T toObject(JsonStringReader reader, Class<?> clazz) {
		Parser parser = getParser(clazz);
		return (T) parser.convertTo(reader, clazz);
	}
}
