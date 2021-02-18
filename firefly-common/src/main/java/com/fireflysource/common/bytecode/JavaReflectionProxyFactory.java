package com.fireflysource.common.bytecode;

import com.fireflysource.common.string.StringUtils;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.UUID;

/**
 * @author Pengtao Qiu
 */
public class JavaReflectionProxyFactory extends AbstractProxyFactory {

    public static final JavaReflectionProxyFactory INSTANCE = new JavaReflectionProxyFactory();

    private JavaReflectionProxyFactory() {

    }

    protected FieldProxy createFieldProxy(Field field) {
        try {
            String packageName = "com.firefly.utils";
            String className = "FieldReflectionProxy" + UUID.randomUUID().toString().replace("-", "");
            String completeClassName = packageName + "." + className;

            String value = "";
            Class<?> fieldClazz = field.getType();
            if (fieldClazz.isPrimitive()) {
                value += StringUtils.replace("(({})value).{}Value()", primitiveWrapMap.get(fieldClazz), fieldClazz.getCanonicalName());
            } else {
                value += StringUtils.replace("({})value", fieldClazz.getCanonicalName());
            }
            String source = "package " + packageName + ";\n"
                    + "import " + Field.class.getCanonicalName() + ";\n"
                    + "public class " + className + " implements " + FieldProxy.class.getCanonicalName() + " {\n"
                    + "private Field field;\n"
                    + "public " + className + "(Field field){\n"
                    + "\tthis.field = field;\n"
                    + "}\n\n"
                    + "public Field field(){return field;}\n"
                    + "public Object get(Object obj){\n"
                    + "\treturn " + StringUtils.replace("(({})obj).{};\n", field.getDeclaringClass().getCanonicalName(), field.getName())
                    + "}\n\n"

                    + "public void set(Object obj, Object value){\n"
                    + StringUtils.replace("\t(({})obj).{} = ", field.getDeclaringClass().getCanonicalName(), field.getName())
                    + value + ";\n"
                    + "}\n"
                    + "}";

            Class<?> fieldProxyClass = CompilerUtils.compileSource(completeClassName, source);
            if (fieldProxyClass == null) {
                return null;
            } else {
                return (FieldProxy) fieldProxyClass.getConstructor(Field.class).newInstance(field);
            }
        } catch (Throwable e) {
            throw new IllegalStateException(e);
        }
    }

    protected MethodProxy createMethodProxy(Method method) {
        try {
            String packageName = "com.firefly.utils";
            String className = "MethodReflectionProxy" + UUID.randomUUID().toString().replace("-", "");
            String completeClassName = packageName + "." + className;

            Class<?>[] paramClazz = method.getParameterTypes();
            String returnCode = "";
            if (!method.getReturnType().equals(Void.TYPE)) {
                returnCode += "return ";
            }

            returnCode += "((" + method.getDeclaringClass().getCanonicalName() + ")obj)." + method.getName() + "(";
            if (paramClazz.length > 0) {
                int max = paramClazz.length - 1;
                for (int i = 0; ; i++) {
                    Class<?> param = paramClazz[i];
                    if (param.isPrimitive()) {
                        returnCode += StringUtils.replace("(({})args[{}]).{}Value()", primitiveWrapMap.get(param), i, param.getCanonicalName());
                    } else {
                        returnCode += "(" + param.getCanonicalName() + ")args[" + i + "]";
                    }

                    if (i == max)
                        break;

                    returnCode += ",";
                }
            }
            returnCode += ");";

            if (method.getReturnType().equals(Void.TYPE)) {
                returnCode += "\n\treturn null;";
            }

            String source = "package " + packageName + ";\n"
                    + "import " + Method.class.getCanonicalName() + ";\n"
                    + "public class " + className + " implements " + MethodProxy.class.getCanonicalName() + " {\n"
                    + "private Method method;\n"
                    + "public " + className + "(Method method){\n"
                    + "\tthis.method = method;\n"
                    + "}\n\n"
                    + "public Method method(){return method;}\n\n"
                    + "public Object invoke(Object obj, Object[] args){\n"
                    + "\tif(args == null || args.length != " + paramClazz.length + ")\n"
                    + "\t\tthrow new IllegalArgumentException(\"arguments error\");\n\n"
                    + "\t" + returnCode + "\n"
                    + "}\n"
                    + "}";

            Class<?> methodProxyClass = CompilerUtils.compileSource(completeClassName, source);
            if (methodProxyClass == null)
                return null;

            return (MethodProxy) methodProxyClass.getConstructor(Method.class).newInstance(method);
        } catch (Throwable e) {
            throw new IllegalStateException(e);
        }
    }

    protected ArrayProxy createArrayProxy(Class<?> clazz) {
        try {
            String packageName = "com.firefly.utils";
            String className = "ArrayReflectionProxy" + UUID.randomUUID().toString().replace("-", "");
            String completeClassName = packageName + "." + className;

            Class<?> componentType = clazz.getComponentType();
            String v;
            if (componentType.isPrimitive()) {
                v = StringUtils.replace("(({})value).{}Value()", primitiveWrapMap.get(componentType), componentType.getCanonicalName());
            } else {
                v = "(" + clazz.getComponentType().getCanonicalName() + ")value;\n";
            }

            String source = "package " + packageName + ";\n"
                    + "public class " + className + " implements " + ArrayProxy.class.getCanonicalName() + " {\n"
                    + "@Override\n"
                    + "public int size(Object array){\n"
                    + "\treturn ((" + clazz.getCanonicalName() + ")array).length;\n"
                    + "}\n\n"

                    + "@Override\n"
                    + "public Object get(Object array, int index){\n"
                    + "\treturn ((" + clazz.getCanonicalName() + ")array)[index];\n"
                    + "}\n\n"

                    + "@Override\n"
                    + "public void set(Object array, int index, Object value){\n"
                    + "\t((" + clazz.getCanonicalName() + ")array)[index] = " + v + ";"
                    + "}\n\n"
                    + "}";

            Class<?> arrayProxyClazz = CompilerUtils.compileSource(completeClassName, source);
            if (arrayProxyClazz == null)
                return null;

            return (ArrayProxy) arrayProxyClazz.getConstructor().newInstance();
        } catch (Throwable e) {
            throw new IllegalStateException(e);
        }
    }
}
