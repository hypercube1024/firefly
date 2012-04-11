package com.firefly.utils.json.parser;

import com.firefly.utils.json.Parser;
import com.firefly.utils.json.support.JsonStringReader;

public class DoubleParser implements Parser {

	@Override
	public Object convertTo(JsonStringReader reader, Class<?> clazz) {
		return reader.readDouble();
	}

}
