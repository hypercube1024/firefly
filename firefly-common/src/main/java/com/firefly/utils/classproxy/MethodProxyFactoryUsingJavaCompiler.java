package com.firefly.utils.classproxy;

import java.lang.reflect.Method;
import java.util.Map;
import java.util.UUID;

import com.firefly.utils.CompilerUtils;
import com.firefly.utils.ReflectUtils.MethodProxy;
import com.firefly.utils.collection.ConcurrentReferenceHashMap;
import com.firefly.utils.StringUtils;

public class MethodProxyFactoryUsingJavaCompiler extends AbstractMethodProxyFactory {

	private static final Map<Method, MethodProxy> methodCache = new ConcurrentReferenceHashMap<>(256);
	public static final MethodProxyFactoryUsingJavaCompiler INSTANCE = new MethodProxyFactoryUsingJavaCompiler();
	
	private MethodProxyFactoryUsingJavaCompiler() {
		
	}
	
	@Override
	public MethodProxy getMethodProxy(Method method) throws Throwable {
		MethodProxy ret = methodCache.get(method);
		if(ret != null)
			return ret;
		
		synchronized(methodCache) {
			ret = methodCache.get(method);
			if(ret != null)
				return ret;
		
			ret = _getMethodProxy(method);
			methodCache.put(method, ret);
			return ret;
		}
	}
	
	private MethodProxy _getMethodProxy(Method method) throws Throwable {
//		long start = System.currentTimeMillis();
		String packageName = "com.firefly.utils";
		String className = "MethodReflectionProxy" + UUID.randomUUID().toString().replace("-", "");
		String completeClassName = packageName + "." + className;
		
		Class<?>[] paramClazz = method.getParameterTypes();
		String returnCode = "";		
		if(!method.getReturnType().equals(Void.TYPE)) {
			returnCode += "return ";
		}
		
		returnCode += "((" + method.getDeclaringClass().getCanonicalName() + ")obj)." + method.getName() + "(";
		if(paramClazz.length > 0) {
			int max = paramClazz.length - 1;
			for (int i = 0; ;i++) {
				Class<?> param = paramClazz[i];
				if(param.isPrimitive()) {
					returnCode += StringUtils.replace("(({})args[{}]).{}Value()", primitiveWrapMap.get(param), i, param.getCanonicalName());
				} else {
					returnCode += "(" + param.getCanonicalName() + ")args[" + i + "]";
				}
				
				if(i == max)
					break;
				
				returnCode += ",";
			}
		}
		returnCode += ");";
		
		if(method.getReturnType().equals(Void.TYPE)) {
			returnCode += "\n\treturn null;";
		}
		
		String source = "package " + packageName + ";\n"
				+ "import " + Method.class.getCanonicalName() + ";\n"
				+"public class " + className + " implements " + MethodProxy.class.getCanonicalName() + " {\n"
					+"private Method method;\n"
					+"public " + className + "(Method method){\n"
						+"\tthis.method = method;\n"
					+"}\n\n"
					+"public Method method(){return method;}\n\n"
					+"public Object invoke(Object obj, Object[] args){\n"
						+"\tif(args == null || args.length != " + paramClazz.length +")\n"
							+"\t\tthrow new IllegalArgumentException(\"arguments error\");\n\n"
						+"\t" + returnCode + "\n"
					+"}\n"
				+"}"
				;
		
//		System.out.println(source);
		Class<?> methodProxyClass = CompilerUtils.compileSource(completeClassName, source);
		if(methodProxyClass == null)
			return null;
		
		MethodProxy obj = (MethodProxy)methodProxyClass.getConstructor(Method.class).newInstance(method);
//		long end = System.currentTimeMillis();
//		System.out.println("Java compiler generates class proxy time -> " + (end - start));
		return obj;
	}
}
