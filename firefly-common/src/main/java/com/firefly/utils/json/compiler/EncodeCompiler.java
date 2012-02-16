package com.firefly.utils.json.compiler;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;

import com.firefly.utils.json.annotation.Transient;
import com.firefly.utils.json.serializer.StateMachine;
import com.firefly.utils.json.support.JsonObjMetaInfo;

public class EncodeCompiler {
	
	private static final JsonObjMetaInfo[] EMPTY_ARRAY = new JsonObjMetaInfo[0];
	
	public static JsonObjMetaInfo[] compile(Class<?> clazz) {
		JsonObjMetaInfo[] jsonObjMetaInfos = null;
		List<JsonObjMetaInfo> fieldList = new ArrayList<JsonObjMetaInfo>();
		
		boolean first = true;
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
			JsonObjMetaInfo fieldJsonObjMetaInfo = new JsonObjMetaInfo();
			fieldJsonObjMetaInfo.setPropertyName(propertyName, first);
			fieldJsonObjMetaInfo.setMethod(method);
			
			fieldJsonObjMetaInfo.setSerializer(StateMachine.getSerializerInCompiling(fieldClazz));
			fieldList.add(fieldJsonObjMetaInfo);
			first = false;
		}
		
		jsonObjMetaInfos = fieldList.toArray(EMPTY_ARRAY);
		return jsonObjMetaInfos;
	}

}
