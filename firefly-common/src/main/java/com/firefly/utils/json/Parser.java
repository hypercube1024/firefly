package com.firefly.utils.json;

import java.io.IOException;


public interface Parser {
    Object convertTo(JsonReader reader, Class<?> clazz) throws IOException;
}
