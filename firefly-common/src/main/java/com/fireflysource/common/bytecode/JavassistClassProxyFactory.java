package com.fireflysource.common.bytecode;

import javassist.*;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static com.fireflysource.common.string.StringUtils.replace;

public class JavassistClassProxyFactory implements ClassProxyFactory {

    public static final JavassistClassProxyFactory INSTANCE = new JavassistClassProxyFactory();

    private JavassistClassProxyFactory() {
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T> T createProxy(T instance, ClassProxy proxy, MethodFilter filter) throws Throwable {
        Class<?> clazz = instance.getClass();
        Method[] methods = Arrays.stream(clazz.getMethods()).filter(m -> filterMethods(filter, m)).toArray(Method[]::new);
        if (methods.length == 0) {
            return instance;
        }

        ClassPool classPool = ClassPool.getDefault();
        classPool.insertClassPath(new ClassClassPath(ClassProxyFactory.class));
        CtClass cc = buildClass(classPool, clazz);
        buildPrivateFields(clazz, cc);
        buildConstructor(classPool, clazz, cc);
        List<String> methodCodes = buildMethodCodes(methods);
        for (String str : methodCodes) {
            cc.addMethod(CtMethod.make(str, cc));
        }

        MethodProxy[] methodProxies = getMethodProxies(methods);
        return (T) cc.toClass(this.getClass())
                     .getConstructor(ClassProxy.class, clazz, MethodProxy[].class)
                     .newInstance(proxy, instance, methodProxies);
    }

    private List<String> buildMethodCodes(Method[] methods) {
        return IntStream
                .range(0, methods.length)
                .boxed()
                .map(index -> buildMethodCode(methods[index], index))
                .collect(Collectors.toList());
    }

    private String buildMethodCode(Method m, Integer index) {
        Class<?>[] parameters = m.getParameterTypes();
        return buildMethodSignatureLine(m, parameters) + "{\n" +
                convertParametersToObjectArray(parameters) +
                buildInvokeInterceptMethodAndReturnLine(m, index) +
                "}";
    }

    private MethodProxy[] getMethodProxies(Method[] methods) {
        return Arrays.stream(methods)
                     .map(JavassistReflectionProxyFactory.INSTANCE::getMethodProxy)
                     .toArray(MethodProxy[]::new);
    }

    private String buildMethodSignatureLine(Method m, Class<?>[] parameters) {
        String t = "public {} {} ({})";
        return replace(t, m.getReturnType().getCanonicalName(), m.getName(), buildParameters(parameters));
    }

    private String convertParametersToObjectArray(Class<?>[] parameters) {
        if (parameters == null || parameters.length == 0) {
            return "Object[] args = new Object[0];\n";
        } else {
            return "Object[] args = new Object[]{" +
                    buildParameterObjectArray(parameters) +
                    "};\n";
        }
    }

    private String buildInvokeInterceptMethodAndReturnLine(Method m, Integer index) {
        if (!m.getReturnType().equals(void.class)) {
            if (m.getReturnType().isPrimitive()) {
                String t = "\t{} ret = (({})classProxy.intercept(methodProxies[{}], originalInstance, args)).{}Value();\n" +
                        "\treturn ret;\n";
                return replace(t,
                        m.getReturnType().getCanonicalName(),
                        AbstractProxyFactory.primitiveWrapMap.get(m.getReturnType()),
                        index,
                        m.getReturnType().getCanonicalName());
            } else {
                String t = "\t{} ret = ({})classProxy.intercept(methodProxies[{}], originalInstance, args);\n" +
                        "\treturn ret;\n";
                return replace(t,
                        m.getReturnType().getCanonicalName(),
                        m.getReturnType().getCanonicalName(),
                        index);
            }
        } else {
            String t = "\tclassProxy.intercept(methodProxies[{}], originalInstance, args);\n";
            return replace(t, index);
        }
    }


    private String buildParameterObjectArray(Class<?>[] parameters) {
        return IntStream.range(0, parameters.length)
                        .boxed()
                        .map(index -> convertTypeToObject(parameters, index))
                        .collect(Collectors.joining(","));
    }

    private String convertTypeToObject(Class<?>[] parameters, Integer index) {
        final Class<?> parameter = parameters[index];
        String objectParam;
        if (parameter.isPrimitive()) {
            String t = "(Object){}.valueOf(arg{})";
            objectParam = replace(t, AbstractProxyFactory.primitiveWrapMap.get(parameters[index]), index);
        } else {
            objectParam = "(Object)arg" + index;
        }
        return objectParam;
    }

    private String buildParameters(Class<?>[] parameters) {
        if (parameters == null || parameters.length == 0) {
            return "";
        }
        return IntStream
                .range(0, parameters.length)
                .boxed()
                .map(index -> parameters[index].getCanonicalName() + " arg" + index)
                .collect(Collectors.joining(","));
    }

    private boolean filterMethods(MethodFilter filter, Method m) {
        return !m.getDeclaringClass().equals(Object.class)
                && !Modifier.isFinal(m.getModifiers())
                && !Modifier.isStatic(m.getModifiers())
                && !Modifier.isNative(m.getModifiers())
                && Optional.ofNullable(filter).map(f -> f.accept(m)).orElse(true);
    }

    private CtClass buildClass(ClassPool classPool, Class<?> clazz) throws NotFoundException, CannotCompileException {
        String className = "com.fireflysource.common.bytecode.ClassProxy" + UUID.randomUUID().toString().replace("-", "");
        CtClass cc = classPool.makeClass(className);
        cc.setSuperclass(classPool.get(clazz.getName()));
        return cc;
    }

    private void buildPrivateFields(Class<?> clazz, CtClass cc) throws CannotCompileException {
        cc.addField(CtField.make("private " + ClassProxy.class.getCanonicalName() + " classProxy;", cc));
        cc.addField(CtField.make("private " + clazz.getCanonicalName() + " originalInstance;", cc));
        cc.addField(CtField.make("private " + MethodProxy[].class.getCanonicalName() + " methodProxies;", cc));
    }

    private void buildConstructor(ClassPool classPool, Class<?> clazz, CtClass cc) throws CannotCompileException, NotFoundException {
        CtConstructor empty = new CtConstructor(null, cc);
        empty.setBody("{}");
        cc.addConstructor(empty);

        CtConstructor constructor = new CtConstructor(new CtClass[]{
                classPool.get(ClassProxy.class.getName()),
                classPool.get(clazz.getName()),
                classPool.get(MethodProxy[].class.getName())
        }, cc);
        String bodyTemplate = "{"
                + "this.classProxy = ({})$1;"
                + "this.originalInstance = ({})$2;"
                + "this.methodProxies = ({})$3;"
                + "}";
        String body = replace(bodyTemplate,
                ClassProxy.class.getCanonicalName(), clazz.getCanonicalName(), MethodProxy[].class.getCanonicalName());
        constructor.setBody(body);
        cc.addConstructor(constructor);
    }
}
