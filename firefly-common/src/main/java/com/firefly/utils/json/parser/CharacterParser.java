package com.firefly.utils.json.parser;

import com.firefly.utils.json.Parser;
import com.firefly.utils.json.support.JsonStringReader;

public class CharacterParser implements Parser {

	@Override
	public Object convertTo(JsonStringReader reader, Class<?> clazz) {
		char ret = 0;
		String s = reader.readString();
		
		if(s.length() > 0)
			ret = s.charAt(0);
		return ret;
	}

}
