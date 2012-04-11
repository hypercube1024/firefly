package com.firefly.utils.json.serializer;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Collection;
import java.util.Date;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import com.firefly.utils.collection.IdentityHashMap;
import com.firefly.utils.json.Serializer;
import com.firefly.utils.json.annotation.CircularReferenceCheck;
import com.firefly.utils.json.support.JsonStringWriter;

abstract public class SerialStateMachine {
	private static final IdentityHashMap<Class<?>, Serializer> SERIAL_MAP = new IdentityHashMap<Class<?>, Serializer>();

	private static final Serializer MAP = new MapSerializer();
	private static final Serializer COLLECTION = new CollectionSerializer();
	private static final Serializer ARRAY = new ArraySerializer();
	private static final Serializer ENUM = new EnumSerializer();
	private static final DynamicObjectSerializer DYNAMIC = new DynamicObjectSerializer();

	static {
		SERIAL_MAP.put(long.class, new LongSerializer());
		SERIAL_MAP.put(int.class, new IntSerializer());
		SERIAL_MAP.put(char.class, new CharacterSerializer());
		SERIAL_MAP.put(short.class, new ShortSerializer());
		SERIAL_MAP.put(byte.class, new ByteSerializer());
		SERIAL_MAP.put(boolean.class, new BoolSerializer());
		SERIAL_MAP.put(String.class, new StringSerializer());
		SERIAL_MAP.put(Date.class, new DateSerializer());
		SERIAL_MAP.put(double.class, new StringValueSerializer());
		SERIAL_MAP.put(long[].class, new LongArraySerializer());
		SERIAL_MAP.put(int[].class, new IntegerArraySerializer());
		SERIAL_MAP.put(short[].class, new ShortArraySerializer());
		SERIAL_MAP.put(boolean[].class, new BooleanArraySerializer());
		SERIAL_MAP.put(String[].class, new StringArraySerializer());

		SERIAL_MAP.put(Long.class, SERIAL_MAP.get(long.class));
		SERIAL_MAP.put(Integer.class, SERIAL_MAP.get(int.class));
		SERIAL_MAP.put(Character.class, SERIAL_MAP.get(char.class));
		SERIAL_MAP.put(Short.class, SERIAL_MAP.get(short.class));
		SERIAL_MAP.put(Byte.class, SERIAL_MAP.get(byte.class));
		SERIAL_MAP.put(Boolean.class, SERIAL_MAP.get(boolean.class));
		SERIAL_MAP.put(Long[].class, SERIAL_MAP.get(long[].class));
		SERIAL_MAP.put(Integer[].class, SERIAL_MAP.get(int[].class));
		SERIAL_MAP.put(Short[].class, SERIAL_MAP.get(short[].class));
		SERIAL_MAP.put(Boolean[].class, SERIAL_MAP.get(Boolean[].class));

		SERIAL_MAP.put(StringBuilder.class, SERIAL_MAP.get(String.class));
		SERIAL_MAP.put(StringBuffer.class, SERIAL_MAP.get(String.class));

		SERIAL_MAP.put(java.sql.Date.class, SERIAL_MAP.get(Date.class));
		SERIAL_MAP.put(java.sql.Time.class, SERIAL_MAP.get(Date.class));
		SERIAL_MAP.put(java.sql.Timestamp.class, SERIAL_MAP.get(Date.class));

		SERIAL_MAP.put(Double.class, SERIAL_MAP.get(double.class));
		SERIAL_MAP.put(float.class, SERIAL_MAP.get(double.class));
		SERIAL_MAP.put(Float.class, SERIAL_MAP.get(double.class));
		SERIAL_MAP.put(AtomicInteger.class, SERIAL_MAP.get(double.class));
		SERIAL_MAP.put(AtomicLong.class, SERIAL_MAP.get(double.class));
		SERIAL_MAP.put(BigDecimal.class, SERIAL_MAP.get(double.class));
		SERIAL_MAP.put(BigInteger.class, SERIAL_MAP.get(double.class));
		SERIAL_MAP.put(AtomicBoolean.class, SERIAL_MAP.get(double.class));
	}

	public static Serializer getSerializer(Class<?> clazz) {
		Serializer ret = SERIAL_MAP.get(clazz);
		if (ret == null) {
			if (clazz.isEnum())
				ret = ENUM;
			else if (Map.class.isAssignableFrom(clazz))
				ret = MAP;
			else if (Collection.class.isAssignableFrom(clazz))
				ret = COLLECTION;
			else if (clazz.isArray())
				ret = ARRAY;
			else
				ret = clazz.isAnnotationPresent(CircularReferenceCheck.class) ? new ObjectSerializer(
						clazz) : new ObjectNoCheckSerializer(clazz);
			SERIAL_MAP.put(clazz, ret);
		}
		return ret;
	}
	
	public static Serializer getSerializerInCompiling(Class<?> clazz) {
		Serializer ret = SERIAL_MAP.get(clazz);
		if (ret == null || ret instanceof ObjectSerializer || ret instanceof ObjectNoCheckSerializer) {
			if (clazz.isEnum()) {
				ret = ENUM;
				SERIAL_MAP.put(clazz, ret);
			} else if (Map.class.isAssignableFrom(clazz)) {
				ret = MAP;
				SERIAL_MAP.put(clazz, ret);
			} else if (Collection.class.isAssignableFrom(clazz)) {
				ret = COLLECTION;
				SERIAL_MAP.put(clazz, ret);
			} else if (clazz.isArray()) {
				ret = ARRAY;
				SERIAL_MAP.put(clazz, ret);
			} else
				ret = DYNAMIC;
		}
		return ret;
	}

	public static void toJson(Object obj, JsonStringWriter writer)
			throws IOException {
		if (obj == null) {
			writer.writeNull();
			return;
		}

		Class<?> clazz = obj.getClass();
		Serializer serializer = getSerializer(clazz);
		serializer.convertTo(writer, obj);
	}
}
