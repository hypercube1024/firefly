package com.firefly.utils.json;

import java.io.IOException;

import com.firefly.utils.json.io.JsonStringReader;
import com.firefly.utils.json.io.JsonStringWriter;
import com.firefly.utils.json.parser.GeneralJSONObjectStateMacine;
import com.firefly.utils.json.parser.ParserStateMachine;
import com.firefly.utils.json.serializer.SerialStateMachine;


public abstract class Json {
	public static String toJson(Object obj) {
		String ret = null;
		JsonWriter writer = null;
		try {
			writer = new JsonStringWriter();
			SerialStateMachine.toJson(obj, writer);
			ret = writer.toString();
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			if(writer != null)
				try {
					writer.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
		}
		return ret;
	}
	
	@SuppressWarnings("unchecked")
	public static <T> T toObject(String json, Class<T> clazz) {
		JsonReader reader = null;
		T ret = null;
		try {
			reader = new JsonStringReader(json);
			ret = (T) ParserStateMachine.toObject(reader, clazz);
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			if(reader != null)
				try {
					reader.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
		}
		return ret;
	}
	
	public static JsonObject toJsonObject(String json) {
		JsonReader reader = null;
		JsonObject ret = null;
		try {
			reader = new JsonStringReader(json);
			ret = GeneralJSONObjectStateMacine.toJsonObject(reader);
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			if(reader != null)
				try {
					reader.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
		}
		return ret;
	}
	
	public static JsonArray toJsonArray(String json) {
		JsonReader reader = null;
		JsonArray ret = null;
		try {
			reader = new JsonStringReader(json);
			ret = GeneralJSONObjectStateMacine.toJsonArray(reader);
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			if(reader != null)
				try {
					reader.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
		}
		return ret;
	}
}
