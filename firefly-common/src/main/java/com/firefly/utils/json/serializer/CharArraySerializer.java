package com.firefly.utils.json.serializer;

import com.firefly.utils.json.JsonWriter;
import com.firefly.utils.json.Serializer;

import java.io.IOException;

public class CharArraySerializer implements Serializer {

    @Override
    public void convertTo(JsonWriter writer, Object obj)
            throws IOException {
        writer.writeStringWithQuote(new String((char[]) obj));
    }

}
