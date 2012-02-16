package com.firefly.utils.json.serializer;

import com.firefly.utils.json.Serializer;
import com.firefly.utils.json.support.JsonStringWriter;

public class StringValueSerializer implements Serializer {

	@Override
	public void convertTo(JsonStringWriter writer, Object obj) {
		writer.write(String.valueOf(obj));
	}

}
