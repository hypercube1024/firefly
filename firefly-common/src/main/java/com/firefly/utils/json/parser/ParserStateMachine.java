package com.firefly.utils.json.parser;

import java.util.Collection;
import java.util.Map;

import com.firefly.utils.collection.IdentityHashMap;
import com.firefly.utils.json.Parser;
import com.firefly.utils.json.exception.JsonException;
import com.firefly.utils.json.support.JsonStringReader;

public class ParserStateMachine {
	
	private static final IdentityHashMap<Class<?>, Parser> PARSER_MAP = new IdentityHashMap<Class<?>, Parser>();
	
	static {
		PARSER_MAP.put(int.class, new IntParser());
		PARSER_MAP.put(long.class, new LongParser());
		PARSER_MAP.put(short.class, new ShortParser());
		PARSER_MAP.put(float.class, new FloatParser());
		PARSER_MAP.put(double.class, new DoubleParser());
		PARSER_MAP.put(boolean.class, new BooleanParser());
		
		PARSER_MAP.put(Integer.class, PARSER_MAP.get(int.class));
		PARSER_MAP.put(Long.class, PARSER_MAP.get(long.class));
		PARSER_MAP.put(Short.class, PARSER_MAP.get(short.class));
		PARSER_MAP.put(Float.class, PARSER_MAP.get(float.class));
		PARSER_MAP.put(Double.class, PARSER_MAP.get(double.class));
		PARSER_MAP.put(Boolean.class, PARSER_MAP.get(boolean.class));
		
		PARSER_MAP.put(String.class, new StringParser());
	}
	
	public static Parser getParser(Class<?> clazz) {
		Parser ret = PARSER_MAP.get(clazz);
		if(ret == null) {
			synchronized(ParserStateMachine.class) {
				ret = PARSER_MAP.get(clazz);
				if(ret == null) {
					if (clazz.isEnum()) {
						ret = new EnumParser(clazz);
						PARSER_MAP.put(clazz, ret);
					} else if (Collection.class.isAssignableFrom(clazz) 
							|| Map.class.isAssignableFrom(clazz)
							|| clazz.isArray()) {
						throw new JsonException("not support type " + clazz);
					} else {
						ret = new ObjectParser();
						PARSER_MAP.put(clazz, ret);
						((ObjectParser)ret).init(clazz);
					}
				}
			}
		}
		return ret;
	}
	
	@SuppressWarnings("unchecked")
	public static <T> T toObject(JsonStringReader reader, Class<?> clazz) {
		Parser parser = getParser(clazz);
		return (T) parser.convertTo(reader, clazz);
	}
}
