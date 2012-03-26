package com.firefly.utils.json;

import com.firefly.utils.json.support.JsonStringReader;

public interface Parser {
	void convertToObj(JsonStringReader reader, Object obj);
	
	Object convertToSimple(JsonStringReader reader);
}
