package com.firefly.utils.json.serializer;

import com.firefly.utils.json.JsonWriter;
import com.firefly.utils.json.Serializer;

public class LongSerializer implements Serializer {

	@Override
	public void convertTo(JsonWriter writer, Object obj) {
		writer.writeLong((Long)obj);
	}

}
