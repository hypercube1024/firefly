package com.firefly.utils.json.parser;

import com.firefly.utils.json.JsonReader;
import com.firefly.utils.json.Parser;
import com.firefly.utils.time.SafeSimpleDateFormat;

public class DateParser implements Parser {

	@Override
	public Object convertTo(JsonReader reader, Class<?> clazz) {
		return SafeSimpleDateFormat.defaultDateFormat.parse(reader.readString());
	}

}
