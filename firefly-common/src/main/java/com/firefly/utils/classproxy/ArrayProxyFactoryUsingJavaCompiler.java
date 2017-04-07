package com.firefly.utils.classproxy;

import com.firefly.utils.CompilerUtils;
import com.firefly.utils.ReflectUtils.ArrayProxy;
import com.firefly.utils.StringUtils;

import java.util.UUID;

public class ArrayProxyFactoryUsingJavaCompiler extends AbstractArrayProxyFactory {
	

	public static final ArrayProxyFactoryUsingJavaCompiler INSTANCE = new ArrayProxyFactoryUsingJavaCompiler();
	
	private ArrayProxyFactoryUsingJavaCompiler(){
		
	}
	
	protected ArrayProxy _getArrayProxy(Class<?> clazz) throws Throwable {
//		long start = System.currentTimeMillis();
		
		String packageName = "com.firefly.utils";
		String className = "ArrayReflectionProxy" + UUID.randomUUID().toString().replace("-", "");
		String completeClassName = packageName + "." + className;
//		System.out.println(completeClassName);
		
		Class<?> componentType = clazz.getComponentType();
		String v;
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
