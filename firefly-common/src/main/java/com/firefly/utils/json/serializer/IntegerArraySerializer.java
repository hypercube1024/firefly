package com.firefly.utils.json.serializer;

import java.io.IOException;

import com.firefly.utils.json.JsonWriter;

public class IntegerArraySerializer extends SimpleArraySerializer{

	public IntegerArraySerializer(boolean primitive) {
		super(primitive);
	}

	@Override
	public void convertTo(JsonWriter writer, Object obj) throws IOException {
		if(primitive) {
			writer.writeIntArray((int[])obj);
		} else {
			writer.writeIntArray((Integer[])obj);
		}
	}

}
