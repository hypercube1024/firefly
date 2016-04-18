package com.firefly.utils.classproxy;

import java.util.Map;
import java.util.UUID;

import com.firefly.utils.CompilerUtils;
import com.firefly.utils.ReflectUtils.ArrayProxy;
import com.firefly.utils.collection.ConcurrentReferenceHashMap;
import com.firefly.utils.StringUtils;

public class ArrayProxyFactoryUsingJavaCompiler extends AbstractArrayProxyFactory {
	
	private static final Map<Class<?>, ArrayProxy> arrayCache = new ConcurrentReferenceHashMap<>(256);
	public static final ArrayProxyFactoryUsingJavaCompiler INSTANCE = new ArrayProxyFactoryUsingJavaCompiler();
	
	private ArrayProxyFactoryUsingJavaCompiler(){
		
	}
	
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
	
	private ArrayProxy _getArrayProxy(Class<?> clazz) throws Throwable {
//		long start = System.currentTimeMillis();
		
		String packageName = "com.firefly.utils";
		String className = "ArrayReflectionProxy" + UUID.randomUUID().toString().replace("-", "");
		String completeClassName = packageName + "." + className;
//		System.out.println(completeClassName);
		
		Class<?> componentType = clazz.getComponentType();
		String v = null;
		if(componentType.isPrimitive()) {
			v = StringUtils.replace("(({})value).{}Value()", primitiveWrapMap.get(componentType), componentType.getCanonicalName());
		} else {
			v = "(" + clazz.getComponentType().getCanonicalName() + ")value;\n";
		}
		
		String source = "package " + packageName + ";\n"
				+"public class " + className + " implements " + ArrayProxy.class.getCanonicalName() + " {\n"
					+"@Override\n"
					+"public int size(Object array){\n"
						+"\treturn ((" + clazz.getCanonicalName() + ")array).length;\n"
					+"}\n\n"
					
					+"@Override\n"
					+"public Object get(Object array, int index){\n"
						+"\treturn ((" + clazz.getCanonicalName() + ")array)[index];\n"
					+"}\n\n"
					
					+"@Override\n"
					+"public void set(Object array, int index, Object value){\n"
						+"\t((" + clazz.getCanonicalName() + ")array)[index] = " + v + ";"
					+"}\n\n"
				+"}";
//		System.out.println(source);
		
		
		
		Class<?> arrayProxyClazz = CompilerUtils.compileSource(completeClassName, source);
		if(arrayProxyClazz == null)
			return null;
		
		ArrayProxy obj = (ArrayProxy)arrayProxyClazz.newInstance();
//		long end = System.currentTimeMillis();
//		System.out.println("Java compiler generates class proxy time -> " + (end - start));
		return obj;
	}
}
