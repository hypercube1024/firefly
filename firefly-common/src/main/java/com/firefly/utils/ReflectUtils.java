package com.firefly.utils;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javassist.ClassClassPath;
import javassist.ClassPool;
import javassist.CtClass;
import javassist.CtConstructor;
import javassist.CtField;
import javassist.CtMethod;

import com.firefly.utils.collection.IdentityHashMap;

public abstract class ReflectUtils {
	
	private static final Map<Class<?>, Map<String, Method>> getterCache = new ConcurrentHashMap<Class<?>, Map<String,Method>>();
	private static final Map<Class<?>, Map<String, Method>> setterCache = new ConcurrentHashMap<Class<?>, Map<String,Method>>();
	private static final Map<Class<?>, Map<String, Field>> propertyCache = new ConcurrentHashMap<Class<?>, Map<String, Field>>();
	private static final Map<Method, MethodProxy> methodCache = new ConcurrentHashMap<Method, MethodProxy>();
	private static final Map<Field, FieldProxy> fieldCache = new ConcurrentHashMap<Field, FieldProxy>();
	private static final Map<Class<?>, ArrayProxy> arrayCache = new ConcurrentHashMap<Class<?>, ArrayProxy>();
	private static final IdentityHashMap<Class<?>, String> primitiveWrapMap = new IdentityHashMap<Class<?>, String>();
	
	static {
		primitiveWrapMap.put(short.class, Short.class.getCanonicalName());
		primitiveWrapMap.put(byte.class, Byte.class.getCanonicalName());
		primitiveWrapMap.put(int.class, Integer.class.getCanonicalName());
		primitiveWrapMap.put(char.class, Character.class.getCanonicalName());
		primitiveWrapMap.put(float.class, Float.class.getCanonicalName());
		primitiveWrapMap.put(double.class, Double.class.getCanonicalName());
		primitiveWrapMap.put(long.class, Long.class.getCanonicalName());
		primitiveWrapMap.put(boolean.class, Boolean.class.getCanonicalName());
	}

	public static interface BeanMethodFilter {
		public boolean accept(String propertyName, Method method);
	}
	
	public static interface BeanFieldFilter {
		public boolean accept(String propertyName, Field field);
	}
	
	public static interface MethodProxy {
		
		Method method();
		
		/**
		 * Executes this method
		 * @param obj The instance of object that contains this method
		 * @param args The parameters of this method
		 * @return Return value of this method
		 */
		Object invoke(Object obj, Object... args);
	}
	
	public static interface FieldProxy {
		
		Field field();
		
		Object get(Object obj);
		
		void set(Object obj, Object value);
	}
	
	public static interface ArrayProxy {
		int size(Object array);
		
		Object get(Object array, int index);
		
		void set(Object array, int index, Object value);
	}
	
	public static void setProperty(Object obj, String property, Object value) throws Throwable {
		getFields(obj.getClass()).get(property).set(obj, value);
	}
	
	public static Object getProperty(Object obj, String property) throws Throwable {
		return getFields(obj.getClass()).get(property).get(obj);
	}
	
	/**
	 * Invokes a object's "setter" method by property name
	 * @param obj The instance of a object
	 * @param property The property name of this object
	 * @param value The parameter of "setter" method that you want to set
	 * @throws Throwable
	 */
	public static void set(Object obj, String property, Object value) throws Throwable {
		getMethodProxy(getSetterMethod(obj.getClass(), property)).invoke(obj, value);
	}
	
	/**
	 * Invokes a object's "getter" method by property name
	 * @param obj The instance of a object
	 * @param property The property name of this object
	 * @return The value of this property
	 * @throws Throwable
	 */
	public static Object get(Object obj, String property) throws Throwable {
		return getMethodProxy(getGetterMethod(obj.getClass(), property)).invoke(obj);
	}
	
	public static Object arrayGet(Object array, int index) throws Throwable {
		return getArrayProxy(array.getClass()).get(array, index);
	}
	
	public static void arraySet(Object array, int index, Object value) throws Throwable {
		getArrayProxy(array.getClass()).set(array, index, value);
	}
	
	public static int arraySize(Object array) throws Throwable {
		return getArrayProxy(array.getClass()).size(array);
	}
	
	@SuppressWarnings("unchecked")
	public static ArrayProxy getArrayProxy(Class<?> clazz) throws Throwable {
		if(!clazz.isArray())
			throw new IllegalArgumentException("type error, it's not array");
			
		ArrayProxy ret = arrayCache.get(clazz);
		if(ret != null)
			return ret;
		
		synchronized(arrayCache) {
			ret = arrayCache.get(clazz);
			if(ret != null)
				return ret;
			
			ClassPool classPool = ClassPool.getDefault();
			classPool.insertClassPath(new ClassClassPath(ArrayProxy.class));
			
			CtClass cc = classPool.makeClass("com.firefly.utils.ArrayField$" + clazz.hashCode());
			cc.addInterface(classPool.get(ArrayProxy.class.getName()));
			
			cc.addMethod(CtMethod.make(createArraySizeCode(clazz), cc));
			cc.addMethod(CtMethod.make(createArrayGetCode(clazz), cc));
			cc.addMethod(CtMethod.make(createArraySetCode(clazz), cc));
			
			ret = (ArrayProxy) cc.toClass().getConstructor().newInstance();
			arrayCache.put(clazz, ret);
		}
		return ret;
	}
	
	private static String createArraySetCode(Class<?> clazz) {
		StringBuilder code = new StringBuilder();
		code.append("public void set(Object array, int index, Object value){\n")
			.append(StringUtils.replace("\t(({})array)[index] = ", clazz.getCanonicalName()));
		
		Class<?> componentType = clazz.getComponentType();
		if(componentType.isPrimitive()) {
			code.append(StringUtils.replace("(({})value).{}Value()", primitiveWrapMap.get(componentType), componentType.getCanonicalName()));
		} else {
			code.append(StringUtils.replace("({})value", componentType.getCanonicalName()));
		}
		
		code.append(";\n")
			.append("}");
		return code.toString();
	}
	
	private static String createArrayGetCode(Class<?> clazz) {
		StringBuilder code = new StringBuilder();
		code.append("public Object get(Object array, int index){\n")
			.append("\treturn ");
		Class<?> componentType = clazz.getComponentType();
		boolean hasValueOf = false;
		if(componentType.isPrimitive()) {
			code.append(StringUtils.replace("(Object){}.valueOf(", primitiveWrapMap.get(componentType)));
			hasValueOf = true;
		}
		
		code.append(StringUtils.replace("(({})array)[index]", clazz.getCanonicalName()));
		if(hasValueOf)
			code.append(")");
		
		code.append(";\n")
		.append("}");
		return code.toString();
	}
	
	private static String createArraySizeCode(Class<?> clazz) {
		StringBuilder code = new StringBuilder();
		code.append("public int size(Object array){\n")
			.append("\treturn ").append(StringUtils.replace("(({})array).length;\n", clazz.getCanonicalName()))
			.append("}");
		return code.toString();
	}
	
	@SuppressWarnings("unchecked")
	public static FieldProxy getFieldProxy(Field field) throws Throwable {
		FieldProxy ret = fieldCache.get(field);
		if(ret != null)
			return ret;
		
		synchronized(fieldCache) {
			ret = fieldCache.get(field);
			if(ret != null)
				return ret;
			
			ClassPool classPool = ClassPool.getDefault();
			classPool.insertClassPath(new ClassClassPath(FieldProxy.class));
			classPool.importPackage(Field.class.getCanonicalName());
			
			CtClass cc = classPool.makeClass("com.firefly.utils.ProxyField$" + field.hashCode());
			cc.addInterface(classPool.get(FieldProxy.class.getName()));
			cc.addField(CtField.make("private Field field;", cc));
			
			CtConstructor constructor = new CtConstructor(new CtClass[]{classPool.get(Field.class.getName())}, cc);
			constructor.setBody("{this.field = (Field)$1;}");
			cc.addConstructor(constructor);
			
			cc.addMethod(CtMethod.make("public Field field(){return field;}", cc));
			cc.addMethod(CtMethod.make(createFieldGetterMethodCode(field), cc));
			cc.addMethod(CtMethod.make(createFieldSetterMethodCode(field), cc));
			
			ret = (FieldProxy) cc.toClass().getConstructor(Field.class).newInstance(field);
			fieldCache.put(field, ret);
		}
		return ret;
	}
	
	private static String createFieldGetterMethodCode(Field field) {
		Class<?> fieldClazz = field.getType();
		StringBuilder code = new StringBuilder();
		code.append("public Object get(Object obj){\n")
			.append("\treturn ");
		
		boolean hasValueOf = false;
		if(fieldClazz.isPrimitive()) {
			code.append(StringUtils.replace("(Object){}.valueOf(", primitiveWrapMap.get(fieldClazz)));
			hasValueOf = true;
		}
		code.append(StringUtils.replace("(({})obj).{}", field.getDeclaringClass().getCanonicalName(), field.getName()));
		if(hasValueOf)
			code.append(")");
		
		code.append(";\n")
			.append("}");
		return code.toString();
	}
	
	private static String createFieldSetterMethodCode(Field field) {
		Class<?> fieldClazz = field.getType();
		StringBuilder code = new StringBuilder();
		code.append("public void set(Object obj, Object value){\n");
		code.append(StringUtils.replace("\t(({})obj).{} = ", field.getDeclaringClass().getCanonicalName(), field.getName()));
		
		if(fieldClazz.isPrimitive()) {
			code.append(StringUtils.replace("(({})value).{}Value()", primitiveWrapMap.get(fieldClazz), fieldClazz.getCanonicalName()));
		} else {
			code.append(StringUtils.replace("({})value", fieldClazz.getCanonicalName()));
		}
		code.append(";\n")
			.append("}");
		return code.toString();
	}
	
	@SuppressWarnings("unchecked")
	public static MethodProxy getMethodProxy(Method method) throws Throwable {
		MethodProxy ret = methodCache.get(method);
		if(ret != null)
			return ret;
		
		synchronized(methodCache) {
			ret = methodCache.get(method);
			if(ret != null)
				return ret;
		
			ClassPool classPool = ClassPool.getDefault();
			classPool.insertClassPath(new ClassClassPath(MethodProxy.class));
			classPool.importPackage(Method.class.getCanonicalName());
			
			CtClass cc = classPool.makeClass("com.firefly.utils.ProxyMethod$" + method.hashCode());
			
			cc.addInterface(classPool.get(MethodProxy.class.getName()));
			cc.addField(CtField.make("private Method method;", cc));
			
			CtConstructor constructor = new CtConstructor(new CtClass[]{classPool.get(Method.class.getName())}, cc);
			constructor.setBody("{this.method = (Method)$1;}");
			cc.addConstructor(constructor);
			
			cc.addMethod(CtMethod.make("public Method method(){return method;}", cc));
			cc.addMethod(CtMethod.make(createInvokeMethodCode(method), cc));
			
			ret = (MethodProxy) cc.toClass().getConstructor(Method.class).newInstance(method);
			methodCache.put(method, ret);
		}
		return ret;
	}
	
	private static String createInvokeMethodCode(Method method) {
		Class<?>[] paramClazz = method.getParameterTypes();
		StringBuilder code = new StringBuilder();

		code.append("public Object invoke(Object obj, Object[] args){\n ");
		if(paramClazz.length > 0)
			code.append('\t')
			.append(StringUtils.replace("if(args == null || args.length != {})", paramClazz.length))
			.append("\n\t\t")
			.append("throw new IllegalArgumentException(\"arguments error\");\n")
			.append('\n');
		
		boolean hasValueOf = false;
		code.append('\t');
		if(!method.getReturnType().equals(Void.TYPE)) {
			code.append("return ");
			if(method.getReturnType().isPrimitive()) {
				code.append(StringUtils.replace("(Object){}.valueOf(", primitiveWrapMap.get(method.getReturnType())));
				hasValueOf = true;
			}
		}
			
		if(Modifier.isStatic(method.getModifiers()))
			code.append(method.getDeclaringClass().getCanonicalName());
		else
			code.append(StringUtils.replace("(({})obj)", method.getDeclaringClass().getCanonicalName()));
		
		code.append('.').append(method.getName()).append('(');
		if(paramClazz.length > 0) {
			int max = paramClazz.length - 1;
			for (int i = 0; ;i++) {
				Class<?> param = paramClazz[i];
				if(param.isPrimitive()) {
					code.append(StringUtils.replace("(({})args[{}]).{}Value()", primitiveWrapMap.get(param), i, param.getCanonicalName()));
				} else {
					code.append(StringUtils.replace("({})args[{}]", param.getCanonicalName(), i));
				}
				
				if(i == max)
					break;
				code.append(", ");
			}
		}
		if(hasValueOf)
			code.append(")");

		code.append(");\n");
		
		if(method.getReturnType().equals(Void.TYPE))
			code.append("\treturn null;\n");
		
		code.append('}');
		
		String ret = code.toString();
		return ret;
	}
	
	/**
	 * Gets the all interface names of this class
	 * 
	 * @param c The class of one object 
	 * @return Returns the all interface names
	 */
	public static String[] getInterfaceNames(Class<?> c) {
		Class<?>[] interfaces = c.getInterfaces();
		List<String> names = new ArrayList<String>();
		for (Class<?> i : interfaces) {
			names.add(i.getName());
		}
		return names.toArray(new String[0]);
	}
	
	public static String getPropertyName(Method method) {
		String methodName = method.getName();
		int index = (methodName.charAt(0) == 'i' ? 2 : 3);
		String propertyName = Character.toLowerCase(methodName.charAt(index)) + methodName.substring(index + 1);
		return propertyName;
	}
	
	public static Method getSetterMethod(Class<?> clazz, String propertyName) {
		return getSetterMethods(clazz).get(propertyName);
	}
	
	public static Map<String, Method> getSetterMethods(Class<?> clazz) {
		Map<String, Method> ret = setterCache.get(clazz);
		if(ret != null)
			return ret;
		
		ret = getSetterMethods(clazz, null);
		setterCache.put(clazz, ret);
		return ret;
	}

	public static Map<String, Method> getSetterMethods(Class<?> clazz, BeanMethodFilter filter) {
		Map<String, Method> setMethodMap = new HashMap<String, Method>();
		Method[] methods = clazz.getMethods();

		for (Method method : methods) {
			method.setAccessible(true);
			if (method.getName().length() < 4
				|| !Character.isUpperCase(method.getName().charAt(3))
				|| !method.getName().startsWith("set")
				|| Modifier.isStatic(method.getModifiers())
				|| !method.getReturnType().equals(Void.TYPE)
				|| method.getParameterTypes().length != 1)
				continue;
			
			String propertyName = getPropertyName(method);
			if (filter == null || filter.accept(propertyName, method))
				setMethodMap.put(propertyName, method);
		}
		return setMethodMap;
	}

	public static Method getGetterMethod(Class<?> clazz, String propertyName) {
		return getGetterMethods(clazz).get(propertyName);
	}
	
	public static Map<String, Method> getGetterMethods(Class<?> clazz) {
		Map<String, Method> ret = getterCache.get(clazz);
		if(ret != null)
			return ret;
		
		ret = getGetterMethods(clazz, null);
		getterCache.put(clazz, ret);
		return ret;
	}
	
	public static Map<String, Method> getGetterMethods(Class<?> clazz, BeanMethodFilter filter) {
		Map<String, Method> getMethodMap = new HashMap<String, Method>();
		Method[] methods = clazz.getMethods();
		for (int i = 0; i < methods.length; i++) {
			Method method = methods[i];
			method.setAccessible(true);
			String methodName = method.getName();

            if (Modifier.isStatic(method.getModifiers())) continue;
            if (Modifier.isAbstract(method.getModifiers())) continue;
            if (method.getName().equals("getClass")) continue;
            if (!(method.getName().startsWith("is") || method.getName().startsWith("get"))) continue;
            if (method.getParameterTypes().length != 0) continue;
            if (method.getReturnType() == void.class) continue;
            int index = (methodName.charAt(0) == 'i' ? 2 : 3);
            if (methodName.length() < index + 1 || !Character.isUpperCase(methodName.charAt(index))) continue;
			
			String propertyName = getPropertyName(method);
			if (filter == null || filter.accept(propertyName, method))
				getMethodMap.put(propertyName, method);
		}

		return getMethodMap;
	}
	
	public static Map<String, Field> getFields(Class<?> clazz) {
		Map<String, Field> ret = propertyCache.get(clazz);
		if(ret != null)
			return ret;
		
		ret = getFields(clazz, null);
		propertyCache.put(clazz, ret);
		return ret;
	}
	
	public static Map<String, Field> getFields(Class<?> clazz, BeanFieldFilter filter) {
		Map<String, Field> fieldMap = new HashMap<String, Field>();
		Field[] fields = clazz.getFields();
		for(Field field : fields) {
			field.setAccessible(true);
			if(Modifier.isStatic(field.getModifiers())) continue;
			
			String propertyName = field.getName();
			if (filter == null || filter.accept(propertyName, field))
				fieldMap.put(propertyName, field);
		}
		return fieldMap;
	}
}
