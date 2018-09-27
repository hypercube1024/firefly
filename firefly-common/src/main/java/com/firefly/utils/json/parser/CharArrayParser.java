package com.firefly.utils.json.parser;

import com.firefly.utils.json.JsonReader;
import com.firefly.utils.json.Parser;

public class CharArrayParser implements Parser {

    @Override
    public Object convertTo(JsonReader reader, Class<?> clazz) {
        String ret = reader.readString();
        return ret != null ? ret.toCharArray() : null;
    }

}
