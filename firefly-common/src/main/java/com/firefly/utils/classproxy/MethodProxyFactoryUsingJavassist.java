package com.firefly.utils.classproxy;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import javassist.ClassClassPath;
import javassist.ClassPool;
import javassist.CtClass;
import javassist.CtConstructor;
import javassist.CtField;
import javassist.CtMethod;

import com.firefly.utils.ReflectUtils.MethodProxy;
import com.firefly.utils.StringUtils;

public class MethodProxyFactoryUsingJavassist extends AbstractMethodProxyFactory {
	
	private static final Map<Method, MethodProxy> methodCache = new ConcurrentHashMap<Method, MethodProxy>();
	public static final MethodProxyFactoryUsingJavassist INSTANCE = new MethodProxyFactoryUsingJavassist();

	private MethodProxyFactoryUsingJavassist() {
		
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
		}
		return ret;
	}
	
	@SuppressWarnings("unchecked")
	private MethodProxy _getMethodProxy(Method method) throws Throwable {
//		long start = System.currentTimeMillis();
		ClassPool classPool = ClassPool.getDefault();
		classPool.insertClassPath(new ClassClassPath(MethodProxy.class));
		classPool.importPackage(Method.class.getCanonicalName());
		
		CtClass cc = classPool.makeClass("com.firefly.utils.ProxyMethod" + UUID.randomUUID().toString().replace("-", ""));
		
		cc.addInterface(classPool.get(MethodProxy.class.getName()));
		cc.addField(CtField.make("private Method method;", cc));
		
		CtConstructor constructor = new CtConstructor(new CtClass[]{classPool.get(Method.class.getName())}, cc);
		constructor.setBody("{this.method = (Method)$1;}");
		cc.addConstructor(constructor);
		
		cc.addMethod(CtMethod.make("public Method method(){return method;}", cc));
		cc.addMethod(CtMethod.make(createInvokeMethodCode(method), cc));
		
		MethodProxy ret = (MethodProxy) cc.toClass().getConstructor(Method.class).newInstance(method);
//		long end = System.currentTimeMillis();
//		System.out.println("Javassist generates class proxy time -> " + (end - start));
		return ret;
	}

	private String createInvokeMethodCode(Method method) {
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
}
