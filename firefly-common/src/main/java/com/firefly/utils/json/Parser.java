package com.firefly.utils.json;


public interface Parser {
	Object convertTo(JsonReader reader, Class<?> clazz);
}
