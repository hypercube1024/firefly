package com.firefly.utils.json.serializer;

import java.io.IOException;
import java.util.Date;

import com.firefly.utils.json.JsonWriter;
import com.firefly.utils.json.Serializer;

public class TimestampSerializer implements Serializer {

	@Override
	public void convertTo(JsonWriter writer, Object obj) throws IOException {
		writer.writeLong(((Date) obj).getTime());
	}

}
