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

            CtClass cc = classPool.makeClass("com.fireflysource.common.bytecode.ArrayField" + UUID.randomUUID().toString().replace("-", ""));
            cc.addInterface(classPool.get(ArrayProxy.class.getName()));

            cc.addMethod(CtMethod.make(createArraySizeCode(clazz), cc));
            cc.addMethod(CtMethod.make(createArrayGetCode(clazz), cc));
            cc.addMethod(CtMethod.make(createArraySetCode(clazz), cc));

            return (ArrayProxy) cc.toClass(this.getClass()).getConstructor().newInstance();
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    private String createArraySetCode(Class<?> clazz) {
        StringBuilder code = new StringBuilder("public void set(Object array, int index, Object value){\n");

        Class<?> componentType = clazz.getComponentType();
        if (componentType.isPrimitive()) {
            String t = "\t(({})array)[index] = (({})value).{}Value();\n";
            code.append(StringUtils.replace(t,
                    clazz.getCanonicalName(),
                    primitiveWrapMap.get(componentType),
                    componentType.getCanonicalName()));
        } else {
            String t = "\t(({})array)[index] = ({})value;\n";
            code.append(StringUtils.replace(t, clazz.getCanonicalName(), componentType.getCanonicalName()));
        }

        code.append("}");
        return code.toString();
    }

    private String createArrayGetCode(Class<?> clazz) {
        StringBuilder code = new StringBuilder("public Object get(Object array, int index){\n");

        Class<?> componentType = clazz.getComponentType();
        if (componentType.isPrimitive()) {
            String t = "\treturn (Object){}.valueOf((({})array)[index]);\n";
            code.append(StringUtils.replace(t, primitiveWrapMap.get(componentType), clazz.getCanonicalName()));
        } else {
            String t = "\treturn (({})array)[index];\n";
            code.append(StringUtils.replace(t, clazz.getCanonicalName()));
        }

        code.append("}");
        return code.toString();
    }

    private String createArraySizeCode(Class<?> clazz) {
        String t = "public int size(Object array){\n" +
                "\treturn (({})array).length;\n" +
                "}";
        return StringUtils.replace(t, clazz.getCanonicalName());
    }

    protected FieldProxy createFieldProxy(Field field) {
        try {
            ClassPool classPool = ClassPool.getDefault();
            classPool.insertClassPath(new ClassClassPath(FieldProxy.class));

            CtClass cc = classPool.makeClass("com.fireflysource.common.bytecode.ProxyField" + UUID.randomUUID().toString().replace("-", ""));
            cc.addInterface(classPool.get(FieldProxy.class.getName()));
            cc.addField(CtField.make("private java.lang.reflect.Field field;", cc));

            CtConstructor constructor = new CtConstructor(new CtClass[]{classPool.get(Field.class.getName())}, cc);
            constructor.setBody("{this.field = (java.lang.reflect.Field)$1;}");
            cc.addConstructor(constructor);

            cc.addMethod(CtMethod.make("public java.lang.reflect.Field field(){return field;}", cc));
            cc.addMethod(CtMethod.make(createFieldGetterMethodCode(field), cc));
            cc.addMethod(CtMethod.make(createFieldSetterMethodCode(field), cc));

            return (FieldProxy) cc.toClass(this.getClass()).getConstructor(Field.class).newInstance(field);
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    private String createFieldGetterMethodCode(Field field) {
        Class<?> fieldClazz = field.getType();
        StringBuilder code = new StringBuilder("public Object get(Object obj){\n");

        if (fieldClazz.isPrimitive()) {
            String t = "\treturn (Object){}.valueOf( (({})obj).{} );\n";
            code.append(StringUtils.replace(t,
                    primitiveWrapMap.get(fieldClazz),
                    field.getDeclaringClass().getCanonicalName(),
                    field.getName()));
        } else {
            String t = "\treturn (({})obj).{};\n";
            code.append(StringUtils.replace(t,
                    field.getDeclaringClass().getCanonicalName(),
                    field.getName()));
        }

        code.append("}");
        return code.toString();
    }

    private String createFieldSetterMethodCode(Field field) {
        Class<?> fieldClazz = field.getType();
        StringBuilder code = new StringBuilder("public void set(Object obj, Object value){\n");

        if (fieldClazz.isPrimitive()) {
            String t = "\t(({})obj).{} = (({})value).{}Value();\n";
            code.append(StringUtils.replace(t,
                    field.getDeclaringClass().getCanonicalName(), field.getName(),
                    primitiveWrapMap.get(fieldClazz), fieldClazz.getCanonicalName()));
        } else {
            String t = "\t(({})obj).{} = ({})value;\n";
            code.append(StringUtils.replace(t,
                    field.getDeclaringClass().getCanonicalName(), field.getName(),
                    fieldClazz.getCanonicalName()));
        }

        code.append("}");
        return code.toString();
    }

    protected MethodProxy createMethodProxy(Method method) {
        try {
            ClassPool classPool = ClassPool.getDefault();
            classPool.insertClassPath(new ClassClassPath(MethodProxy.class));

            CtClass cc = classPool.makeClass("com.fireflysource.common.bytecode.ProxyMethod" + UUID.randomUUID().toString().replace("-", ""));

            cc.addInterface(classPool.get(MethodProxy.class.getName()));
            cc.addField(CtField.make("private java.lang.reflect.Method method;", cc));

            CtConstructor constructor = new CtConstructor(new CtClass[]{classPool.get(Method.class.getName())}, cc);
            constructor.setBody("{this.method = (java.lang.reflect.Method)$1;}");
            cc.addConstructor(constructor);

            cc.addMethod(CtMethod.make("public java.lang.reflect.Method method(){return method;}", cc));
            cc.addMethod(CtMethod.make(createInvokeMethodCode(method), cc));

            return (MethodProxy) cc.toClass(this.getClass()).getConstructor(Method.class).newInstance(method);
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    private String createInvokeMethodCode(Method method) {
        Class<?>[] paramClazz = method.getParameterTypes();
        StringBuilder code = new StringBuilder();

        code.append("public Object invoke(Object obj, Object[] args){\n ");
        if (paramClazz.length > 0) {
            String t = "\tif(args == null || args.length != {})\n" +
                    "\t\tthrow new IllegalArgumentException(\"arguments error\");\n\n";
            code.append(StringUtils.replace(t, paramClazz.length));
        }
        if (method.getReturnType().equals(Void.TYPE)) {
            code.append('\t').append(createMethodCall(method)).append(";\n")
                .append("\treturn null;\n");
        } else {
            code.append("\treturn ");
            if (method.getReturnType().isPrimitive()) {
                code.append(StringUtils.replace("(Object){}.valueOf(", primitiveWrapMap.get(method.getReturnType())))
                    .append(createMethodCall(method))
                    .append(");\n");
            } else {
                code.append(createMethodCall(method)).append(";\n");
            }
        }

        code.append('}');
        return code.toString();
    }

    private String createMethodCall(Method method) {
        Class<?>[] paramClazz = method.getParameterTypes();
        StringBuilder code = new StringBuilder();

        if (java.lang.reflect.Modifier.isStatic(method.getModifiers())) {
            code.append(method.getDeclaringClass().getCanonicalName());
        } else {
            code.append(StringUtils.replace("(({})obj)", method.getDeclaringClass().getCanonicalName()));
        }

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
        code.append(')');
        return code.toString();
    }
}
