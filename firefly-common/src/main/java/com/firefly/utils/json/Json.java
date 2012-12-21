package com.firefly.utils.json;

import java.io.IOException;

import com.firefly.utils.json.io.JsonStringReader;
import com.firefly.utils.json.io.JsonStringWriter;
import com.firefly.utils.json.parser.ParserStateMachine;
import com.firefly.utils.json.serializer.SerialStateMachine;


public abstract class Json {
	public static String toJson(Object obj) {
		String ret = null;
		JsonStringWriter writer = new JsonStringWriter();
		try {
			SerialStateMachine.toJson(obj, writer);
			ret = writer.toString();
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			writer.close();
		}
		return ret;
	}
	
	@SuppressWarnings("unchecked")
	public static <T> T toObject(String json, Class<T> clazz) {
		JsonReader reader = new JsonStringReader(json);
		return (T) ParserStateMachine.toObject(reader, clazz);
	}
}
