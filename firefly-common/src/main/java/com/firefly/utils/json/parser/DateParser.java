package com.firefly.utils.json.parser;

import com.firefly.utils.json.JsonReader;
import com.firefly.utils.json.Parser;
import com.firefly.utils.time.SafeSimpleDateFormat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Date;

public class DateParser implements Parser {

	private SafeSimpleDateFormat safeSimpleDateFormat = SafeSimpleDateFormat.defaultDateFormat;

	public DateParser() {
	}

	public DateParser(String datePattern) {
		this.safeSimpleDateFormat = new SafeSimpleDateFormat(datePattern);
	}

	@Override
	public Object convertTo(JsonReader reader, Class<?> clazz) {
		if (reader.isNull())
			return null;

		try {
			reader.mark(1024);
			if (reader.isString()) {
				reader.reset();
				String s = reader.readString();
				return safeSimpleDateFormat.parse(s);
			} else {
				reader.reset();
				long timestamp = reader.readLong();
				return new Date(timestamp);
			}
		} catch (Exception e) {
			return null;
		}
	}

}
