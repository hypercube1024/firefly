package com.firefly.utils;

import com.firefly.utils.classproxy.JavassistReflectionProxyFactory;
import com.firefly.utils.function.Func1;
import com.firefly.utils.function.Func2;
import com.firefly.utils.lang.GenericTypeReference;
import com.firefly.utils.lang.bean.FieldGenericTypeBind;
import com.firefly.utils.lang.bean.GenericArrayTypeImpl;
import com.firefly.utils.lang.bean.MethodGenericTypeBind;
import com.firefly.utils.lang.bean.ParameterizedTypeImpl;

import java.lang.reflect.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public abstract class ReflectUtils {

    public static final Type[] EMPTY_TYPE_ARRAY = new Type[0];

    private static final ConcurrentHashMap<Class<?>, Map<String, Method>> getterCache = new ConcurrentHashMap<>(256);
    private static final ConcurrentHashMap<Class<?>, Map<String, Method>> setterCache = new ConcurrentHashMap<>(256);
    private static final ConcurrentHashMap<Class<?>, Map<String, Field>> propertyCache = new ConcurrentHashMap<>(256);

    private static final ConcurrentHashMap<String, Map<String, MethodGenericTypeBind>> genericGetterCache = new ConcurrentHashMap<>(256);
    private static final ConcurrentHashMap<String, Map<String, MethodGenericTypeBind>> genericSetterCache = new ConcurrentHashMap<>(256);
    private static final ConcurrentHashMap<String, Map<String, FieldGenericTypeBind>> genericPropertyCache = new ConcurrentHashMap<>(256);

    public static ProxyFactory defaultProxyFactory = ServiceUtils.loadService(ProxyFactory.class, JavassistReflectionProxyFactory.INSTANCE);

    public interface BeanMethodFilter {
        boolean accept(String propertyName, Method method);
    }

    public interface BeanFieldFilter {
        boolean accept(String propertyName, Field field);
    }

    public interface MethodProxy {

        Method method();

        /**
         * Executes this method
         *
         * @param obj  The instance of object that contains this method
         * @param args The parameters of this method
         * @return Return value of this method
         */
        Object invoke(Object obj, Object... args);
    }

    public interface FieldProxy {

        Field field();

        Object get(Object obj);

        void set(Object obj, Object value);
    }

    public interface ArrayProxy {
        int size(Object array);

        Object get(Object array, int index);

        void set(Object array, int index, Object value);
    }

    public interface ProxyFactory {
        ArrayProxy getArrayProxy(Class<?> clazz);

        FieldProxy getFieldProxy(Field field);

        MethodProxy getMethodProxy(Method method);
    }

    public static void setProperty(Object obj, String property, Object value) throws Throwable {
        getFields(obj.getClass()).get(property).set(obj, value);
    }

    public static Object getProperty(Object obj, String property) throws Throwable {
        return getFields(obj.getClass()).get(property).get(obj);
    }

    /**
     * Invokes a object's "setter" method by property name
     *
     * @param obj      The instance of a object
     * @param property The property name of this object
     * @param value    The parameter of "setter" method that you want to set
     * @throws Throwable A runtime exception
     */
    public static void set(Object obj, String property, Object value) throws Throwable {
        getSetterMethod(obj.getClass(), property).invoke(obj, value);
    }

    /**
     * Invokes a object's "getter" method by property name
     *
     * @param obj      The instance of a object
     * @param property The property name of this object
     * @return The value of this property
     * @throws Throwable A runtime exception
     */
    public static Object get(Object obj, String property) throws Throwable {
        return getGetterMethod(obj.getClass(), property).invoke(obj);
    }

    public static Object arrayGet(Object array, int index) {
        return getArrayProxy(array.getClass()).get(array, index);
    }

    public static void arraySet(Object array, int index, Object value) {
        getArrayProxy(array.getClass()).set(array, index, value);
    }

    public static int arraySize(Object array) {
        return getArrayProxy(array.getClass()).size(array);
    }

    public static ArrayProxy getArrayProxy(Class<?> clazz) {
        return defaultProxyFactory.getArrayProxy(clazz);
    }

    public static FieldProxy getFieldProxy(Field field) {
        return defaultProxyFactory.getFieldProxy(field);
    }

    public static MethodProxy getMethodProxy(Method method) {
        return defaultProxyFactory.getMethodProxy(method);
    }

    /**
     * Gets the all interface names of this class
     *
     * @param c The class of one object
     * @return Returns the all interface names
     */
    public static String[] getInterfaceNames(Class<?> c) {
        Class<?>[] interfaces = c.getInterfaces();
        List<String> names = new ArrayList<>();
        for (Class<?> i : interfaces) {
            names.add(i.getName());
        }
        return names.toArray(new String[0]);
    }

    public static String getPropertyName(Method method) {
        String methodName = method.getName();
        int index = (methodName.charAt(0) == 'i' ? 2 : 3);
        char c = methodName.charAt(index);
        if (Character.isLowerCase(c)) {
            return methodName.substring(index);
        } else {
            return Character.toLowerCase(methodName.charAt(index)) + methodName.substring(index + 1);
        }
    }

    public static Method getSetterMethod(Class<?> clazz, String propertyName) {
        return getSetterMethods(clazz).get(propertyName);
    }

    public static Map<String, Method> getSetterMethods(Class<?> clazz) {
        return setterCache.computeIfAbsent(clazz, key -> getSetterMethods(key, null));
    }

    public static Map<String, Method> getSetterMethods(Class<?> clazz, BeanMethodFilter filter) {
        Map<String, Method> setMethodMap = new HashMap<>();
        Method[] methods = clazz.getMethods();

        for (Method method : methods) {
            method.setAccessible(true);

            if (Modifier.isStatic(method.getModifiers())) continue;
            if (Modifier.isAbstract(method.getModifiers())) continue;
            if (method.getName().length() < 4) continue;
            if (!method.getName().startsWith("set")) continue;
            if (!method.getReturnType().equals(Void.TYPE)) continue;
            if (method.getParameterTypes().length != 1) continue;

            String propertyName = getPropertyName(method);
            if (filter == null || filter.accept(propertyName, method)) {
                setMethodMap.put(propertyName, method);
            }
        }
        return setMethodMap;
    }

    public static Method getGetterMethod(Class<?> clazz, String propertyName) {
        return getGetterMethods(clazz).get(propertyName);
    }

    public static Map<String, Method> getGetterMethods(Class<?> clazz) {
        return getterCache.computeIfAbsent(clazz, key -> getGetterMethods(key, null));
    }

    public static Map<String, Method> getGetterMethods(Class<?> clazz, BeanMethodFilter filter) {
        Map<String, Method> getMethodMap = new HashMap<>();
        Method[] methods = clazz.getMethods();
        for (Method method : methods) {
            method.setAccessible(true);

            if (Modifier.isStatic(method.getModifiers())) continue;
            if (Modifier.isAbstract(method.getModifiers())) continue;
            if (method.getName().equals("getClass")) continue;
            if (!(method.getName().startsWith("is") || method.getName().startsWith("get"))) continue;
            if (method.getParameterTypes().length != 0) continue;
            if (method.getReturnType() == void.class) continue;

            String methodName = method.getName();
            int index = (methodName.charAt(0) == 'i' ? 2 : 3);
            if (methodName.length() < index + 1) continue;

            String propertyName = getPropertyName(method);
            if (filter == null || filter.accept(propertyName, method)) {
                getMethodMap.put(propertyName, method);
            }
        }
        return getMethodMap;
    }

    public static Map<String, Field> getFields(Class<?> clazz) {
        return propertyCache.computeIfAbsent(clazz, key -> getFields(key, null));
    }

    public static Map<String, Field> getFields(Class<?> clazz, BeanFieldFilter filter) {
        Map<String, Field> fieldMap = new HashMap<>();
        Field[] fields = clazz.getFields();
        for (Field field : fields) {
            field.setAccessible(true);
            if (Modifier.isStatic(field.getModifiers())) continue;

            String propertyName = field.getName();
            if (filter == null || filter.accept(propertyName, field))
                fieldMap.put(propertyName, field);
        }
        return fieldMap;
    }

    public static void copy(Object src, Object dest) {
        Map<String, Method> getterMethodMap = ReflectUtils.getGetterMethods(src.getClass());
        Map<String, Method> setterMethodMap = ReflectUtils.getSetterMethods(dest.getClass());

        for (Map.Entry<String, Method> entry : setterMethodMap.entrySet()) {
            Method getter = getterMethodMap.get(entry.getKey());
            if (getter == null) continue;

            try {
                Object obj = getter.invoke(src);
                if (obj != null) {
                    entry.getValue().invoke(dest, obj);
                }
            } catch (Throwable t) {
                System.err.println("copy object exception, " + t.getMessage());
            }
        }
    }

    public static Map<String, MethodGenericTypeBind> getGenericBeanGetterMethods(GenericTypeReference genericTypeReference) {
        return genericGetterCache.computeIfAbsent(genericTypeReference.getType().getTypeName(), k -> getGenericBeanGetterMethods(genericTypeReference, null));
    }

    public static Map<String, MethodGenericTypeBind> getGenericBeanGetterMethods(GenericTypeReference genericTypeReference,
                                                                                 BeanMethodFilter filter) {
        return getGenericBeanMethods(genericTypeReference.getType(), filter,
                ReflectUtils::getGetterMethods,
                Method::getGenericReturnType);
    }

    public static Map<String, MethodGenericTypeBind> getGenericBeanSetterMethods(GenericTypeReference genericTypeReference) {
        return genericSetterCache.computeIfAbsent(genericTypeReference.getType().getTypeName(), k -> getGenericBeanSetterMethods(genericTypeReference, null));
    }

    public static Map<String, MethodGenericTypeBind> getGenericBeanSetterMethods(GenericTypeReference genericTypeReference,
                                                                                 BeanMethodFilter filter) {
        return getGenericBeanMethods(genericTypeReference.getType(), filter,
                ReflectUtils::getSetterMethods,
                method -> method.getGenericParameterTypes()[0]);
    }

    private static Map<String, MethodGenericTypeBind> getGenericBeanMethods(Type type,
                                                                           BeanMethodFilter filter,
                                                                           Func2<Class<?>, BeanMethodFilter, Map<String, Method>> getMethodMap,
                                                                           Func1<Method, Type> getType) {
        Class<?> rawClass = type instanceof ParameterizedType
                ? (Class<?>) ((ParameterizedType) type).getRawType()
                : (Class<?>) type;
        Map<String, Method> methodMap = getMethodMap.call(rawClass, filter);
        if (methodMap == null || methodMap.isEmpty()) {
            return Collections.emptyMap();
        } else {
            Map<String, MethodGenericTypeBind> genericTypeBindMap = new HashMap<>();
            if (type instanceof ParameterizedType) {
                Map<String, Type> genericNameTypeMap = createGenericNameTypeMap((ParameterizedType) type);
                methodMap.forEach((name, method) -> bindMethodType(name, method, getBindType(getType.call(method), genericNameTypeMap), genericTypeBindMap));
                return genericTypeBindMap;
            } else {
                methodMap.forEach((name, method) -> bindMethodType(name, method, getType.call(method), genericTypeBindMap));
                return genericTypeBindMap;
            }
        }
    }

    private static void bindMethodType(String name, Method method, Type type, Map<String, MethodGenericTypeBind> genericTypeBindMap) {
        MethodGenericTypeBind bind = new MethodGenericTypeBind();
        bind.setMethod(method);
        bind.setType(type);
        genericTypeBindMap.put(name, bind);
    }

    public static Map<String, FieldGenericTypeBind> getGenericBeanFields(GenericTypeReference genericTypeReference) {
        return genericPropertyCache.computeIfAbsent(genericTypeReference.getType().getTypeName(), k -> getGenericBeanFields(genericTypeReference, null));
    }

    public static Map<String, FieldGenericTypeBind> getGenericBeanFields(GenericTypeReference genericTypeReference, BeanFieldFilter filter) {
        return getGenericBeanFields(genericTypeReference.getType(), filter);
    }

    private static Map<String, FieldGenericTypeBind> getGenericBeanFields(Type type, BeanFieldFilter filter) {
        Class<?> rawClass = type instanceof ParameterizedType
                ? (Class<?>) ((ParameterizedType) type).getRawType()
                : (Class<?>) type;
        Map<String, Field> fieldMap = getFields(rawClass, filter);
        if (fieldMap == null || fieldMap.isEmpty()) {
            return Collections.emptyMap();
        } else {
            Map<String, FieldGenericTypeBind> genericTypeBindMap = new HashMap<>();
            if (type instanceof ParameterizedType) {
                Map<String, Type> genericNameTypeMap = createGenericNameTypeMap((ParameterizedType) type);
                fieldMap.forEach((name, field) -> bindFieldType(name, field, getBindType(field.getGenericType(), genericNameTypeMap), genericTypeBindMap));
                return genericTypeBindMap;
            } else {
                fieldMap.forEach((name, field) -> bindFieldType(name, field, field.getGenericType(), genericTypeBindMap));
                return genericTypeBindMap;
            }
        }
    }

    private static void bindFieldType(String name, Field field, Type type, Map<String, FieldGenericTypeBind> genericTypeBindMap) {
        FieldGenericTypeBind bind = new FieldGenericTypeBind();
        bind.setField(field);
        bind.setType(type);
        genericTypeBindMap.put(name, bind);
    }

    private static Type getBindType(Type erasureType, Map<String, Type> genericNameTypeMap) {
        Type replacedType;
        if (erasureType instanceof ParameterizedType) {
            replacedType = convertParameterizedType((ParameterizedType) erasureType, genericNameTypeMap);
        } else if (erasureType instanceof TypeVariable) {
            replacedType = genericNameTypeMap.get(((TypeVariable) erasureType).getName());
        } else if (erasureType instanceof GenericArrayType) {
            replacedType = convertGenericArrayType((GenericArrayType) erasureType, genericNameTypeMap);
        } else {
            replacedType = erasureType;
        }
        return replacedType;
    }

    private static GenericArrayTypeImpl convertGenericArrayType(GenericArrayType genericArrayType, Map<String, Type> genericNameTypeMap) {
        GenericArrayTypeImpl replacedType = new GenericArrayTypeImpl();
        replacedType.setGenericComponentType(getBindType(genericArrayType.getGenericComponentType(), genericNameTypeMap));
        return replacedType;
    }

    private static ParameterizedTypeImpl convertParameterizedType(ParameterizedType parameterizedType, Map<String, Type> genericNameTypeMap) {
        ParameterizedTypeImpl replacedType = new ParameterizedTypeImpl();
        replacedType.setActualTypeArguments(Arrays.stream(parameterizedType.getActualTypeArguments())
                                                  .map(actualType -> getBindType(actualType, genericNameTypeMap))
                                                  .collect(Collectors.toList())
                                                  .toArray(EMPTY_TYPE_ARRAY));
        replacedType.setRawType(parameterizedType.getRawType());
        replacedType.setOwnerType(parameterizedType.getOwnerType());
        return replacedType;
    }

    private static Map<String, Type> createGenericNameTypeMap(ParameterizedType parameterizedType) {
        List<Type> types = new ArrayList<>();
        types.add(parameterizedType);
        for (Type ownType = parameterizedType.getOwnerType(); ownType != null; ) {
            types.add(ownType);
            if (ownType instanceof ParameterizedType) {
                ownType = ((ParameterizedType) ownType).getOwnerType();
            } else {
                break;
            }
        }
        Collections.reverse(types);

        Map<String, Type> genericNameTypeMap = new HashMap<>();
        types.stream()
             .filter(type -> type instanceof ParameterizedType)
             .map(type -> (ParameterizedType) type)
             .forEach(paramType -> _buildGenericNameTypeMap((Class<?>) paramType.getRawType(), paramType, genericNameTypeMap));
        return genericNameTypeMap;
    }

    private static void _buildGenericNameTypeMap(Class<?> rawType, ParameterizedType parameterizedType, Map<String, Type> genericNameTypeMap) {
        TypeVariable[] typeVariables = rawType.getTypeParameters();
        Type[] types = parameterizedType.getActualTypeArguments();
        for (int i = 0; i < typeVariables.length; i++) {
            genericNameTypeMap.put(typeVariables[i].getName(), types[i]);
        }
    }

}
