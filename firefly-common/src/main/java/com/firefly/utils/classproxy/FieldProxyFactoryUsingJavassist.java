package com.firefly.utils.classproxy;

import java.lang.reflect.Field;
import java.util.Map;
import java.util.UUID;

import javassist.ClassClassPath;
import javassist.ClassPool;
import javassist.CtClass;
import javassist.CtConstructor;
import javassist.CtField;
import javassist.CtMethod;

import com.firefly.utils.ReflectUtils.FieldProxy;
import com.firefly.utils.collection.ConcurrentReferenceHashMap;
import com.firefly.utils.StringUtils;

public class FieldProxyFactoryUsingJavassist extends AbstractFieldProxyFactory {
	

	public static final FieldProxyFactoryUsingJavassist INSTANCE = new FieldProxyFactoryUsingJavassist();

	private FieldProxyFactoryUsingJavassist() {}
	
	@SuppressWarnings("unchecked")
	protected FieldProxy _getFieldProxy(Field field) throws Throwable {
//		long start = System.currentTimeMillis();
		ClassPool classPool = ClassPool.getDefault();
		classPool.insertClassPath(new ClassClassPath(FieldProxy.class));
//		classPool.importPackage(Field.class.getCanonicalName());
		
		CtClass cc = classPool.makeClass("com.firefly.utils.ProxyField" + UUID.randomUUID().toString().replace("-", ""));
		cc.addInterface(classPool.get(FieldProxy.class.getName()));
		cc.addField(CtField.make("private java.lang.reflect.Field field;", cc));
		
		CtConstructor constructor = new CtConstructor(new CtClass[]{classPool.get(Field.class.getName())}, cc);
		constructor.setBody("{this.field = (java.lang.reflect.Field)$1;}");
		cc.addConstructor(constructor);
		
		cc.addMethod(CtMethod.make("public java.lang.reflect.Field field(){return field;}", cc));
		cc.addMethod(CtMethod.make(createFieldGetterMethodCode(field), cc));
		cc.addMethod(CtMethod.make(createFieldSetterMethodCode(field), cc));
		
		FieldProxy ret = (FieldProxy) cc.toClass(classLoader, null).getConstructor(Field.class).newInstance(field);
//		long end = System.currentTimeMillis();
//		System.out.println("Javassist generates class proxy time -> " + (end - start));
		return ret;
	}

	private String createFieldGetterMethodCode(Field field) {
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
	
	private String createFieldSetterMethodCode(Field field) {
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
}
