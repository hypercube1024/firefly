package com.firefly.utils.json.compiler;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import com.firefly.utils.json.exception.JsonException;
import com.firefly.utils.json.support.ParserMetaInfo;

public class DecodeCompiler {
	private static final ParserMetaInfo[] EMPTY_ARRAY = new ParserMetaInfo[0];
	
	public static ParserMetaInfo[] compile(Class<?> clazz) {
		ParserMetaInfo[] parserMetaInfos = null;
		List<ParserMetaInfo> list = new ArrayList<ParserMetaInfo>();
		for (Method method : clazz.getMethods()) {
			method.setAccessible(true);
			String methodName = method.getName();
			
			if (method.getName().length() < 4) continue;
            if (!method.getName().startsWith("set")) continue;
            if (method.getParameterTypes().length != 1) continue;
            if (Modifier.isStatic(method.getModifiers())) continue;
            if (Modifier.isAbstract(method.getModifiers())) continue;
            
            ParserMetaInfo parserMetaInfo = new ParserMetaInfo();
            parserMetaInfo.setPropertyNameString(Character.toLowerCase(methodName.charAt(3)) + methodName.substring(4));
            parserMetaInfo.setMethod(method);
            Class<?> type =  method.getParameterTypes()[0];
            parserMetaInfo.setType(type);
            
            if (Collection.class.isAssignableFrom(type)) {
            	Type[] types = method.getGenericParameterTypes();
            	if(types.length != 1 || !(types[0] instanceof ParameterizedType))
            		throw new JsonException("not support the " + method);
            	
            	ParameterizedType paramType = (ParameterizedType) types[0];
            	Type[] types2 = paramType.getActualTypeArguments();
            	if(types2.length != 1 || !(types2[0] instanceof Class))
            		throw new JsonException("not support the " + method);
            	Class<?> actualType = (Class<?>) types2[0];
            	parserMetaInfo.setActualTypeArguments(new Class<?>[]{ actualType });
            }
            
            list.add(parserMetaInfo);
		}
		parserMetaInfos = list.toArray(EMPTY_ARRAY);
		if(parserMetaInfos.length > 0) {
			Arrays.sort(parserMetaInfos);
		}
		return parserMetaInfos;
	}
}
