package com.firefly.utils.json.parser;

import com.firefly.utils.json.Parser;
import com.firefly.utils.json.support.JsonStringReader;
import com.firefly.utils.time.SafeSimpleDateFormat;

public class DateParser implements Parser {

	@Override
	public Object convertTo(JsonStringReader reader, Class<?> clazz) {
		return SafeSimpleDateFormat.defaultDateFormat.parse(reader.readString());
	}

}
