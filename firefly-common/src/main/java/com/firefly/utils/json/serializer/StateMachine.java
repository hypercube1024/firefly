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

abstract public class StateMachine {
	private static final IdentityHashMap<Class<?>, Serializer> map = new IdentityHashMap<Class<?>, Serializer>();

	private static final Serializer MAP = new MapSerializer();
	private static final Serializer COLLECTION = new CollectionSerializer();
	private static final Serializer ARRAY = new ArraySerializer();
	private static final Serializer ENUM = new EnumSerializer();
	private static final DynamicObjectSerializer DYNAMIC = new DynamicObjectSerializer();

	static {
		map.put(long.class, new LongSerializer());
		map.put(int.class, new IntSerializer());
		map.put(char.class, new CharacterSerializer());
		map.put(short.class, new ShortSerializer());
		map.put(byte.class, new ByteSerializer());
		map.put(boolean.class, new BoolSerializer());
		map.put(String.class, new StringSerializer());
		map.put(Date.class, new DateSerializer());
		map.put(double.class, new StringValueSerializer());
		map.put(long[].class, new LongArraySerializer());
		map.put(int[].class, new IntegerArraySerializer());
		map.put(short[].class, new ShortArraySerializer());
		map.put(boolean[].class, new BooleanArraySerializer());
		map.put(String[].class, new StringArraySerializer());

		map.put(Long.class, map.get(long.class));
		map.put(Integer.class, map.get(int.class));
		map.put(Character.class, map.get(char.class));
		map.put(Short.class, map.get(short.class));
		map.put(Byte.class, map.get(byte.class));
		map.put(Boolean.class, map.get(boolean.class));
		map.put(Long[].class, map.get(long[].class));
		map.put(Integer[].class, map.get(int[].class));
		map.put(Short[].class, map.get(short[].class));
		map.put(Boolean[].class, map.get(Boolean[].class));

		map.put(StringBuilder.class, map.get(String.class));
		map.put(StringBuffer.class, map.get(String.class));

		map.put(java.sql.Date.class, map.get(Date.class));
		map.put(java.sql.Time.class, map.get(Date.class));
		map.put(java.sql.Timestamp.class, map.get(Date.class));

		map.put(Double.class, map.get(double.class));
		map.put(float.class, map.get(double.class));
		map.put(Float.class, map.get(double.class));
		map.put(AtomicInteger.class, map.get(double.class));
		map.put(AtomicLong.class, map.get(double.class));
		map.put(BigDecimal.class, map.get(double.class));
		map.put(BigInteger.class, map.get(double.class));
		map.put(AtomicBoolean.class, map.get(double.class));
	}

	public static Serializer getSerializer(Class<?> clazz) {
		Serializer ret = map.get(clazz);
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
			map.put(clazz, ret);
		}
		return ret;
	}
	
	public static Serializer getSerializerInCompiling(Class<?> clazz) {
		Serializer ret = map.get(clazz);
		if (ret == null || ret instanceof ObjectSerializer || ret instanceof ObjectNoCheckSerializer) {
			if (clazz.isEnum()) {
				ret = ENUM;
				map.put(clazz, ret);
			} else if (Map.class.isAssignableFrom(clazz)) {
				ret = MAP;
				map.put(clazz, ret);
			} else if (Collection.class.isAssignableFrom(clazz)) {
				ret = COLLECTION;
				map.put(clazz, ret);
			} else if (clazz.isArray()) {
				ret = ARRAY;
				map.put(clazz, ret);
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
