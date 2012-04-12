package com.firefly.utils.json.parser;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Collection;

import com.firefly.utils.json.Parser;
import com.firefly.utils.json.exception.JsonException;
import com.firefly.utils.json.support.JsonStringReader;
import com.firefly.utils.json.support.ParserMetaInfo;

public class CollectionParser implements Parser {
	
	private ParserMetaInfo elementMetaInfo;
	
	public CollectionParser(Class<?> clazz, Type elementType) {
		if(elementType instanceof ParameterizedType) { // 集合元素是参数类型
    		ParameterizedType pt = (ParameterizedType)elementType;
    		Class<?> rawClass = (Class<?>) (pt.getRawType());
    		
    		Type[] types2 = pt.getActualTypeArguments();
    		if(types2.length != 1)
        		throw new JsonException("collection type more than 1");
    		
    		Type eleType = types2[0];
    		elementMetaInfo = new ParserMetaInfo();
        	elementMetaInfo.setType(rawClass);
        	
        	if(Collection.class.isAssignableFrom(rawClass))
        		elementMetaInfo.setParser(new CollectionParser(rawClass, eleType));
        	else
        		elementMetaInfo.setParser(ParserStateMachine.getParser(rawClass));
    	} else if (elementType instanceof Class) {
        	Class<?> eleClass = (Class<?>) elementType; // 获取集合元素Parser
        	elementMetaInfo = new ParserMetaInfo();
        	elementMetaInfo.setType(eleClass);
        	elementMetaInfo.setParser(ParserStateMachine.getParser(eleClass));
    	}
	}

	public ParserMetaInfo getElementMetaInfo() {
		return elementMetaInfo;
	}

	@Override
	public Object convertTo(JsonStringReader reader, Class<?> clazz) {
		return null;
	}

}
