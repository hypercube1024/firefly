package com.fireflysource.common.bytecode;

import com.fireflysource.common.string.StringUtils;
import javassist.*;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.UUID;

/**
 * @author Pengtao Qiu
 */
public class JavassistReflectionProxyFactory extends AbstractProxyFactory {

    public static final JavassistReflectionProxyFactory INSTANCE = new JavassistReflectionProxyFactory();

    private JavassistReflectionProxyFactory() {

    }

    protected ArrayProxy createArrayProxy(Class<?> clazz) {
        try {
            ClassPool classPool = ClassPool.getDefault();
            classPool.insertClassPath(new ClassClassPath(ArrayProxy.class));

            CtClass cc = classPool.makeClass("com.firefly.utils.ArrayField" + UUID.randomUUID().toString().replace("-", ""));
            cc.addInterface(classPool.get(ArrayProxy.class.getName()));

            cc.addMethod(CtMethod.make(createArraySizeCode(clazz), cc));
            cc.addMethod(CtMethod.make(createArrayGetCode(clazz), cc));
            cc.addMethod(CtMethod.make(createArraySetCode(clazz), cc));

            return (ArrayProxy) cc.toClass(classLoader, null).getConstructor().newInstance();
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    private String createArraySetCode(Class<?> clazz) {
        StringBuilder code = new StringBuilder();
        code.append("public void set(Object array, int index, Object value){\n")
            .append(StringUtils.replace("\t(({})array)[index] = ", clazz.getCanonicalName()));

        Class<?> componentType = clazz.getComponentType();
        if (componentType.isPrimitive()) {
            code.append(StringUtils.replace("(({})value).{}Value()", primitiveWrapMap.get(componentType), componentType.getCanonicalName()));
        } else {
            code.append(StringUtils.replace("({})value", componentType.getCanonicalName()));
        }

        code.append(";\n")
            .append("}");
        return code.toString();
    }

    private String createArrayGetCode(Class<?> clazz) {
        StringBuilder code = new StringBuilder();
        code.append("public Object get(Object array, int index){\n")
            .append("\treturn ");
        Class<?> componentType = clazz.getComponentType();
        boolean hasValueOf = false;
        if (componentType.isPrimitive()) {
            code.append(StringUtils.replace("(Object){}.valueOf(", primitiveWrapMap.get(componentType)));
            hasValueOf = true;
        }

        code.append(StringUtils.replace("(({})array)[index]", clazz.getCanonicalName()));
        if (hasValueOf)
            code.append(")");

        code.append(";\n")
            .append("}");
        return code.toString();
    }

    private String createArraySizeCode(Class<?> clazz) {
        StringBuilder code = new StringBuilder();
        code.append("public int size(Object array){\n")
            .append("\treturn ").append(StringUtils.replace("(({})array).length;\n", clazz.getCanonicalName()))
            .append("}");
        return code.toString();
    }

    protected FieldProxy createFieldProxy(Field field) {
        try {
            ClassPool classPool = ClassPool.getDefault();
            classPool.insertClassPath(new ClassClassPath(FieldProxy.class));

            CtClass cc = classPool.makeClass("com.firefly.utils.ProxyField" + UUID.randomUUID().toString().replace("-", ""));
            cc.addInterface(classPool.get(FieldProxy.class.getName()));
            cc.addField(CtField.make("private java.lang.reflect.Field field;", cc));

            CtConstructor constructor = new CtConstructor(new CtClass[]{classPool.get(Field.class.getName())}, cc);
            constructor.setBody("{this.field = (java.lang.reflect.Field)$1;}");
            cc.addConstructor(constructor);

            cc.addMethod(CtMethod.make("public java.lang.reflect.Field field(){return field;}", cc));
            cc.addMethod(CtMethod.make(createFieldGetterMethodCode(field), cc));
            cc.addMethod(CtMethod.make(createFieldSetterMethodCode(field), cc));

            return (FieldProxy) cc.toClass(classLoader, null).getConstructor(Field.class).newInstance(field);
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    private String createFieldGetterMethodCode(Field field) {
        Class<?> fieldClazz = field.getType();
        StringBuilder code = new StringBuilder();
        code.append("public Object get(Object obj){\n")
            .append("\treturn ");

        boolean hasValueOf = false;
        if (fieldClazz.isPrimitive()) {
            code.append(StringUtils.replace("(Object){}.valueOf(", primitiveWrapMap.get(fieldClazz)));
            hasValueOf = true;
        }
        code.append(StringUtils.replace("(({})obj).{}", field.getDeclaringClass().getCanonicalName(), field.getName()));
        if (hasValueOf)
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

        if (fieldClazz.isPrimitive()) {
            code.append(StringUtils.replace("(({})value).{}Value()", primitiveWrapMap.get(fieldClazz), fieldClazz.getCanonicalName()));
        } else {
            code.append(StringUtils.replace("({})value", fieldClazz.getCanonicalName()));
        }
        code.append(";\n")
            .append("}");
        return code.toString();
    }

    protected MethodProxy createMethodProxy(Method method) {
        try {
            ClassPool classPool = ClassPool.getDefault();
            classPool.insertClassPath(new ClassClassPath(MethodProxy.class));

            CtClass cc = classPool.makeClass("com.firefly.utils.ProxyMethod" + UUID.randomUUID().toString().replace("-", ""));

            cc.addInterface(classPool.get(MethodProxy.class.getName()));
            cc.addField(CtField.make("private java.lang.reflect.Method method;", cc));

            CtConstructor constructor = new CtConstructor(new CtClass[]{classPool.get(Method.class.getName())}, cc);
            constructor.setBody("{this.method = (java.lang.reflect.Method)$1;}");
            cc.addConstructor(constructor);

            cc.addMethod(CtMethod.make("public java.lang.reflect.Method method(){return method;}", cc));
            cc.addMethod(CtMethod.make(createInvokeMethodCode(method), cc));

            return (MethodProxy) cc.toClass(classLoader, null).getConstructor(Method.class).newInstance(method);
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    private String createInvokeMethodCode(Method method) {
        Class<?>[] paramClazz = method.getParameterTypes();
        StringBuilder code = new StringBuilder();

        code.append("public Object invoke(Object obj, Object[] args){\n ");
        if (paramClazz.length > 0)
            code.append('\t')
                .append(StringUtils.replace("if(args == null || args.length != {})", paramClazz.length))
                .append("\n\t\t")
                .append("throw new IllegalArgumentException(\"arguments error\");\n")
                .append('\n');

        boolean hasValueOf = false;
        code.append('\t');
        if (!method.getReturnType().equals(Void.TYPE)) {
            code.append("return ");
            if (method.getReturnType().isPrimitive()) {
                code.append(StringUtils.replace("(Object){}.valueOf(", primitiveWrapMap.get(method.getReturnType())));
                hasValueOf = true;
            }
        }

        if (java.lang.reflect.Modifier.isStatic(method.getModifiers()))
            code.append(method.getDeclaringClass().getCanonicalName());
        else
            code.append(StringUtils.replace("(({})obj)", method.getDeclaringClass().getCanonicalName()));

        code.append('.').append(method.getName()).append('(');
        if (paramClazz.length > 0) {
            int max = paramClazz.length - 1;
            for (int i = 0; ; i++) {
                Class<?> param = paramClazz[i];
                if (param.isPrimitive()) {
                    code.append(StringUtils.replace("(({})args[{}]).{}Value()", primitiveWrapMap.get(param), i, param.getCanonicalName()));
                } else {
                    code.append(StringUtils.replace("({})args[{}]", param.getCanonicalName(), i));
                }

                if (i == max) {
                    break;
                }
                code.append(", ");
            }
        }
        if (hasValueOf) {
            code.append(")");
        }
        code.append(");\n");

        if (method.getReturnType().equals(Void.TYPE)) {
            code.append("\treturn null;\n");
        }
        code.append('}');

        return code.toString();
    }
}
