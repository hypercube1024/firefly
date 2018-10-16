package com.firefly.utils.json.parser;

import com.firefly.utils.codec.Base64Utils;
import com.firefly.utils.json.JsonReader;
import com.firefly.utils.json.Parser;

public class ByteArrayParser implements Parser {

    @Override
    public Object convertTo(JsonReader reader, Class<?> clazz) {
        return Base64Utils.decodeFromString(new String(reader.readChars()));
    }

}
