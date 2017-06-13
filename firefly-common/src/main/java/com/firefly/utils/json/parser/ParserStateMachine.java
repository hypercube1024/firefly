package com.firefly.utils.json.parser;

import com.firefly.utils.json.JsonReader;
import com.firefly.utils.json.Parser;
import com.firefly.utils.json.annotation.DateFormat;
import com.firefly.utils.json.exception.JsonException;

import java.io.IOException;
import java.lang.reflect.GenericArrayType;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class ParserStateMachine {

    private static final Lock lock = new ReentrantLock();
    private static final TimestampParser TIMESTAMP = new TimestampParser();
    private static final Map<String, Parser> PARSER_MAP = new HashMap<>();

    static {
        PARSER_MAP.put(int.class.getTypeName(), new IntParser());
        PARSER_MAP.put(long.class.getTypeName(), new LongParser());
        PARSER_MAP.put(short.class.getTypeName(), new ShortParser());
        PARSER_MAP.put(float.class.getTypeName(), new FloatParser());
        PARSER_MAP.put(double.class.getTypeName(), new DoubleParser());
        PARSER_MAP.put(boolean.class.getTypeName(), new BooleanParser());
        PARSER_MAP.put(char.class.getTypeName(), new CharacterParser());

        PARSER_MAP.put(Integer.class.getTypeName(), new IntParser());
        PARSER_MAP.put(Long.class.getTypeName(), new LongParser());
        PARSER_MAP.put(Short.class.getTypeName(), new ShortParser());
        PARSER_MAP.put(Float.class.getTypeName(), new FloatParser());
        PARSER_MAP.put(Double.class.getTypeName(), new DoubleParser());
        PARSER_MAP.put(Boolean.class.getTypeName(), new BooleanParser());
        PARSER_MAP.put(Character.class.getTypeName(), new CharacterParser());

        PARSER_MAP.put(BigDecimal.class.getTypeName(), new BigDecimalParser());
        PARSER_MAP.put(BigInteger.class.getTypeName(), new BigIntegerParser());

        PARSER_MAP.put(String.class.getTypeName(), new StringParser());
        PARSER_MAP.put(Date.class.getTypeName(), new DateParser());

        PARSER_MAP.put(int[].class.getTypeName(), new ArrayParser(int.class));
        PARSER_MAP.put(long[].class.getTypeName(), new ArrayParser(long.class));
        PARSER_MAP.put(short[].class.getTypeName(), new ArrayParser(short.class));
        PARSER_MAP.put(float[].class.getTypeName(), new ArrayParser(float.class));
        PARSER_MAP.put(double[].class.getTypeName(), new ArrayParser(double.class));
        PARSER_MAP.put(boolean[].class.getTypeName(), new ArrayParser(boolean.class));
        PARSER_MAP.put(byte[].class.getTypeName(), new ByteArrayParser());
        PARSER_MAP.put(char[].class.getTypeName(), new CharArrayParser());

        PARSER_MAP.put(Integer[].class.getTypeName(), new ArrayParser(Integer.class));
        PARSER_MAP.put(Long[].class.getTypeName(), new ArrayParser(Long.class));
        PARSER_MAP.put(Short[].class.getTypeName(), new ArrayParser(Short.class));
        PARSER_MAP.put(Float[].class.getTypeName(), new ArrayParser(Float.class));
        PARSER_MAP.put(Double[].class.getTypeName(), new ArrayParser(Double.class));
        PARSER_MAP.put(Boolean[].class.getTypeName(), new ArrayParser(Boolean.class));

        PARSER_MAP.put(String[].class.getTypeName(), new ArrayParser(String.class));
    }

    public static Parser getParser(Class<?> clazz, Type type, DateFormat dateFormat) {
        lock.lock();
        try {
            if (dateFormat != null && clazz == Date.class) {
                return getTimeParser(clazz, dateFormat);
            } else {
                Parser ret = PARSER_MAP.get(type.getTypeName());
                if (ret == null) {
                    if (clazz.isEnum()) {
                        ret = getEnumParser(clazz);
                    } else if (Collection.class.isAssignableFrom(clazz)) {
                        ret = getCollectionParser(type);
                    } else if (Map.class.isAssignableFrom(clazz)) {
                        ret = getMapParser(type);
                    } else if (clazz.isArray()) {
                        ret = getArrayParser(clazz, type);
                    } else {
                        ret = getObjectParser(clazz, type);
                    }
                }
                return ret;
            }
        } finally {
            lock.unlock();
        }
    }

    private static Parser getEnumParser(Class<?> clazz) {
        Parser ret;
        ret = new EnumParser(clazz);
        PARSER_MAP.put(clazz.getTypeName(), ret);
        return ret;
    }

    private static Parser getObjectParser(Class<?> clazz, Type type) {
        Parser ret;
        ObjectParser objectParser = new ObjectParser();
        PARSER_MAP.put(type.getTypeName(), objectParser);
        ret = objectParser;
        objectParser.init(clazz, type);
        return ret;
    }

    private static Parser getArrayParser(Class<?> clazz, Type type) {
        Parser ret;
        Class<?> elementClass = clazz.getComponentType();
        ArrayParser arrayParser = new ArrayParser();
        PARSER_MAP.put(type.getTypeName(), arrayParser);
        ret = arrayParser;
        if (type instanceof GenericArrayType) {
            arrayParser.init(elementClass, ((GenericArrayType) type).getGenericComponentType());
        } else if (type instanceof Class<?>) {
            arrayParser.init(elementClass, elementClass);
        }
        return ret;
    }

    private static Parser getMapParser(Type type) {
        Parser ret;
        ParameterizedType parameterizedType = (ParameterizedType) type;
        MapParser mapParser = new MapParser();
        PARSER_MAP.put(type.getTypeName(), mapParser);
        ret = mapParser;
        mapParser.init(parameterizedType.getActualTypeArguments()[1]);
        return ret;
    }

    private static Parser getCollectionParser(Type type) {
        Parser ret;
        ParameterizedType parameterizedType = (ParameterizedType) type;
        CollectionParser collectionParser = new CollectionParser();
        PARSER_MAP.put(type.getTypeName(), collectionParser);
        ret = collectionParser;
        collectionParser.init(parameterizedType.getActualTypeArguments()[0]);
        return ret;
    }

    private static Parser getTimeParser(Class<?> clazz, DateFormat dateFormat) {
        if (clazz == Date.class) {
            if (dateFormat == null) {
                return PARSER_MAP.get(Date.class.getTypeName());
            } else switch (dateFormat.type()) {
                case DATE_PATTERN_STRING:
                    return new DateParser(dateFormat.value());
                case TIMESTAMP:
                    return TIMESTAMP;
            }
        }
        throw new JsonException("not support date type -> " + clazz.getTypeName());
    }

    @SuppressWarnings("unchecked")
    public static <T> T toObject(JsonReader reader, Class<?> clazz, Type type) throws IOException {
        Parser parser = getParser(clazz, type, null);
        return (T) parser.convertTo(reader, clazz);
    }

}
