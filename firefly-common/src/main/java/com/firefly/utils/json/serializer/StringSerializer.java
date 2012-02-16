package com.firefly.utils.json.serializer;

import com.firefly.utils.json.Serializer;
import com.firefly.utils.json.support.JsonStringWriter;

public class StringSerializer implements Serializer {

	@Override
	public void convertTo(JsonStringWriter writer, Object obj) {
//		System.out.println("string");
		writer.writeStringWithQuote(obj.toString());
	}

}
