package com.firefly.utils.json.serializer;

import java.io.IOException;

import com.firefly.utils.json.Serializer;
import com.firefly.utils.json.support.JsonStringWriter;

public class StringArraySerializer implements Serializer {

	@Override
	public void convertTo(JsonStringWriter writer, Object obj)
			throws IOException {
		String[] object = (String[])obj;
		writer.writeStringArray(object);
	}

}
