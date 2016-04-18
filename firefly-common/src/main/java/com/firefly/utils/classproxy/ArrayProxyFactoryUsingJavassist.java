package com.firefly.utils.classproxy;

import java.util.Map;
import java.util.UUID;

import javassist.ClassClassPath;
import javassist.ClassPool;
import javassist.CtClass;
import javassist.CtMethod;

import com.firefly.utils.ReflectUtils.ArrayProxy;
import com.firefly.utils.collection.ConcurrentReferenceHashMap;
import com.firefly.utils.StringUtils;

public class ArrayProxyFactoryUsingJavassist extends AbstractArrayProxyFactory {

	private static final Map<Class<?>, ArrayProxy> arrayCache = new ConcurrentReferenceHashMap<>(256);
	public static final ArrayProxyFactoryUsingJavassist INSTANCE = new ArrayProxyFactoryUsingJavassist();
	
	private ArrayProxyFactoryUsingJavassist() {}
	
	@Override
	public ArrayProxy getArrayProxy(Class<?> clazz) throws Throwable {
		if(!clazz.isArray())
			throw new IllegalArgumentException("type error, it's not array");
			
		ArrayProxy ret = arrayCache.get(clazz);
		if(ret != null)
			return ret;
		
		synchronized(arrayCache) {
			ret = arrayCache.get(clazz);
			if(ret != null)
				return ret;
			
			ret = _getArrayProxy(clazz);
			arrayCache.put(clazz, ret);
			return ret;
		}
	}
	
	@SuppressWarnings("unchecked")
	private ArrayProxy _getArrayProxy(Class<?> clazz) throws Throwable {
//		long start = System.currentTimeMillis();
		ClassPool classPool = ClassPool.getDefault();
		classPool.insertClassPath(new ClassClassPath(ArrayProxy.class));
		
		CtClass cc = classPool.makeClass("com.firefly.utils.ArrayField" + UUID.randomUUID().toString().replace("-", ""));
		cc.addInterface(classPool.get(ArrayProxy.class.getName()));
		
		cc.addMethod(CtMethod.make(createArraySizeCode(clazz), cc));
		cc.addMethod(CtMethod.make(createArrayGetCode(clazz), cc));
		cc.addMethod(CtMethod.make(createArraySetCode(clazz), cc));
		
		ArrayProxy ret = (ArrayProxy) cc.toClass(classLoader, null).getConstructor().newInstance();
//		long end = System.currentTimeMillis();
//		System.out.println("Javassist generates class proxy time -> " + (end - start));
		return ret;
	}
	
	private String createArraySetCode(Class<?> clazz) {
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
	
	private String createArrayGetCode(Class<?> clazz) {
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
	
	private String createArraySizeCode(Class<?> clazz) {
		StringBuilder code = new StringBuilder();
		code.append("public int size(Object array){\n")
			.append("\treturn ").append(StringUtils.replace("(({})array).length;\n", clazz.getCanonicalName()))
			.append("}");
		return code.toString();
	}

}
