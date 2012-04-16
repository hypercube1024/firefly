package com.firefly.utils.json;

import java.io.IOException;

import com.firefly.utils.json.parser.ParserStateMachine;
import com.firefly.utils.json.serializer.SerialStateMachine;
import com.firefly.utils.json.support.JsonStringReader;
import com.firefly.utils.json.support.JsonStringWriter;


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
		JsonStringReader reader = new JsonStringReader(json);
		return (T) ParserStateMachine.toObject(reader, clazz);
	}
}
