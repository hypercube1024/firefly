package com.firefly.utils.json.serializer;

import com.firefly.utils.collection.IdentityHashMap;
import com.firefly.utils.concurrent.ReentrantLocker;
import com.firefly.utils.function.Func1;
import com.firefly.utils.json.JsonWriter;
import com.firefly.utils.json.Serializer;
import com.firefly.utils.json.annotation.DateFormat;
import com.firefly.utils.json.exception.JsonException;
import com.firefly.utils.json.support.ClassType;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Date;
import java.util.EnumMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

abstract public class SerialStateMachine {

    private static final IdentityHashMap<Class<?>, Serializer> SERIAL_MAP = new IdentityHashMap<>();
    private static final EnumMap<ClassType, Func1<Class<?>, Serializer>> CLASS_TYPE_SERIALIZER_MAP = new EnumMap<>(ClassType.class);
    private static final ReentrantLocker lock = new ReentrantLocker();

    private static final Serializer MAP = new MapSerializer();
    private static final Serializer COLLECTION = new CollectionSerializer();
    private static final Serializer ARRAY = new ArraySerializer();
    private static final DynamicObjectSerializer DYNAMIC = new DynamicObjectSerializer();
    private static final StringValueSerializer STRING_VALUE = new StringValueSerializer();
    private static final TimestampSerializer TIMESTAMP = new TimestampSerializer();
    private static final DateSerializer DATE_SERIALIZER = new DateSerializer();

    static {
        SERIAL_MAP.put(long.class, new LongSerializer());
        SERIAL_MAP.put(int.class, new IntSerializer());
        SERIAL_MAP.put(char.class, new CharacterSerializer());
        SERIAL_MAP.put(short.class, new ShortSerializer());
        SERIAL_MAP.put(byte.class, new ByteSerializer());
        SERIAL_MAP.put(boolean.class, new BoolSerializer());
        SERIAL_MAP.put(String.class, new StringSerializer());
        SERIAL_MAP.put(Date.class, DATE_SERIALIZER);
        SERIAL_MAP.put(double.class, STRING_VALUE);
        SERIAL_MAP.put(long[].class, new LongArraySerializer(true));
        SERIAL_MAP.put(int[].class, new IntegerArraySerializer(true));
        SERIAL_MAP.put(short[].class, new ShortArraySerializer(true));
        SERIAL_MAP.put(boolean[].class, new BooleanArraySerializer(true));
        SERIAL_MAP.put(String[].class, new StringArraySerializer());
        SERIAL_MAP.put(byte[].class, new ByteArraySerializer());
        SERIAL_MAP.put(char[].class, new CharArraySerializer());

        SERIAL_MAP.put(Long.class, SERIAL_MAP.get(long.class));
        SERIAL_MAP.put(Integer.class, SERIAL_MAP.get(int.class));
        SERIAL_MAP.put(Character.class, SERIAL_MAP.get(char.class));
        SERIAL_MAP.put(Short.class, SERIAL_MAP.get(short.class));
        SERIAL_MAP.put(Byte.class, SERIAL_MAP.get(byte.class));
        SERIAL_MAP.put(Boolean.class, SERIAL_MAP.get(boolean.class));
        SERIAL_MAP.put(Long[].class, new LongArraySerializer(false));
        SERIAL_MAP.put(Integer[].class, new IntegerArraySerializer(false));
        SERIAL_MAP.put(Short[].class, new ShortArraySerializer(false));
        SERIAL_MAP.put(Boolean[].class, new BooleanArraySerializer(false));

        SERIAL_MAP.put(StringBuilder.class, SERIAL_MAP.get(String.class));
        SERIAL_MAP.put(StringBuffer.class, SERIAL_MAP.get(String.class));

        SERIAL_MAP.put(java.sql.Date.class, SERIAL_MAP.get(Date.class));
        SERIAL_MAP.put(java.sql.Time.class, SERIAL_MAP.get(Date.class));
        SERIAL_MAP.put(java.sql.Timestamp.class, SERIAL_MAP.get(Date.class));

        SERIAL_MAP.put(Double.class, STRING_VALUE);
        SERIAL_MAP.put(float.class, STRING_VALUE);
        SERIAL_MAP.put(Float.class, STRING_VALUE);
        SERIAL_MAP.put(AtomicInteger.class, STRING_VALUE);
        SERIAL_MAP.put(AtomicLong.class, STRING_VALUE);
        SERIAL_MAP.put(BigDecimal.class, STRING_VALUE);
        SERIAL_MAP.put(BigInteger.class, STRING_VALUE);
        SERIAL_MAP.put(AtomicBoolean.class, STRING_VALUE);

        SERIAL_MAP.put(Object.class, DYNAMIC);

        CLASS_TYPE_SERIALIZER_MAP.put(ClassType.ENUM, SerialStateMachine::createEnumSerializer);
        CLASS_TYPE_SERIALIZER_MAP.put(ClassType.MAP, SerialStateMachine::createMapSerializer);
        CLASS_TYPE_SERIALIZER_MAP.put(ClassType.COLLECTION, SerialStateMachine::createCollectionSerializer);
        CLASS_TYPE_SERIALIZER_MAP.put(ClassType.ARRAY, SerialStateMachine::createArraySerializer);
        CLASS_TYPE_SERIALIZER_MAP.put(ClassType.OBJECT, SerialStateMachine::createObjectSerializer);
        CLASS_TYPE_SERIALIZER_MAP.put(ClassType.NO_CHECK_OBJECT, SerialStateMachine::createObjectNoCheckSerializer);
    }

    public static Serializer getSerializer(Class<?> clazz, DateFormat dateFormat) {
        if (dateFormat != null && (clazz == Date.class || Date.class.isAssignableFrom(clazz))) {
            return createTimeSerializer(clazz, dateFormat);
        } else {
            return lock.lock(() -> {
                Serializer ret = SERIAL_MAP.get(clazz);
                if (ret == null) {
                    ret = createSerializer(clazz);
                }
                return ret;
            });
        }
    }

    private static Serializer createSerializer(Class<?> clazz) {
        Func1<Class<?>, Serializer> func1 = CLASS_TYPE_SERIALIZER_MAP.get(ClassType.getClassType(clazz));
        if (func1 != null) {
            return func1.call(clazz);
        } else {
            return null;
        }
    }

    private static Serializer createMapSerializer(Class<?> clazz) {
        Serializer serializer = MAP;
        SERIAL_MAP.put(clazz, serializer);
        return serializer;
    }

    private static Serializer createCollectionSerializer(Class<?> clazz) {
        Serializer serializer = COLLECTION;
        SERIAL_MAP.put(clazz, serializer);
        return serializer;
    }

    private static Serializer createArraySerializer(Class<?> clazz) {
        Serializer serializer = ARRAY;
        SERIAL_MAP.put(clazz, serializer);
        return serializer;
    }

    private static Serializer createEnumSerializer(Class<?> clazz) {
        EnumSerializer enumSerializer = new EnumSerializer(clazz);
        SERIAL_MAP.put(clazz, enumSerializer);
        return enumSerializer;
    }

    private static Serializer createObjectSerializer(Class<?> clazz) {
        ObjectSerializer objectSerializer = new ObjectSerializer();
        SERIAL_MAP.put(clazz, objectSerializer);
        objectSerializer.init(clazz);
        return objectSerializer;
    }

    private static Serializer createObjectNoCheckSerializer(Class<?> clazz) {
        ObjectNoCheckSerializer objectNoCheckSerializer = new ObjectNoCheckSerializer();
        SERIAL_MAP.put(clazz, objectNoCheckSerializer);
        objectNoCheckSerializer.init(clazz);
        return objectNoCheckSerializer;
    }

    private static Serializer createTimeSerializer(Class<?> clazz, DateFormat dateFormat) {
        if ((clazz == Date.class || Date.class.isAssignableFrom(clazz))) {
            if (dateFormat == null) {
                return DATE_SERIALIZER;
            } else switch (dateFormat.type()) {
                case DATE_PATTERN_STRING:
                    return new DateSerializer(dateFormat.value());
                case TIMESTAMP:
                    return TIMESTAMP;
            }
        }
        throw new JsonException("not support date type -> " + clazz.getTypeName());
    }

    public static void toJson(Object obj, JsonWriter writer) throws IOException {
        if (obj == null) {
            writer.writeNull();
            return;
        }

        Class<?> clazz = obj.getClass();
        Serializer serializer = getSerializer(clazz, null);
        serializer.convertTo(writer, obj);
    }
}
