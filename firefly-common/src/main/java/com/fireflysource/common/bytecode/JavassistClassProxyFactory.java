package com.fireflysource.common.bytecode;

import javassist.*;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static com.fireflysource.common.string.StringUtils.replace;

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

            Class[] parameters = m.getParameterTypes();

            StringBuilder parameterArray;
            if (parameters.length == 0) {
                parameterArray = new StringBuilder("Object[] args = new Object[0];\n");
            } else {
                parameterArray = new StringBuilder("Object[] args = new Object[]{");
            }

            StringBuilder str = new StringBuilder("public " + m.getReturnType().getCanonicalName() + " " + m.getName() + "(");

            for (int j = 0; j < parameters.length; j++) {
                if (j != 0) {
                    str.append(", ");
                    parameterArray.append(", ");
                }

                str.append(parameters[j].getCanonicalName()).append(" arg").append(j);

                if (parameters[j].isPrimitive()) {
                    String t = "(Object){}.valueOf(arg{})";
                    parameterArray.append(replace(t, AbstractProxyFactory.primitiveWrapMap.get(parameters[j]), j));
                } else {
                    parameterArray.append("(Object)arg").append(j);
                }
            }

            str.append("){\n");

            if (parameters.length > 0) {
                parameterArray.append("};\n");
            }

            str.append("\t").append(parameterArray);
            if (!m.getReturnType().equals(void.class)) {
                if (m.getReturnType().isPrimitive()) {
                    String t = "\t{} ret = (({})classProxy.intercept(methodProxies[{}], originalInstance, args)).{}Value();\n";
                    str.append(replace(t,
                            m.getReturnType().getCanonicalName(),
                            AbstractProxyFactory.primitiveWrapMap.get(m.getReturnType()),
                            i,
                            m.getReturnType().getCanonicalName()));
                } else {
                    String t = "\t{} ret = ({})classProxy.intercept(methodProxies[{}], originalInstance, args);\n";
                    str.append(replace(t,
                            m.getReturnType().getCanonicalName(),
                            m.getReturnType().getCanonicalName(),
                            i));
                }
                str.append("\treturn ret;\n");
            } else {
                String t = "\tclassProxy.intercept(methodProxies[{}], originalInstance, args);\n";
                str.append(replace(t, i));
            }
            str.append("}");
            cc.addMethod(CtMethod.make(str.toString(), cc));
        }

        // generate a proxy instance
        return (T) cc.toClass(classLoader, null)
                     .getConstructor(ClassProxy.class, clazz, MethodProxy[].class)
                     .newInstance(proxy, instance, methodProxies);
    }
}
