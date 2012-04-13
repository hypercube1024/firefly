package com.firefly.utils.json.parser;

import com.firefly.utils.codec.Base64;
import com.firefly.utils.json.Parser;
import com.firefly.utils.json.support.JsonStringReader;

public class ByteArrayParser implements Parser {

	@Override
	public Object convertTo(JsonStringReader reader, Class<?> clazz) {
		return Base64.decode(reader.readString());
	}

}
