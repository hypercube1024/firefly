package com.firefly.utils.json;

import java.io.IOException;

import com.firefly.utils.json.serializer.StateMachine;
import com.firefly.utils.json.support.JsonStringWriter;


public abstract class Json {
	public static String toJson(Object obj) {
		String ret = null;
		JsonStringWriter writer = new JsonStringWriter();
		try {
			StateMachine.toJson(obj, writer);
			ret = writer.toString();
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			writer.close();
		}
		return ret;
	}
}
