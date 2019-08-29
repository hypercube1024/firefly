package com.fireflysource.common.bytecode;

import com.fireflysource.common.string.StringUtils;
import javassist.*;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class JavassistClassProxyFactory implements ClassProxyFactory {

    public static final JavassistClassProxyFactory INSTANCE = new JavassistClassProxyFactory();
    public static ClassLoader classLoader;

    static {
        classLoader = Thread.currentThread().getContextClassLoader();
    }

    private JavassistClassProxyFactory() {
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    @Override
    public <T> T createProxy(T instance, ClassProxy proxy, MethodFilter filter) throws Throwable {
        ClassPool classPool = ClassPool.getDefault();
        classPool.insertClassPath(new ClassClassPath(ClassProxyFactory.class));

        Class<?> clazz = instance.getClass();
        // make class
        CtClass cc = classPool.makeClass("com.firefly.utils.ClassProxy" + UUID.randomUUID().toString().replace("-", ""));
        cc.setSuperclass(classPool.get(clazz.getName()));

        // make fields
        cc.addField(CtField.make("private " + ClassProxy.class.getCanonicalName() + " classProxy;", cc));
        cc.addField(CtField.make("private " + clazz.getCanonicalName() + " originalInstance;", cc));
        cc.addField(CtField.make("private " + MethodProxy[].class.getCanonicalName() + " methodProxies;", cc));

        // make constructor
        CtConstructor empty = new CtConstructor(null, cc);
        empty.setBody("{}");
        CtConstructor constructor = new CtConstructor(new CtClass[]{
                classPool.get(ClassProxy.class.getName()),
                classPool.get(clazz.getName()),
                classPool.get(MethodProxy[].class.getName())
        }, cc);
        constructor.setBody("{"
                + "this.classProxy = (" + ClassProxy.class.getCanonicalName() + ")$1;"
                + "this.originalInstance = (" + clazz.getCanonicalName() + ")$2;"
                + "this.methodProxies = (" + MethodProxy[].class.getCanonicalName() + ")$3;"
                + "}");
        cc.addConstructor(empty);
        cc.addConstructor(constructor);

        // make methods
        List<Method> list = new ArrayList<>();
        for (Method m : clazz.getMethods()) {
            if (m.getDeclaringClass().equals(Object.class)
                    || Modifier.isFinal(m.getModifiers())
                    || Modifier.isStatic(m.getModifiers())
                    || Modifier.isNative(m.getModifiers())) {
                continue;
            }
            if (filter != null && !filter.accept(m)) {
                continue;
            }
            list.add(m);
        }

        Method[] methods = list.toArray(new Method[0]);
        MethodProxy[] methodProxies = new MethodProxy[methods.length];
        for (int i = 0; i < methods.length; i++) {
            Method m = methods[i];
            methodProxies[i] = JavassistReflectionProxyFactory.INSTANCE.getMethodProxy(m);
            StringBuilder parameterArray = new StringBuilder("Object[] args = new Object[]{");
            StringBuilder str = new StringBuilder("public " + m.getReturnType().getCanonicalName() + " " + m.getName() + "(");
            Class[] parameters = m.getParameterTypes();
            for (int j = 0; j < parameters.length; j++) {
                if (j != 0) {
                    str.append(", ");
                    parameterArray.append(", ");
                }
                str.append(parameters[j].getCanonicalName()).append(" arg").append(j);
                if (parameters[j].isPrimitive()) {
                    parameterArray.append(StringUtils.replace("(Object){}.valueOf(", AbstractProxyFactory.primitiveWrapMap.get(parameters[j]))).append("arg").append(j).append(")");
                } else {
                    parameterArray.append("(Object)arg").append(j);
                }
            }
            str.append("){\n");
            parameterArray.append("};\n");
            if (parameters.length == 0) {
                parameterArray = new StringBuilder(Object[].class.getCanonicalName() + " args = new Object[0];\n");
            }

            str.append("\t").append(parameterArray);
            if (!m.getReturnType().equals(void.class)) {
                if (m.getReturnType().isPrimitive()) {
                    str.append("\t").append(m.getReturnType().getCanonicalName()).append(" ret = ((").append(AbstractProxyFactory.primitiveWrapMap.get(m.getReturnType())).append(")").append("classProxy.intercept(methodProxies[").append(i).append("], ").append("originalInstance, ").append("args").append(")).").append(m.getReturnType().getCanonicalName()).append("Value()").append(";\n");
                } else {
                    str.append("\t").append(m.getReturnType().getCanonicalName()).append(" ret = (").append(m.getReturnType().getCanonicalName()).append(")").append("classProxy.intercept(methodProxies[").append(i).append("], ").append("originalInstance, ").append("args").append(");\n");
                }
                str.append("\treturn ret;\n");
            } else {
                str.append("\tclassProxy.intercept(methodProxies[").append(i).append("], ").append("originalInstance, ").append("args").append(");\n");
            }
            str.append("}");
            cc.addMethod(CtMethod.make(str.toString(), cc));
        }

        // generate a proxy instance
        return (T) cc.toClass(classLoader, null).getConstructor(ClassProxy.class, clazz, MethodProxy[].class).newInstance(proxy, instance, methodProxies);
    }
}
