package com.firefly.utils.json.serializer;

import com.firefly.utils.json.JsonWriter;
import com.firefly.utils.json.Serializer;

import java.io.IOException;

public class StringValueSerializer implements Serializer {

    @Override
    public void convertTo(JsonWriter writer, Object obj) throws IOException {
        writer.write(String.valueOf(obj));
    }

}
