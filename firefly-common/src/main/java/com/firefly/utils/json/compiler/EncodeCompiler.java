package com.firefly.utils.json.compiler;

import com.firefly.utils.json.annotation.DateFormat;
import com.firefly.utils.json.annotation.Transient;
import com.firefly.utils.json.serializer.SerialStateMachine;
import com.firefly.utils.json.support.FieldInvoke;
import com.firefly.utils.json.support.MethodInvoke;
import com.firefly.utils.json.support.SerializerMetaInfo;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Set;
import java.util.TreeSet;

import static com.firefly.utils.ReflectUtils.getPropertyName;
import static com.firefly.utils.json.support.PropertyUtils.getDateFormat;
import static com.firefly.utils.json.support.PropertyUtils.isTransientField;

public class EncodeCompiler {
	
	private static final SerializerMetaInfo[] EMPTY_ARRAY = new SerializerMetaInfo[0];
	
	public static SerializerMetaInfo[] compile(Class<?> clazz) {
		SerializerMetaInfo[] serializerMetaInfos;
		Set<SerializerMetaInfo> fieldSet = new TreeSet<>();
		
		for (Method method : clazz.getMethods()) {
			method.setAccessible(true);
			String propertyName = getPropertyName(method);
			
			if (method.getDeclaringClass().equals(Object.class)) continue;
			if (method.getName().length() < 3) continue;
            if (Modifier.isStatic(method.getModifiers())) continue;
            if (Modifier.isAbstract(method.getModifiers())) continue;
            if (!method.getName().startsWith("get") && !method.getName().startsWith("is")) continue;
            if (method.getParameterTypes().length != 0) continue;
            if (method.getReturnType() == void.class) continue;
            if (method.isAnnotationPresent(Transient.class)) continue;
			if (isTransientField(propertyName, clazz)) continue;

			Class<?> fieldClazz = method.getReturnType();
			SerializerMetaInfo fieldMetaInfo = new SerializerMetaInfo();
			fieldMetaInfo.setPropertyName(propertyName, false);
			fieldMetaInfo.setPropertyInvoke(new MethodInvoke(method));
			fieldMetaInfo.setSerializer(SerialStateMachine.getSerializer(fieldClazz, getDateFormat(propertyName, clazz, method)));
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
