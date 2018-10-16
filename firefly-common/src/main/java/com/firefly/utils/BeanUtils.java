package com.firefly.utils;

import com.firefly.utils.function.Func1;
import com.firefly.utils.function.Func2;
import com.firefly.utils.lang.GenericTypeReference;
import com.firefly.utils.lang.bean.*;

import java.lang.reflect.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import static com.firefly.utils.ReflectUtils.*;

/**
 * @author Pengtao Qiu
 */
abstract public class BeanUtils {

    public static final Type[] EMPTY_TYPE_ARRAY = new Type[0];

    private static final ConcurrentHashMap<String, Map<String, MethodGenericTypeBind>> genericGetterCache = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<String, Map<String, MethodGenericTypeBind>> genericSetterCache = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<String, Map<String, FieldGenericTypeBind>> genericPropertyCache = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<String, Map<String, PropertyAccess>> genericBeanAccessCache = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<Class<?>, Map<String, PropertyAccess>> classBeanAccessCache = new ConcurrentHashMap<>();

    public static boolean isArray(Type type) {
        if (type instanceof Class<?>) {
            return ((Class<?>) type).isArray();
        } else if (type instanceof ParameterizedType) {
            return isArray(((ParameterizedType) type).getRawType());
        } else {
            return type instanceof GenericArrayType;
        }
    }

    public static Class<?> getComponentType(Type type) {
        if (type instanceof Class<?>) {
            return ((Class<?>) type).getComponentType();
        } else if (type instanceof ParameterizedType) {
            return getComponentType(((ParameterizedType) type).getRawType());
        } else if (type instanceof GenericArrayType) {
            return extractGenericArrayClass((GenericArrayType) type);
        } else {
            return null;
        }
    }

    public static Class<?> extractGenericArrayClass(GenericArrayType genericArrayType) {
        Type componentType = genericArrayType.getGenericComponentType();
        if (componentType instanceof Class<?>) {
            return (Class<?>) componentType;
        } else if (componentType instanceof ParameterizedType) {
            return (Class<?>) ((ParameterizedType) componentType).getRawType();
        } else if (componentType instanceof GenericArrayType) {
            return Array.newInstance(extractGenericArrayClass((GenericArrayType) componentType), 0).getClass();
        } else {
            return null;
        }
    }

    public static Class<?> extractClass(Type type) {
        if (isArray(type)) {
            Class<?> componentType = getComponentType(type);
            return Array.newInstance(componentType, 0).getClass();
        } else {
            if (type instanceof Class<?>) {
                return (Class<?>) type;
            } else if (type instanceof ParameterizedType) {
                return extractClass(((ParameterizedType) type).getRawType());
            } else if (type instanceof WildcardType) {
                WildcardType wildcardType = (WildcardType) type;
                if (wildcardType.getUpperBounds() != null && wildcardType.getUpperBounds().length > 0) {
                    return extractClass(wildcardType.getUpperBounds()[0]);
                } else if (wildcardType.getLowerBounds() != null && wildcardType.getLowerBounds().length > 0) {
                    return extractClass(wildcardType.getLowerBounds()[0]);
                } else {
                    return null;
                }
            } else {
                return null;
            }
        }
    }

    public static void copyBean(Object src, Object dest) {
        Map<String, PropertyAccess> srcProperties = getBeanAccess(src.getClass());
        Map<String, PropertyAccess> destProperties = getBeanAccess(dest.getClass());
        srcProperties.forEach((name, srcPropertyAccess) -> {
            PropertyAccess destPropertyAccess = destProperties.get(name);
            if (destPropertyAccess != null) {
                try {
                    Object srcValue = srcPropertyAccess.getValue(src);
                    if (srcValue != null) {
                        destPropertyAccess.setValue(dest, srcValue);
                    }
                } catch (Throwable t) {
                    System.err.println("copy java bean exception " + t.getMessage());
                }
            }
        });
    }

    public static Map<String, PropertyAccess> getBeanAccess(Class<?> clazz) {
        return classBeanAccessCache.computeIfAbsent(clazz, k -> getBeanAccessByType(clazz));
    }

    public static Map<String, PropertyAccess> getBeanAccess(GenericTypeReference genericTypeReference) {
        return genericBeanAccessCache.computeIfAbsent(genericTypeReference.getType().getTypeName(), k -> getBeanAccessByType(genericTypeReference.getType()));
    }

    public static Map<String, PropertyAccess> getBeanAccessByType(Type type) {
        Map<String, FieldGenericTypeBind> fields = getGenericBeanFields(type, null);
        Map<String, MethodGenericTypeBind> getters = getGenericBeanMethods(type, null, ReflectUtils::getGetterMethods, Method::getGenericReturnType);
        Map<String, MethodGenericTypeBind> setters = getGenericBeanMethods(type, null, ReflectUtils::getSetterMethods, method -> method.getGenericParameterTypes()[0]);
        return createPropertyAccessMap(fields, getters, setters);
    }

    private static Map<String, PropertyAccess> createPropertyAccessMap(Map<String, FieldGenericTypeBind> fields,
                                                                       Map<String, MethodGenericTypeBind> getters,
                                                                       Map<String, MethodGenericTypeBind> setters) {
        Set<String> properties = new HashSet<>();
        properties.addAll(fields.keySet());
        properties.addAll(getters.keySet());
        properties.addAll(setters.keySet());

        Map<String, PropertyAccess> map = new HashMap<>();
        properties.forEach(name -> {
            PropertyAccessImpl propertyAccess = new PropertyAccessImpl();
            propertyAccess.setName(name);
            FieldGenericTypeBind fieldGenericTypeBind = fields.get(name);
            if (fieldGenericTypeBind != null) {
                propertyAccess.setField(fieldGenericTypeBind.getField());
                propertyAccess.setFieldProxy(getFieldProxy(fieldGenericTypeBind.getField()));
                propertyAccess.setType(fieldGenericTypeBind.getType());
            }

            MethodGenericTypeBind getter = getters.get(name);
            if (getter != null) {
                propertyAccess.setGetterMethod(getter.getMethod());
                propertyAccess.setGetterMethodProxy(getMethodProxy(getter.getMethod()));
                propertyAccess.setType(getter.getType());
            }

            MethodGenericTypeBind setter = setters.get(name);
            if (setter != null) {
                propertyAccess.setSetterMethod(setter.getMethod());
                propertyAccess.setSetterMethodProxy(getMethodProxy(setter.getMethod()));
                propertyAccess.setType(setter.getType());
            }
            map.put(name, propertyAccess);
        });
        return map;
    }

    public static Map<String, MethodGenericTypeBind> getGenericBeanGetterMethods(GenericTypeReference genericTypeReference) {
        return genericGetterCache.computeIfAbsent(genericTypeReference.getType().getTypeName(), k -> getGenericBeanGetterMethods(genericTypeReference, null));
    }

    public static Map<String, MethodGenericTypeBind> getGenericBeanGetterMethods(GenericTypeReference genericTypeReference,
                                                                                 ReflectUtils.BeanMethodFilter filter) {
        return getGenericBeanMethods(genericTypeReference.getType(), filter,
                ReflectUtils::getGetterMethods,
                Method::getGenericReturnType);
    }

    public static Map<String, MethodGenericTypeBind> getGenericBeanSetterMethods(GenericTypeReference genericTypeReference) {
        return genericSetterCache.computeIfAbsent(genericTypeReference.getType().getTypeName(), k -> getGenericBeanSetterMethods(genericTypeReference, null));
    }

    public static Map<String, MethodGenericTypeBind> getGenericBeanSetterMethods(GenericTypeReference genericTypeReference,
                                                                                 ReflectUtils.BeanMethodFilter filter) {
        return getGenericBeanMethods(genericTypeReference.getType(), filter,
                ReflectUtils::getSetterMethods,
                method -> method.getGenericParameterTypes()[0]);
    }

    private static Map<String, MethodGenericTypeBind> getGenericBeanMethods(Type type,
                                                                            ReflectUtils.BeanMethodFilter filter,
                                                                            Func2<Class<?>, ReflectUtils.BeanMethodFilter, Map<String, Method>> getMethodMap,
                                                                            Func1<Method, Type> getType) {
        Class<?> rawClass = extractClass(type);
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

    public static Map<String, FieldGenericTypeBind> getGenericBeanFields(GenericTypeReference genericTypeReference, ReflectUtils.BeanFieldFilter filter) {
        return getGenericBeanFields(genericTypeReference.getType(), filter);
    }

    private static Map<String, FieldGenericTypeBind> getGenericBeanFields(Type type, ReflectUtils.BeanFieldFilter filter) {
        Class<?> rawClass = extractClass(type);
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

    public static Field getField(String propertyName, Class<?> clazz) {
        Class<?> tmpClass = clazz;
        while (tmpClass != null) {
            try {
                Field field = tmpClass.getDeclaredField(propertyName);
                if (field != null) {
                    field.setAccessible(true);
                    return field;
                }
                tmpClass = tmpClass.getSuperclass();
            } catch (NoSuchFieldException e) {
                tmpClass = tmpClass.getSuperclass();
            }
        }
        return null;
    }
}
