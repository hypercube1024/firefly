package com.firefly.utils.json.compiler;

import com.firefly.utils.json.annotation.DateFormat;
import com.firefly.utils.json.annotation.Transient;
import com.firefly.utils.json.exception.JsonException;
import com.firefly.utils.json.parser.CollectionParser;
import com.firefly.utils.json.parser.ComplexTypeParser;
import com.firefly.utils.json.parser.MapParser;
import com.firefly.utils.json.parser.ParserStateMachine;
import com.firefly.utils.json.support.FieldInvoke;
import com.firefly.utils.json.support.MethodInvoke;
import com.firefly.utils.json.support.ParserMetaInfo;

import java.lang.reflect.*;
import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import static com.firefly.utils.ReflectUtils.getPropertyName;
import static com.firefly.utils.json.support.PropertyUtils.getDateFormat;
import static com.firefly.utils.json.support.PropertyUtils.isTransientField;

public class DecodeCompiler {
	private static final ParserMetaInfo[] EMPTY_ARRAY = new ParserMetaInfo[0];
	
	public static ParserMetaInfo[] compile(Class<?> clazz) {
		ParserMetaInfo[] parserMetaInfos;
		Set<ParserMetaInfo> fieldSet = new TreeSet<>();
		for (Method method : clazz.getMethods()) {
			method.setAccessible(true);
			String propertyName = getPropertyName(method);
			
			if (method.getDeclaringClass().equals(Object.class)) continue;
			if (method.getName().length() < 4) continue;
            if (!method.getName().startsWith("set")) continue;
            if (method.getParameterTypes().length != 1) continue;
            if (Modifier.isStatic(method.getModifiers())) continue;
            if (Modifier.isAbstract(method.getModifiers())) continue;
            if (method.isAnnotationPresent(Transient.class)) continue;
			if (isTransientField(propertyName, clazz)) continue;
            
            ParserMetaInfo parserMetaInfo = new ParserMetaInfo();
            parserMetaInfo.setPropertyNameString(propertyName);
            parserMetaInfo.setPropertyInvoke(new MethodInvoke(method));
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
            	parserMetaInfo.setType(ComplexTypeParser.getImplClass(type));
            	parserMetaInfo.setParser(new CollectionParser(elementType));
            } else if (Map.class.isAssignableFrom(type)) { // construct map meta information
            	Type[] types = method.getGenericParameterTypes();
            	if(types.length != 1 || !(types[0] instanceof ParameterizedType))
            		throw new JsonException("not support the " + method);
            	
            	ParameterizedType paramType = (ParameterizedType) types[0];
            	Type[] types2 = paramType.getActualTypeArguments();
            	if(types2.length != 2)
            		throw new JsonException("not support the " + method);
            	
            	Type key = types2[0];
            	if (!((key instanceof Class) && key == String.class))
            		throw new JsonException("not support the " + method);
            	
            	Type elementType = types2[1];
            	parserMetaInfo.setType(ComplexTypeParser.getImplClass(type));
            	parserMetaInfo.setParser(new MapParser(elementType));
            } else { // get array, object or enumeration parser
            	parserMetaInfo.setType(type);
            	parserMetaInfo.setParser(ParserStateMachine.getParser(type, getDateFormat(propertyName, clazz, method)));
            }
            fieldSet.add(parserMetaInfo);
		}
		
		for(Field field : clazz.getFields()) { // construct public field parser
			if(Modifier.isTransient(field.getModifiers()) || field.isAnnotationPresent(Transient.class) || Modifier.isStatic(field.getModifiers()))
				continue;
			
			field.setAccessible(true);
			
			ParserMetaInfo parserMetaInfo = new ParserMetaInfo();
            parserMetaInfo.setPropertyNameString(field.getName());
            parserMetaInfo.setPropertyInvoke(new FieldInvoke(field));
            
            Class<?> type = field.getType();
            if (Collection.class.isAssignableFrom(type)) {
            	Type fieldType = field.getGenericType();
            	if(!(fieldType instanceof ParameterizedType))
            		throw new JsonException("not support the " + field);
            	
            	ParameterizedType paramType = (ParameterizedType)fieldType;
            	Type[] types2 = paramType.getActualTypeArguments();
            	if(types2.length != 1)
            		throw new JsonException("not support the " + field);
            	
            	Type elementType = types2[0];
            	parserMetaInfo.setType(ComplexTypeParser.getImplClass(type));
            	parserMetaInfo.setParser(new CollectionParser(elementType));
            } else if (Map.class.isAssignableFrom(type)) { // construct map meta information
            	Type fieldType = field.getGenericType();
            	if(!(fieldType instanceof ParameterizedType))
            		throw new JsonException("not support the " + field);
            	
            	ParameterizedType paramType = (ParameterizedType) fieldType;
            	Type[] types2 = paramType.getActualTypeArguments();
            	if(types2.length != 2)
            		throw new JsonException("not support the " + field);
            	
            	Type key = types2[0];
            	if (!((key instanceof Class) && key == String.class))
            		throw new JsonException("not support the " + field);
            	
            	Type elementType = types2[1];
            	parserMetaInfo.setType(ComplexTypeParser.getImplClass(type));
            	parserMetaInfo.setParser(new MapParser(elementType));
            } else { // get array, object or enumeration parser
            	parserMetaInfo.setType(type);
            	parserMetaInfo.setParser(ParserStateMachine.getParser(type, field.getAnnotation(DateFormat.class)));
            }
            fieldSet.add(parserMetaInfo);
		}
		
		parserMetaInfos = fieldSet.toArray(EMPTY_ARRAY);
		if(parserMetaInfos.length <= 0) {
			throw new JsonException("not support the " + clazz.getName());
		}
		return parserMetaInfos;
	}


}
