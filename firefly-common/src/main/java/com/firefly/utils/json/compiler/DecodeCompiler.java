package com.firefly.utils.json.compiler;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import com.firefly.utils.json.annotation.Transient;
import com.firefly.utils.json.exception.JsonException;
import com.firefly.utils.json.parser.CollectionParser;
import com.firefly.utils.json.parser.ParserStateMachine;
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
            if (method.isAnnotationPresent(Transient.class)) continue;
            
            if (methodName.length() < 4 || !Character.isUpperCase(methodName.charAt(3)))
				continue;
            
            String propertyName = Character.toLowerCase(methodName.charAt(3)) + methodName.substring(4);
            Field field = null;
			try {
				field = clazz.getDeclaredField(propertyName);
			} catch (Throwable t) {
				t.printStackTrace();
			}

			if (field != null
					&& (Modifier.isTransient(field.getModifiers())
					|| field.isAnnotationPresent(Transient.class)))
				continue;
            
            ParserMetaInfo parserMetaInfo = new ParserMetaInfo();
            parserMetaInfo.setPropertyNameString(propertyName);
            parserMetaInfo.setMethod(method);
            Class<?> type =  method.getParameterTypes()[0];
            
            if (Collection.class.isAssignableFrom(type)) {
            	Type[] types = method.getGenericParameterTypes();
            	if(types.length != 1 || !(types[0] instanceof ParameterizedType))
            		throw new JsonException("not support the " + method);
            	
            	ParameterizedType paramType = (ParameterizedType) types[0];
            	Type[] types2 = paramType.getActualTypeArguments();
            	if(types2.length != 1)
            		throw new JsonException("not support the " + method);
            	
            	Type elementType = types2[0];
            	parserMetaInfo.setType(CollectionParser.getImplClass(type));
            	parserMetaInfo.setParser(new CollectionParser(elementType));
            } else if (type.isArray()) { // TODO 数组元信息构造
            	
            } else if (Map.class.isAssignableFrom(clazz)) { // TODO Map元信息构造
            	
            } else { // 获取对象Parser
            	parserMetaInfo.setType(type);
            	parserMetaInfo.setParser(ParserStateMachine.getParser(type)); 
            }
            list.add(parserMetaInfo);
		}
		
		parserMetaInfos = list.toArray(EMPTY_ARRAY);
		if(parserMetaInfos.length > 0)
			Arrays.sort(parserMetaInfos);
		else 
			throw new JsonException("not support the " + clazz.getName());
		return parserMetaInfos;
	}
}
