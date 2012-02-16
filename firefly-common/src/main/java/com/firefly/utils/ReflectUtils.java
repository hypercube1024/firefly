package com.firefly.utils;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public abstract class ReflectUtils {

	public static String getPropertyNameBySetterMethod(Method method) {
		String methodName = method.getName();
		String propertyName = Character.toLowerCase(methodName.charAt(3))
				+ methodName.substring(4);
		return propertyName;
	}

	public static interface BeanMethodFilter {
		boolean accept(String propertyName, Method method);
	}

	public static Map<String, Method> getSetterMethods(Class<?> clazz) {
		return getSetterMethods(clazz, null);
	}

	public static Map<String, Method> getSetterMethods(Class<?> clazz,
			BeanMethodFilter filter) {
		Map<String, Method> beanSetMethod = new HashMap<String, Method>();
		Method[] methods = clazz.getMethods();

		for (Method method : methods) {

			if (!method.getName().startsWith("set")
					|| Modifier.isStatic(method.getModifiers())
					|| !method.getReturnType().equals(Void.TYPE)
					|| method.getParameterTypes().length != 1) {
				continue;
			}
			String propertyName = getPropertyNameBySetterMethod(method);
			method.setAccessible(true);

			if (filter == null || filter.accept(propertyName, method))
				beanSetMethod.put(propertyName, method);
		}
		return beanSetMethod;
	}

	/**
	 * 获取所有接口名称
	 * 
	 * @param c
	 * @return
	 */
	public static String[] getInterfaceNames(Class<?> c) {
		Class<?>[] interfaces = c.getInterfaces();
		List<String> names = new ArrayList<String>();
		for (Class<?> i : interfaces) {
			names.add(i.getName());
		}
		return names.toArray(new String[0]);
	}

	public static Method getGetterMethod(Class<?> clazz, String p) {
		Method ret = null;
		Method[] methods = clazz.getMethods();
		for (int i = 0; i < methods.length; i++) {
			Method method = methods[i];
			method.setAccessible(true);
			String methodName = method.getName();

			if (method.getName().length() < 3) continue;
            if (Modifier.isStatic(method.getModifiers())) continue;
            if (Modifier.isAbstract(method.getModifiers())) continue;
            if (method.getName().equals("getClass")) continue;
            if (!method.getName().startsWith("is") && !method.getName().startsWith("get")) continue;
            if (method.getParameterTypes().length != 0) continue;
            if (method.getReturnType() == void.class) continue;

			if (methodName.charAt(0) == 'g') { // 取get方法的返回值
				if (methodName.length() < 4 || !Character.isUpperCase(methodName.charAt(3))) {
					continue;
				}

				String propertyName = Character.toLowerCase(methodName
						.charAt(3)) + methodName.substring(4);

				if (propertyName.equals(p))
					return method;

			} else { // 取is方法的返回值
				if (methodName.length() < 3 || !Character.isUpperCase(methodName.charAt(2))) {
					continue;
				}

				String propertyName = Character.toLowerCase(methodName
						.charAt(2)) + methodName.substring(3);

				if (propertyName.equals(p))
					return method;
			}
		}

		return ret;
	}
}
