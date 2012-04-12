package com.firefly.utils.json.parser;

import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Deque;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

import com.firefly.utils.json.Parser;
import com.firefly.utils.json.exception.JsonException;
import com.firefly.utils.json.support.JsonStringReader;
import com.firefly.utils.json.support.ParserMetaInfo;

public class CollectionParser implements Parser {
	
	private ParserMetaInfo elementMetaInfo;
	
	public CollectionParser(Type elementType) {
		if(elementType instanceof ParameterizedType) { // 集合元素是参数类型
    		ParameterizedType pt = (ParameterizedType)elementType;
    		Class<?> rawClass = (Class<?>) (pt.getRawType());
    		
    		Type[] types2 = pt.getActualTypeArguments();
    		if(types2.length != 1)
        		throw new JsonException("collection type more than 1");
    		
    		Type eleType = types2[0];
    		elementMetaInfo = new ParserMetaInfo();
        	
        	if(Collection.class.isAssignableFrom(rawClass)) {
        		elementMetaInfo.setType(getImplClass(rawClass));
        		elementMetaInfo.setParser(new CollectionParser(eleType));
        	} else {
        		elementMetaInfo.setType(rawClass);
        		elementMetaInfo.setParser(ParserStateMachine.getParser(rawClass));
        	}
    	} else if (elementType instanceof Class) {
        	Class<?> eleClass = (Class<?>) elementType; // 获取集合元素Parser
        	elementMetaInfo = new ParserMetaInfo();
        	elementMetaInfo.setType(eleClass);
        	elementMetaInfo.setParser(ParserStateMachine.getParser(eleClass));
    	}
	}
	
	public static Class<?> getImplClass(Class<?> clazz) {
		if(clazz.isInterface() || Modifier.isAbstract(clazz.getModifiers())) {
			Class<?> ret = ArrayList.class;
			if(List.class.isAssignableFrom(clazz))
				ret = ArrayList.class;
			else if(Queue.class.isAssignableFrom(clazz) || Deque.class.isAssignableFrom(clazz))
				ret = LinkedList.class;
			else if(SortedSet.class.isAssignableFrom(clazz))
				ret = TreeSet.class;
			else if(Set.class.isAssignableFrom(clazz))
				ret = HashSet.class;
			return ret;
		} else 
			return clazz;
	}

	public ParserMetaInfo getElementMetaInfo() {
		return elementMetaInfo;
	}

	@Override
	@SuppressWarnings({ "rawtypes", "unchecked" })
	public Object convertTo(JsonStringReader reader, Class<?> clazz) {
		reader.mark();
		if(reader.isNull())
			return null;
		else
			reader.reset();
		
		if(!reader.isArray())
			throw new JsonException("json string is not array format");
		
		Collection obj = null;
		try {
			obj = (Collection)clazz.newInstance();
		} catch (Throwable e) {
			e.printStackTrace();
		}
		
		// 判断空数组
		reader.mark();
		char c0 = reader.readAndSkipBlank();
		if(c0 == ']')
			return obj;
		else
			reader.reset();
		
		for(;;) {
			obj.add(elementMetaInfo.getValue(reader));
			
			char ch = reader.readAndSkipBlank();
			if(ch == ']')
				return obj;

			if(ch != ',')
				throw new JsonException("missing ','");
		}
	}

}
