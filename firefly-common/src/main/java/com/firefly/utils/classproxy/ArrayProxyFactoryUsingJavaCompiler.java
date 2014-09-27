package com.firefly.utils.classproxy;

import java.nio.charset.Charset;
import java.util.Arrays;

import javax.tools.JavaCompiler.CompilationTask;
import javax.tools.JavaFileManager;

import com.firefly.utils.CompilerUtils;
import com.firefly.utils.CompilerUtils.JavaSourceFromString;
import com.firefly.utils.ReflectUtils.ArrayProxy;
import com.firefly.utils.StringUtils;

public class ArrayProxyFactoryUsingJavaCompiler extends AbstractArrayProxyFactory {
	@Override
	public ArrayProxy getArrayProxy(Class<?> clazz) throws Throwable {
		long start = System.currentTimeMillis();
		
		String packageName = "com.firefly.utils";
		String className = "ArrayReflectionProxy" + clazz.hashCode();
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
		
		boolean result = false;
		JavaFileManager fileManager = CompilerUtils.getStringSourceJavaFileManager(CompilerUtils.compiler, null, null, Charset.forName("UTF-8"));
		try {
			CompilationTask task = CompilerUtils.compiler.getTask(null, fileManager, null, null, null,Arrays.asList(new JavaSourceFromString(completeClassName, source)));
			result = task.call();
		} finally {
			fileManager.close();
		}
		
		if(!result)
			return null;
		
		Class<?> arrayProxyClazz = CompilerUtils.getClassByName(completeClassName);
		ArrayProxy obj = (ArrayProxy)arrayProxyClazz.newInstance();
		
		long end = System.currentTimeMillis();
		System.out.println("Java compiler generates class proxy time -> " + (end - start));
		return obj;
	}
}
