package com.firefly.utils.json.parser;

import com.firefly.utils.json.Parser;
import com.firefly.utils.json.exception.JsonException;
import com.firefly.utils.json.support.ParserMetaInfo;

import java.lang.reflect.GenericArrayType;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.*;

import static com.firefly.utils.BeanUtils.extractGenericArrayClass;

public abstract class ComplexTypeParser implements Parser {

    protected ParserMetaInfo elementMetaInfo;

    protected ComplexTypeParser(Type elementType) {
        if (elementType instanceof ParameterizedType) {
            ParameterizedType pt = (ParameterizedType) elementType;
            Class<?> rawClass = (Class<?>) (pt.getRawType());
            elementMetaInfo = new ParserMetaInfo();

            if (Collection.class.isAssignableFrom(rawClass)) {
                Type[] types2 = pt.getActualTypeArguments();
                if (types2.length != 1)
                    throw new JsonException("collection actual type args length not equals 1");

                Type eleType = types2[0];
                elementMetaInfo.setType(getImplClass(rawClass));
                elementMetaInfo.setParser(new CollectionParser(eleType));
            } else if (Map.class.isAssignableFrom(rawClass)) {
                Type[] types2 = pt.getActualTypeArguments();
                if (types2.length != 2)
                    throw new JsonException("map actual type args length not equals 2");

                Type key = types2[0];
                if (!((key instanceof Class) && key == String.class))
                    throw new JsonException("map key type not string");

                Type eleType = types2[1];
                elementMetaInfo.setType(getImplClass(rawClass));
                elementMetaInfo.setParser(new MapParser(eleType));
            } else {
                elementMetaInfo.setType(rawClass);
                elementMetaInfo.setParser(ParserStateMachine.getParser(rawClass, null));
            }
        } else if (elementType instanceof Class) {
            Class<?> eleClass = (Class<?>) elementType;
            elementMetaInfo = new ParserMetaInfo();
            elementMetaInfo.setType(eleClass);
            elementMetaInfo.setParser(ParserStateMachine.getParser(eleClass, null));
        } else if (elementType instanceof GenericArrayType) {
            Class<?> rawClass = extractGenericArrayClass((GenericArrayType) elementType);
            elementMetaInfo = new ParserMetaInfo();
            elementMetaInfo.setType(rawClass);
            elementMetaInfo.setParser(ParserStateMachine.getParser(rawClass, null));
        } else {
            throw new JsonException("mot support type " + elementType);
        }
    }

    public ParserMetaInfo getElementMetaInfo() {
        return elementMetaInfo;
    }

    public static Class<?> getImplClass(Class<?> clazz) {
        if (clazz.isInterface() || Modifier.isAbstract(clazz.getModifiers())) {
            if (Collection.class.isAssignableFrom(clazz)) {
                Class<?> ret = ArrayList.class;
                if (List.class.isAssignableFrom(clazz))
                    ret = ArrayList.class;
                else if (Queue.class.isAssignableFrom(clazz) || Deque.class.isAssignableFrom(clazz))
                    ret = LinkedList.class;
                else if (SortedSet.class.isAssignableFrom(clazz))
                    ret = TreeSet.class;
                else if (Set.class.isAssignableFrom(clazz))
                    ret = HashSet.class;
                return ret;
            } else if (Map.class.isAssignableFrom(clazz)) {
                Class<?> ret = HashMap.class;
                if (SortedMap.class.isAssignableFrom(clazz))
                    ret = TreeMap.class;
                return ret;
            }
            throw new JsonException("not support the type " + clazz);
        } else
            return clazz;
    }

}
