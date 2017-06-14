package com.firefly.utils.json.parser;

import com.firefly.utils.concurrent.ReentrantLocker;
import com.firefly.utils.function.Func2;
import com.firefly.utils.json.JsonReader;
import com.firefly.utils.json.Parser;
import com.firefly.utils.json.annotation.DateFormat;
import com.firefly.utils.json.exception.JsonException;
import com.firefly.utils.json.support.ClassType;

import java.io.IOException;
import java.lang.reflect.GenericArrayType;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Date;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;

public class ParserStateMachine {

    private static final ReentrantLocker lock = new ReentrantLocker();
    private static final DateParser DATE_PARSER = new DateParser();
    private static final TimestampParser TIMESTAMP = new TimestampParser();
    private static final Map<String, Parser> PARSER_MAP = new HashMap<>();
    private static final EnumMap<ClassType, Func2<Class<?>, Type, Parser>> CLASS_TYPE_PARSER_MAP = new EnumMap<>(ClassType.class);

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
        PARSER_MAP.put(Date.class.getTypeName(), DATE_PARSER);

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

        CLASS_TYPE_PARSER_MAP.put(ClassType.ENUM, ParserStateMachine::createEnumParser);
        CLASS_TYPE_PARSER_MAP.put(ClassType.MAP, ParserStateMachine::createMapParser);
        CLASS_TYPE_PARSER_MAP.put(ClassType.COLLECTION, ParserStateMachine::createCollectionParser);
        CLASS_TYPE_PARSER_MAP.put(ClassType.ARRAY, ParserStateMachine::createArrayParser);
        CLASS_TYPE_PARSER_MAP.put(ClassType.OBJECT, ParserStateMachine::createObjectParser);
        CLASS_TYPE_PARSER_MAP.put(ClassType.NO_CHECK_OBJECT, ParserStateMachine::createObjectParser);
    }

    public static Parser getParser(Class<?> clazz, Type type, DateFormat dateFormat) {
        if (dateFormat != null && clazz == Date.class) {
            return createTimeParser(clazz, dateFormat);
        } else {
            return lock.lock(() -> {
                Parser ret = PARSER_MAP.get(type.getTypeName());
                if (ret == null) {
                    ret = createParser(clazz, type);
                }
                return ret;
            });
        }
    }

    private static Parser createParser(Class<?> clazz, Type type) {
        Func2<Class<?>, Type, Parser> func2 = CLASS_TYPE_PARSER_MAP.get(ClassType.getClassType(clazz));
        if (func2 != null) {
            return func2.call(clazz, type);
        } else {
            return null;
        }
    }

    private static Parser createEnumParser(Class<?> clazz, Type type) {
        Parser ret = new EnumParser(clazz);
        PARSER_MAP.put(clazz.getTypeName(), ret);
        return ret;
    }

    private static Parser createObjectParser(Class<?> clazz, Type type) {
        ObjectParser objectParser = new ObjectParser();
        PARSER_MAP.put(type.getTypeName(), objectParser);
        objectParser.init(clazz, type);
        return objectParser;
    }

    private static Parser createArrayParser(Class<?> clazz, Type type) {
        Class<?> elementClass = clazz.getComponentType();
        ArrayParser arrayParser = new ArrayParser();
        PARSER_MAP.put(type.getTypeName(), arrayParser);
        if (type instanceof GenericArrayType) {
            arrayParser.init(elementClass, ((GenericArrayType) type).getGenericComponentType());
        } else if (type instanceof Class<?>) {
            arrayParser.init(elementClass, elementClass);
        }
        return arrayParser;
    }

    private static Parser createMapParser(Class<?> clazz, Type type) {
        ParameterizedType parameterizedType = (ParameterizedType) type;
        MapParser mapParser = new MapParser();
        PARSER_MAP.put(type.getTypeName(), mapParser);
        mapParser.init(parameterizedType.getActualTypeArguments()[1]);
        return mapParser;
    }

    private static Parser createCollectionParser(Class<?> clazz, Type type) {
        ParameterizedType parameterizedType = (ParameterizedType) type;
        CollectionParser collectionParser = new CollectionParser();
        PARSER_MAP.put(type.getTypeName(), collectionParser);
        collectionParser.init(parameterizedType.getActualTypeArguments()[0]);
        return collectionParser;
    }

    private static Parser createTimeParser(Class<?> clazz, DateFormat dateFormat) {
        if (clazz == Date.class) {
            if (dateFormat == null) {
                return DATE_PARSER;
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
