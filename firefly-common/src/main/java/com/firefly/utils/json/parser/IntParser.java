package com.firefly.utils.json.parser;

import com.firefly.utils.json.JsonReader;
import com.firefly.utils.json.Parser;

public class IntParser implements Parser {

	@Override
	public Object convertTo(JsonReader reader, Class<?> clazz) {
		return reader.readInt();
	}

}
