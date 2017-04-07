package com.firefly.utils.classproxy;

import com.firefly.utils.CompilerUtils;
import com.firefly.utils.ReflectUtils.FieldProxy;
import com.firefly.utils.StringUtils;

import java.lang.reflect.Field;
import java.util.UUID;

public class FieldProxyFactoryUsingJavaCompiler extends AbstractFieldProxyFactory {

    public static final FieldProxyFactoryUsingJavaCompiler INSTANCE = new FieldProxyFactoryUsingJavaCompiler();

    private FieldProxyFactoryUsingJavaCompiler() {
    }

    protected FieldProxy _getFieldProxy(Field field) throws Throwable {
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
    }

}
