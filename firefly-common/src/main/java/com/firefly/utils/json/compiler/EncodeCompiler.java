package com.firefly.utils.json.compiler;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.firefly.utils.json.annotation.Transient;
import com.firefly.utils.json.serializer.SerialStateMachine;
import com.firefly.utils.json.support.SerializerMetaInfo;

public class EncodeCompiler {
	
	private static final SerializerMetaInfo[] EMPTY_ARRAY = new SerializerMetaInfo[0];
	
	public static SerializerMetaInfo[] compile(Class<?> clazz) {
		SerializerMetaInfo[] serializerMetaInfos = null;
		List<SerializerMetaInfo> fieldList = new ArrayList<SerializerMetaInfo>();
		
//		boolean first = true;
		for (Method method : clazz.getMethods()) {
			method.setAccessible(true);
			String methodName = method.getName();
			
			if (method.getName().length() < 3) continue;
            if (Modifier.isStatic(method.getModifiers())) continue;
            if (Modifier.isAbstract(method.getModifiers())) continue;
            if (method.getName().equals("getClass")) continue;
            if (!method.getName().startsWith("is") && !method.getName().startsWith("get")) continue;
            if (method.getParameterTypes().length != 0) continue;
            if (method.getReturnType() == void.class) continue;
            if (method.isAnnotationPresent(Transient.class)) continue;

            String propertyName = null;
			if (methodName.charAt(0) == 'g') {
				if (methodName.length() < 4 || !Character.isUpperCase(methodName.charAt(3))) {
					continue;
				}

				propertyName = Character.toLowerCase(methodName
						.charAt(3)) + methodName.substring(4);
			} else {
				if (methodName.length() < 3
						|| !Character.isUpperCase(methodName.charAt(2))) {
					continue;
				}

				propertyName = Character.toLowerCase(methodName
						.charAt(2)) + methodName.substring(3);
			}
			
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

			Class<?> fieldClazz = method.getReturnType();
			SerializerMetaInfo fieldMetaInfo = new SerializerMetaInfo();
			fieldMetaInfo.setPropertyName(propertyName, false);
			fieldMetaInfo.setMethod(method);
			
			fieldMetaInfo.setSerializer(SerialStateMachine.getSerializerInCompiling(fieldClazz));
			fieldList.add(fieldMetaInfo);
		}
		
		serializerMetaInfos = fieldList.toArray(EMPTY_ARRAY);
		if(serializerMetaInfos.length > 0) {
			Arrays.sort(serializerMetaInfos);
			serializerMetaInfos[0].setPropertyName(serializerMetaInfos[0].getPropertyNameString(), true);
		}
		return serializerMetaInfos;
	}

}
