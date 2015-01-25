package com.firefly.utils.json.parser;

import com.firefly.utils.json.JsonReader;
import com.firefly.utils.json.Parser;
import com.firefly.utils.time.SafeSimpleDateFormat;

public class DateParser implements Parser {
	
	private SafeSimpleDateFormat safeSimpleDateFormat = SafeSimpleDateFormat.defaultDateFormat;
	
	public DateParser() {}

	
	public DateParser(String datePattern) {
		this.safeSimpleDateFormat = new SafeSimpleDateFormat(datePattern);
	}

	@Override
	public Object convertTo(JsonReader reader, Class<?> clazz) {
		return safeSimpleDateFormat.parse(reader.readString());
	}

}
