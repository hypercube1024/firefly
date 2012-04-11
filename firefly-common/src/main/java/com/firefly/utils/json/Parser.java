package com.firefly.utils.json;

import com.firefly.utils.json.support.JsonStringReader;

public interface Parser {
	Object convertTo(JsonStringReader reader, Class<?> clazz);
}
