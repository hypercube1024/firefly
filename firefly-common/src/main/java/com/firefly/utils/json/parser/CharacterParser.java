package com.firefly.utils.json.parser;

import com.firefly.utils.json.JsonReader;
import com.firefly.utils.json.Parser;

public class CharacterParser implements Parser {

	@Override
	public Object convertTo(JsonReader reader, Class<?> clazz) {
		char ret = 0;
		String s = reader.readString();
		
		if(s.length() > 0)
			ret = s.charAt(0);
		return ret;
	}

}
