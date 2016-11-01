package com.firefly.utils.json.compiler;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Set;
import java.util.TreeSet;

import com.firefly.utils.json.annotation.DateFormat;
import com.firefly.utils.json.annotation.Transient;
import com.firefly.utils.json.serializer.SerialStateMachine;
import com.firefly.utils.json.support.FieldInvoke;
import com.firefly.utils.json.support.MethodInvoke;
import com.firefly.utils.json.support.SerializerMetaInfo;

public class EncodeCompiler {
	
	private static final SerializerMetaInfo[] EMPTY_ARRAY = new SerializerMetaInfo[0];
	
	public static SerializerMetaInfo[] compile(Class<?> clazz) {
		SerializerMetaInfo[] serializerMetaInfos = null;
		Set<SerializerMetaInfo> fieldSet = new TreeSet<SerializerMetaInfo>();
		
		for (Method method : clazz.getMethods()) {
			method.setAccessible(true);
			String methodName = method.getName();
			
			if (method.getDeclaringClass().equals(Object.class)) continue;
			if (method.getName().length() < 3) continue;
            if (Modifier.isStatic(method.getModifiers())) continue;
            if (Modifier.isAbstract(method.getModifiers())) continue;
            if (!method.getName().startsWith("get") && !method.getName().startsWith("is")) continue;
            if (method.getParameterTypes().length != 0) continue;
            if (method.getReturnType() == void.class) continue;
            if (method.isAnnotationPresent(Transient.class)) continue;

            String propertyName = null;
			if (methodName.charAt(0) == 'g') { // start with 'get'
				if (methodName.length() < 4)
					continue;

				if(Character.isUpperCase(methodName.charAt(3))) {
					propertyName = Character.toLowerCase(methodName.charAt(3)) + methodName.substring(4);
				} else {
					propertyName = methodName.substring(3);
				}
			} else { // start with 'is'
				if (methodName.length() < 3)
					continue;

				if(Character.isUpperCase(methodName.charAt(2))) {
					propertyName = Character.toLowerCase(methodName.charAt(2)) + methodName.substring(3);
				} else {
					propertyName = methodName.substring(2);
				}
			}
			
			Field field = null;
			try {
				field = clazz.getDeclaredField(propertyName);
			} catch (Throwable t) {
				System.err.println("get declared field exception, " + t.getMessage());
			}

			if (field != null
					&& (Modifier.isTransient(field.getModifiers())
					|| field.isAnnotationPresent(Transient.class)))
				continue;

			Class<?> fieldClazz = method.getReturnType();
			SerializerMetaInfo fieldMetaInfo = new SerializerMetaInfo();
			fieldMetaInfo.setPropertyName(propertyName, false);
			fieldMetaInfo.setPropertyInvoke(new MethodInvoke(method));
			
			DateFormat d = null;
			if(field != null) {
				d = field.getAnnotation(DateFormat.class);
			}
			if(d == null) {
				d = method.getAnnotation(DateFormat.class);
			}
			
			fieldMetaInfo.setSerializer(SerialStateMachine.getSerializer(fieldClazz, d));
			fieldSet.add(fieldMetaInfo);
		}
		
		for(Field field : clazz.getFields()) { // construct public field serializer
			if(Modifier.isTransient(field.getModifiers()) || field.isAnnotationPresent(Transient.class) || Modifier.isStatic(field.getModifiers()))
				continue;
			
			field.setAccessible(true);
			SerializerMetaInfo fieldMetaInfo = new SerializerMetaInfo();
			fieldMetaInfo.setPropertyName(field.getName(), false);
			fieldMetaInfo.setPropertyInvoke(new FieldInvoke(field));
			fieldMetaInfo.setSerializer(SerialStateMachine.getSerializer(field.getType(), field.getAnnotation(DateFormat.class)));
			fieldSet.add(fieldMetaInfo);
		}
		
		serializerMetaInfos = fieldSet.toArray(EMPTY_ARRAY);
		if(serializerMetaInfos.length > 0) {
			serializerMetaInfos[0].setPropertyName(serializerMetaInfos[0].getPropertyNameString(), true);
		}
		return serializerMetaInfos;
	}

}
