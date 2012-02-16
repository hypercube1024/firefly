package com.firefly.utils.json.serializer;

import java.io.IOException;

import com.firefly.utils.json.Serializer;
import com.firefly.utils.json.support.JsonStringWriter;

public class BooleanArraySerializer implements Serializer {

	@Override
	public void convertTo(JsonStringWriter writer, Object obj)
			throws IOException {
		if(obj instanceof boolean[]) {
			writer.writeBooleanArray((boolean[])obj);
		} else {
			writer.writeBooleanArray((Boolean[])obj);
		}

	}

}
