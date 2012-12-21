package com.firefly.utils.json.parser;

import java.util.Collection;
import java.util.Date;
import java.util.Map;

import com.firefly.utils.collection.IdentityHashMap;
import com.firefly.utils.json.JsonReader;
import com.firefly.utils.json.Parser;
import com.firefly.utils.json.exception.JsonException;

public class ParserStateMachine {
	
	private static final IdentityHashMap<Class<?>, Parser> PARSER_MAP = new IdentityHashMap<Class<?>, Parser>();
	private static final Object LOCK = new Object();
	
	static {
		PARSER_MAP.put(int.class, new IntParser());
		PARSER_MAP.put(long.class, new LongParser());
		PARSER_MAP.put(short.class, new ShortParser());
		PARSER_MAP.put(float.class, new FloatParser());
		PARSER_MAP.put(double.class, new DoubleParser());
		PARSER_MAP.put(boolean.class, new BooleanParser());
		PARSER_MAP.put(char.class, new CharacterParser());
		
		PARSER_MAP.put(Integer.class, PARSER_MAP.get(int.class));
		PARSER_MAP.put(Long.class, PARSER_MAP.get(long.class));
		PARSER_MAP.put(Short.class, PARSER_MAP.get(short.class));
		PARSER_MAP.put(Float.class, PARSER_MAP.get(float.class));
		PARSER_MAP.put(Double.class, PARSER_MAP.get(double.class));
		PARSER_MAP.put(Boolean.class, PARSER_MAP.get(boolean.class));
		PARSER_MAP.put(Character.class, PARSER_MAP.get(char.class));
		
		PARSER_MAP.put(String.class, new StringParser());
		PARSER_MAP.put(Date.class, new DateParser());
		
		PARSER_MAP.put(int[].class, new ArrayParser(int.class));
		PARSER_MAP.put(long[].class, new ArrayParser(long.class));
		PARSER_MAP.put(short[].class, new ArrayParser(short.class));
		PARSER_MAP.put(float[].class, new ArrayParser(float.class));
		PARSER_MAP.put(double[].class, new ArrayParser(double.class));
		PARSER_MAP.put(boolean[].class, new ArrayParser(boolean.class));
		PARSER_MAP.put(byte[].class, new ByteArrayParser());
		PARSER_MAP.put(char[].class, new CharArrayParser());
		
		PARSER_MAP.put(Integer[].class, new ArrayParser(Integer.class));
		PARSER_MAP.put(Long[].class, new ArrayParser(Long.class));
		PARSER_MAP.put(Short[].class, new ArrayParser(Short.class));
		PARSER_MAP.put(Float[].class, new ArrayParser(Float.class));
		PARSER_MAP.put(Double[].class, new ArrayParser(Double.class));
		PARSER_MAP.put(Boolean[].class, new ArrayParser(Boolean.class));
		
		PARSER_MAP.put(String[].class, new ArrayParser(String.class));
	}
	
	public static Parser getParser(Class<?> clazz) {
		Parser ret = PARSER_MAP.get(clazz);
		if(ret == null) {
			synchronized(LOCK) {
				ret = PARSER_MAP.get(clazz);
				if(ret == null) {
					if (clazz.isEnum()) {
						ret = new EnumParser(clazz);
						PARSER_MAP.put(clazz, ret);
					} else if (Collection.class.isAssignableFrom(clazz) 
							|| Map.class.isAssignableFrom(clazz)) {
						throw new JsonException("not support type " + clazz);
					} else if (clazz.isArray()) {
						Class<?> elementClass = clazz.getComponentType();
						ret = new ArrayParser(elementClass);
						PARSER_MAP.put(clazz, ret);
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
	public static <T> T toObject(JsonReader reader, Class<?> clazz) {
		Parser parser = getParser(clazz);
		return (T) parser.convertTo(reader, clazz);
	}
}
