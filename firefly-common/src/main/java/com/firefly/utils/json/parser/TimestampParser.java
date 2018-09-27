package com.firefly.utils.json.parser;

import com.firefly.utils.json.JsonReader;
import com.firefly.utils.json.Parser;

import java.io.IOException;
import java.util.Date;

public class TimestampParser implements Parser {

    @Override
    public Object convertTo(JsonReader reader, Class<?> clazz) throws IOException {
        return new Date(reader.readLong());
    }

}
