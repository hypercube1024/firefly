package com.firefly.utils.classproxy;

import com.firefly.utils.ReflectUtils.MethodProxy;
import com.firefly.utils.StringUtils;
import javassist.*;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class ClassProxyFactoryUsingJavassist implements ClassProxyFactory {

    public static final ClassProxyFactoryUsingJavassist INSTANCE = new ClassProxyFactoryUsingJavassist();
    public static ClassLoader classLoader;

    static {
        classLoader = Thread.currentThread().getContextClassLoader();
    }

    private ClassProxyFactoryUsingJavassist() {
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    @Override
    public Object createProxy(Object instance, ClassProxy proxy, MethodFilter filter) throws Throwable {
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
            methodProxies[i] = MethodProxyFactoryUsingJavassist.INSTANCE.getMethodProxy(m);
            String parameterArray = "Object[] args = new Object[]{";
            String str = "public " + m.getReturnType().getCanonicalName() + " " + m.getName() + "(";
            Class[] parameters = m.getParameterTypes();
            for (int j = 0; j < parameters.length; j++) {
                if (j != 0) {
                    str += ", ";
                    parameterArray += ", ";
                }
                str += parameters[j].getCanonicalName() + " arg" + j;
                if (parameters[j].isPrimitive()) {
                    parameterArray += StringUtils.replace("(Object){}.valueOf(", AbstractProxyFactory.primitiveWrapMap.get(parameters[j]))
                            + "arg" + j + ")";
                } else {
                    parameterArray += "(Object)arg" + j;
                }
            }
            str += "){\n";
            parameterArray += "};\n";
            if (parameters.length == 0) {
                parameterArray = Object[].class.getCanonicalName() + " args = new Object[0];\n";
            }

            str += "\t" + parameterArray;
            if (!m.getReturnType().equals(void.class)) {
                if (m.getReturnType().isPrimitive()) {
                    str += "\t" + m.getReturnType().getCanonicalName() + " ret = (("
                            + AbstractProxyFactory.primitiveWrapMap.get(m.getReturnType()) + ")"
                            + "classProxy.intercept(methodProxies[" + i + "], "
                            + "originalInstance, "
                            + "args"
                            + "))." + m.getReturnType().getCanonicalName() + "Value()"
                            + ";\n";
                } else {
                    str += "\t" + m.getReturnType().getCanonicalName() + " ret = ("
                            + m.getReturnType().getCanonicalName() + ")"
                            + "classProxy.intercept(methodProxies[" + i + "], "
                            + "originalInstance, "
                            + "args"
                            + ");\n";
                }
                str += "\treturn ret;\n";
            } else {
                str += "\tclassProxy.intercept(methodProxies[" + i + "], "
                        + "originalInstance, "
                        + "args"
                        + ");\n";
            }
            str += "}";
//			System.out.println(str);
            cc.addMethod(CtMethod.make(str, cc));
        }

        // generate proxy instance
        return cc.toClass(classLoader, null).getConstructor(ClassProxy.class, clazz, MethodProxy[].class).newInstance(proxy, instance, methodProxies);
    }
}
