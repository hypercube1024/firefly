package com.firefly.utils.json.serializer;

import com.firefly.utils.json.JsonWriter;
import com.firefly.utils.json.Serializer;

public class IntSerializer implements Serializer {

	@Override
	public void convertTo(JsonWriter writer, Object obj) {
		writer.writeInt((Integer)obj);
	}

}
